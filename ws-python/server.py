"""Python WebSocket gateway: relays Java ingest ticks from Redis.

Does not generate prices. Java TickIngestWorker publishes peach:quotes.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import logging
import os
from collections import defaultdict
from collections.abc import AsyncIterator
from dataclasses import dataclass, field
from typing import Any, Protocol

import redis.asyncio as redis
import websockets
from websockets.asyncio.server import ServerConnection, serve

from market import (
    QUOTE_CHANNEL,
    QUOTE_KEY_PREFIX,
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


class QuoteSource(Protocol):
    async def snapshot(self) -> list[dict]: ...

    def listen(self) -> AsyncIterator[dict]: ...


class RedisQuoteSource:
    def __init__(self, host: str, port: int) -> None:
        self._redis = redis.Redis(host=host, port=port, decode_responses=True)

    async def snapshot(self) -> list[dict]:
        quotes: list[dict] = []
        async for key in self._redis.scan_iter(match=f"{QUOTE_KEY_PREFIX}*"):
            raw = await self._redis.get(key)
            if not raw:
                continue
            try:
                quotes.append(json.loads(raw))
            except json.JSONDecodeError:
                continue
        return quotes

    async def listen(self) -> AsyncIterator[dict]:
        pubsub = self._redis.pubsub()
        await pubsub.subscribe(QUOTE_CHANNEL)
        try:
            async for message in pubsub.listen():
                if message is None or message.get("type") != "message":
                    continue
                data = message.get("data")
                if not data:
                    continue
                try:
                    yield json.loads(data)
                except json.JSONDecodeError:
                    continue
        finally:
            await pubsub.unsubscribe(QUOTE_CHANNEL)
            await pubsub.aclose()


class InMemoryQuoteSource:
    """Test double: snapshot plus an asyncio queue of published ticks."""

    def __init__(self, snapshot: list[dict] | None = None) -> None:
        self._snapshot = list(snapshot or [])
        self._queue: asyncio.Queue[dict | None] = asyncio.Queue()

    async def snapshot(self) -> list[dict]:
        return list(self._snapshot)

    def publish(self, quote: dict) -> None:
        self._snapshot = [
            item for item in self._snapshot if item.get("curpairCd") != quote.get("curpairCd")
        ]
        self._snapshot.append(quote)
        self._queue.put_nowait(quote)

    async def listen(self) -> AsyncIterator[dict]:
        while True:
            item = await self._queue.get()
            if item is None:
                return
            yield item

    def close(self) -> None:
        self._queue.put_nowait(None)


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


def _now_ms() -> int:
    import time

    return int(time.time() * 1000)


@dataclass
class StreamState:
    pair: Any
    period_ms: int
    price: str
    bar: dict | None = None


class KlineStreamer:
    def __init__(self) -> None:
        self.listeners: dict[str, set[Any]] = defaultdict(set)
        self.states: dict[str, StreamState] = {}

    @staticmethod
    def stream_name(pair, period_ms: int, price: str) -> str:
        return f"{pair.ticker}|{period_ms}|{price}"

    def subscribe(
        self,
        pair,
        period_ms: int,
        price: str,
        listener,
        close: float | None,
    ) -> dict | None:
        name = self.stream_name(pair, period_ms, price)
        self.listeners[name].add(listener)
        self.states.setdefault(name, StreamState(pair, period_ms, price))
        if close is None:
            return None
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
        by_cd = {str(item["curpairCd"]): item for item in quotes}
        outgoing: list[tuple[Any, dict]] = []
        for name, state in list(self.states.items()):
            quote = by_cd.get(str(state.pair.curpair_cd))
            if quote is None:
                continue
            close = float(quote[state.price])
            bar = self.current_bar(state, close, now_ms)
            if bar is None:
                continue
            for listener in list(self.listeners.get(name, ())):
                outgoing.append((listener, bar))
        return outgoing

    def current_bar(self, state: StreamState, close: float, now_ms: int) -> dict | None:
        bar_time = (now_ms // state.period_ms) * state.period_ms
        if state.bar is None or state.bar["time"] != bar_time:
            open_ = state.bar["close"] if state.bar is not None else close
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


@dataclass
class Hub:
    source: QuoteSource
    quote_clients: set[ServerConnection] = field(default_factory=set)
    latest: dict[str, dict] = field(default_factory=dict)
    streamer: KlineStreamer = field(init=False)

    def __post_init__(self) -> None:
        self.streamer = KlineStreamer()

    def apply_quote(self, quote: dict) -> None:
        self.latest[str(quote["curpairCd"])] = quote

    def latest_close(self, curpair_cd: int, price: str) -> float | None:
        quote = self.latest.get(str(curpair_cd))
        if quote is None:
            return None
        return float(quote[price])

    async def run_ingest(self) -> None:
        for quote in await self.source.snapshot():
            self.apply_quote(quote)
        async for quote in self.source.listen():
            self.apply_quote(quote)
            now_ms = _now_ms()
            for client in list(self.quote_clients):
                await _send_json(client, quote)
            for listener, bar in self.streamer.on_quotes([quote], now_ms):
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
        for message in hub.latest.values():
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
            close = hub.latest_close(pair.curpair_cd, price)
            current = hub.streamer.subscribe(pair, period_ms, price, sub.emit_bar, close)
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


def redis_source_from_env() -> RedisQuoteSource:
    host = os.environ.get("REDIS_HOST", "127.0.0.1")
    port = int(os.environ.get("REDIS_PORT", "6379"))
    return RedisQuoteSource(host, port)


async def run_server(host: str, port: int, hub: Hub | None = None) -> None:
    hub = hub or Hub(source=redis_source_from_env())
    log.info("FX WebSocket gateway on ws://%s:%s (Redis %s)", host, port, QUOTE_CHANNEL)
    async with serve(
        lambda connection: router(connection, hub),
        host,
        port,
        origins=ALLOWED_ORIGINS,
    ):
        ingest_task = asyncio.create_task(hub.run_ingest())
        try:
            await asyncio.Future()
        finally:
            ingest_task.cancel()


def main() -> None:
    parser = argparse.ArgumentParser(description="FX WebSocket gateway (Redis ingest)")
    parser.add_argument("--host", default=os.environ.get("WS_HOST", "127.0.0.1"))
    parser.add_argument("--port", type=int, default=int(os.environ.get("WS_PORT", "8081")))
    args = parser.parse_args()
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")
    asyncio.run(run_server(args.host, args.port))


if __name__ == "__main__":
    main()
