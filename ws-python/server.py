"""Python WebSocket server for /ws/fx-quotes and /ws/stream.

Message contracts match the previous Java handlers so the frontend is unchanged.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import logging
import os
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Any

import websockets
from websockets.asyncio.server import ServerConnection, serve

from market import (
    TICK_MS,
    QuoteBook,
    bar_at,
    find_symbol,
    parse_price,
    period_millis,
)

log = logging.getLogger("ws-python")

ALLOWED_ORIGINS = [
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://localhost:3000",
    "http://127.0.0.1:3000",
]


def _path_of(connection: ServerConnection) -> str:
    request = getattr(connection, "request", None)
    if request is not None:
        return request.path.split("?", 1)[0]
    return getattr(connection, "path", "").split("?", 1)[0]


async def _send_json(connection: ServerConnection, payload: dict) -> None:
    try:
        await connection.send(json.dumps(payload, separators=(",", ":")))
    except websockets.ConnectionClosed:
        return


@dataclass
class StreamState:
    pair: Any
    period_ms: int
    price: str
    bar: dict | None = None


class KlineStreamer:
    def __init__(self, book: QuoteBook) -> None:
        self.book = book
        self.listeners: dict[str, set[Any]] = defaultdict(set)
        self.states: dict[str, StreamState] = {}

    @staticmethod
    def stream_name(pair, period_ms: int, price: str) -> str:
        return f"{pair.ticker}|{period_ms}|{price}"

    def subscribe(self, pair, period_ms: int, price: str, listener) -> dict | None:
        name = self.stream_name(pair, period_ms, price)
        self.listeners[name].add(listener)
        self.states.setdefault(name, StreamState(pair, period_ms, price))
        close = self.book.current_price(pair.curpair_cd, price)
        return self.current_bar(self.states[name], close, _now_ms())

    def unsubscribe(self, pair, period_ms: int, price: str, listener) -> None:
        name = self.stream_name(pair, period_ms, price)
        listeners = self.listeners.get(name)
        if not listeners:
            return
        listeners.discard(listener)
        if not listeners:
            self.listeners.pop(name, None)
            self.states.pop(name, None)

    def on_quotes(self, quotes: list[dict], now_ms: int) -> list[tuple[Any, dict]]:
        if not self.states:
            return []
        by_cd = {item["curpairCd"]: item for item in quotes}
        outgoing: list[tuple[Any, dict]] = []
        for name, state in list(self.states.items()):
            quote = by_cd.get(str(state.pair.curpair_cd))
            if quote is None:
                continue
            close = float(quote[state.price])
            bar = self.current_bar(state, close, now_ms)
            for listener in list(self.listeners.get(name, ())):
                outgoing.append((listener, bar))
        return outgoing

    def current_bar(self, state: StreamState, close: float, now_ms: int) -> dict | None:
        bar_time = (now_ms // state.period_ms) * state.period_ms
        if state.bar is None or state.bar["time"] != bar_time:
            open_ = state.bar["close"] if state.bar is not None else self._seed_open(
                state, bar_time, close
            )
            state.bar = {
                "time": bar_time,
                "open": open_,
                "high": max(open_, close),
                "low": min(open_, close),
                "close": close,
                "volume": 1,
            }
            return state.bar
        state.bar = {
            "time": bar_time,
            "open": state.bar["open"],
            "high": max(state.bar["high"], close),
            "low": min(state.bar["low"], close),
            "close": close,
            "volume": state.bar["volume"] + 1,
        }
        return state.bar

    def _seed_open(self, state: StreamState, bar_time: int, close: float) -> float:
        historical = bar_at(state.pair, state.period_ms, bar_time, state.price)
        if historical is not None:
            return historical["open"]
        return close


def _now_ms() -> int:
    return int(time_module() * 1000)


def time_module() -> float:
    import time

    return time.time()


@dataclass
class Hub:
    book: QuoteBook = field(default_factory=QuoteBook)
    quote_clients: set[ServerConnection] = field(default_factory=set)
    streamer: KlineStreamer = field(init=False)

    def __post_init__(self) -> None:
        self.streamer = KlineStreamer(self.book)

    async def run_ticks(self) -> None:
        while True:
            await asyncio.sleep(TICK_MS / 1000)
            messages = self.book.tick()
            now_ms = _now_ms()
            for client in list(self.quote_clients):
                for message in messages:
                    await _send_json(client, message)
            for listener, bar in self.streamer.on_quotes(messages, now_ms):
                await listener(bar)


@dataclass
class StreamSubscription:
    uid: str
    pair: Any
    period_ms: int
    price: str
    connection: ServerConnection

    async def emit_bar(self, bar: dict) -> None:
        await _send_json(
            self.connection,
            {"type": "bar", "uid": self.uid, "bar": bar},
        )


async def handle_fx_quotes(connection: ServerConnection, hub: Hub) -> None:
    hub.quote_clients.add(connection)
    try:
        for message in hub.book.snapshot():
            await _send_json(connection, message)
        await connection.wait_closed()
    finally:
        hub.quote_clients.discard(connection)


async def handle_stream(connection: ServerConnection, hub: Hub) -> None:
    subscriptions: dict[str, StreamSubscription] = {}

    async def drop(uid: str) -> None:
        sub = subscriptions.pop(uid, None)
        if sub is not None:
            hub.streamer.unsubscribe(sub.pair, sub.period_ms, sub.price, sub.emit_bar)

    try:
        async for raw in connection:
            try:
                root = json.loads(raw)
            except json.JSONDecodeError:
                continue
            action = root.get("action")
            uid = str(root.get("uid") or "")
            if action == "unsubscribe":
                await drop(uid)
                continue
            if action != "subscribe":
                continue

            await drop(uid)
            symbol_name = str(root.get("symbol") or "")
            resolution = str(root.get("resolution") or "")
            price = parse_price(root.get("price", "mid"))
            pair = find_symbol(symbol_name)
            period_ms = period_millis(resolution)
            if pair is None or period_ms is None:
                await _send_json(
                    connection,
                    {
                        "type": "error",
                        "uid": uid,
                        "message": f"Cannot subscribe to {symbol_name} @ {resolution}",
                    },
                )
                continue

            sub = StreamSubscription(uid, pair, period_ms, price, connection)
            subscriptions[uid] = sub
            current = hub.streamer.subscribe(pair, period_ms, price, sub.emit_bar)
            if current is not None:
                await sub.emit_bar(current)
    finally:
        for uid in list(subscriptions):
            await drop(uid)


async def router(connection: ServerConnection, hub: Hub) -> None:
    path = _path_of(connection)
    if path == "/ws/fx-quotes":
        await handle_fx_quotes(connection, hub)
    elif path == "/ws/stream":
        await handle_stream(connection, hub)
    else:
        await connection.close(1008, "unknown path")


async def run_server(host: str, port: int, hub: Hub | None = None) -> None:
    hub = hub or Hub()
    log.info("FX WebSocket listening on ws://%s:%s (/ws/fx-quotes, /ws/stream)", host, port)
    async with serve(
        lambda connection: router(connection, hub),
        host,
        port,
        origins=ALLOWED_ORIGINS,
    ):
        tick_task = asyncio.create_task(hub.run_ticks())
        try:
            await asyncio.Future()
        finally:
            tick_task.cancel()


def main() -> None:
    parser = argparse.ArgumentParser(description="Mock FX WebSocket server")
    parser.add_argument("--host", default=os.environ.get("WS_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("WS_PORT", "8081")))
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
    asyncio.run(run_server(args.host, args.port))


if __name__ == "__main__":
    main()
