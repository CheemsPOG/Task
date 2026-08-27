"""Python WebSocket gateway (port 8081): relays Java ingest from Redis to the browser.

This process does NOT generate prices and does NOT build OHLC candles.

Pipeline:
  Java TickIngestWorker
    -> Redis SET peach:quote:{cd}  + PUBLISH peach:quotes   (ticks)
    -> Redis SET peach:forming:{res}:{pair} + PUBLISH peach:bars  (forming candles)
  this file
    -> /ws/fx-quotes  : every tick JSON as Java published it (header ticker)
    -> /ws/stream     : subscribe JSON in, {type:bar, uid, bar} out (chart candles)

Vite proxies browser /ws/* here. CORS origins are localhost:5173 / :3000 only.

Replace DemoTickEngine in Java when a real LP arrives. Keep these Redis keys
and this relay so the frontend does not change.
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
from typing import Any, Literal, Protocol

import redis.asyncio as redis
import websockets
from websockets.asyncio.server import ServerConnection, serve

from market import (
    BAR_CHANNEL,
    FORMING_KEY_PREFIX,
    QUOTE_CHANNEL,
    QUOTE_KEY_PREFIX,
    PriceName,
    find_symbol,
    parse_price,
    period_millis,
    widget_bar,
)

log = logging.getLogger("ws-python")

ALLOWED_ORIGINS = [
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://localhost:3000",
    "http://127.0.0.1:3000",
]

EventKind = Literal["quote", "bar"]


class MarketSource(Protocol):
    """Anything that can dump current Redis snapshots and then yield live events.

    Production uses RedisMarketSource. Tests use InMemoryQuoteSource.
    """

    async def snapshot_quotes(self) -> list[dict]: ...

    async def snapshot_bars(self) -> list[dict]: ...

    def listen(self) -> AsyncIterator[tuple[EventKind, dict]]: ...


class RedisMarketSource:
    """Reads peach:quote:* / peach:forming:* then SUBSCRIBE peach:quotes + peach:bars."""

    def __init__(self, host: str, port: int) -> None:
        self._redis = redis.Redis(host=host, port=port, decode_responses=True)

    async def snapshot_quotes(self) -> list[dict]:
        return await self._scan_json(f"{QUOTE_KEY_PREFIX}*")

    async def snapshot_bars(self) -> list[dict]:
        return await self._scan_json(f"{FORMING_KEY_PREFIX}*")

    async def _scan_json(self, match: str) -> list[dict]:
        items: list[dict] = []
        async for key in self._redis.scan_iter(match=match):
            raw = await self._redis.get(key)
            if not raw:
                continue
            try:
                items.append(json.loads(raw))
            except json.JSONDecodeError:
                continue
        return items

    async def listen(self) -> AsyncIterator[tuple[EventKind, dict]]:
        pubsub = self._redis.pubsub()
        await pubsub.subscribe(QUOTE_CHANNEL, BAR_CHANNEL)
        try:
            async for message in pubsub.listen():
                if message is None or message.get("type") != "message":
                    continue
                data = message.get("data")
                channel = message.get("channel")
                if not data:
                    continue
                try:
                    payload = json.loads(data)
                except json.JSONDecodeError:
                    continue
                if channel == QUOTE_CHANNEL:
                    yield "quote", payload
                elif channel == BAR_CHANNEL:
                    yield "bar", payload
        finally:
            await pubsub.unsubscribe(QUOTE_CHANNEL, BAR_CHANNEL)
            await pubsub.aclose()


class InMemoryQuoteSource:
    """Test double: snapshot plus an asyncio queue of quotes and forming bars."""

    def __init__(
        self,
        snapshot: list[dict] | None = None,
        bars: list[dict] | None = None,
    ) -> None:
        self._snapshot = list(snapshot or [])
        self._bars = list(bars or [])
        self._queue: asyncio.Queue[tuple[EventKind, dict] | None] = asyncio.Queue()

    async def snapshot_quotes(self) -> list[dict]:
        return list(self._snapshot)

    async def snapshot_bars(self) -> list[dict]:
        return list(self._bars)

    def publish(self, quote: dict) -> None:
        self._snapshot = [
            item for item in self._snapshot if item.get("curpairCd") != quote.get("curpairCd")
        ]
        self._snapshot.append(quote)
        self._queue.put_nowait(("quote", quote))

    def publish_bar(self, bar: dict) -> None:
        self._bars = [
            item
            for item in self._bars
            if not (
                item.get("curpairCd") == bar.get("curpairCd")
                and item.get("periodMs") == bar.get("periodMs")
            )
        ]
        self._bars.append(bar)
        self._queue.put_nowait(("bar", bar))

    async def listen(self) -> AsyncIterator[tuple[EventKind, dict]]:
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


def _forming_key(curpair_cd: str, period_ms: int) -> str:
    return f"{curpair_cd}|{period_ms}"


def _stream_name(curpair_cd: int, period_ms: int, price: str) -> str:
    return f"{curpair_cd}|{period_ms}|{price}"


class BarRelay:
    """Fan-out of Java forming bars. Does not compute open/high/low/volume."""

    def __init__(self) -> None:
        self.listeners: dict[str, set[Any]] = defaultdict(set)

    def subscribe(self, pair, period_ms: int, price: str, listener) -> None:
        self.listeners[_stream_name(pair.curpair_cd, period_ms, price)].add(listener)

    def unsubscribe(self, pair, period_ms: int, price: str, listener) -> None:
        name = _stream_name(pair.curpair_cd, period_ms, price)
        listeners = self.listeners.get(name)
        if not listeners:
            return
        listeners.discard(listener)
        if not listeners:
            self.listeners.pop(name, None)

    def outgoing(self, message: dict) -> list[tuple[Any, dict]]:
        curpair_cd = str(message.get("curpairCd") or "")
        try:
            period_ms = int(message["periodMs"])
            pair_cd = int(curpair_cd)
        except (KeyError, TypeError, ValueError):
            return []
        result: list[tuple[Any, dict]] = []
        for price in ("bid", "ask", "mid"):
            bar = widget_bar(message, price)
            if bar is None:
                continue
            name = _stream_name(pair_cd, period_ms, price)
            for listener in list(self.listeners.get(name, ())):
                result.append((listener, bar))
        return result


@dataclass
class Hub:
    """One process-wide cache of latest ticks and forming bars, plus fan-out lists."""
    source: MarketSource
    quote_clients: set[ServerConnection] = field(default_factory=set)
    latest: dict[str, dict] = field(default_factory=dict)
    latest_forming: dict[str, dict] = field(default_factory=dict)
    streamer: BarRelay = field(init=False)

    def __post_init__(self) -> None:
        self.streamer = BarRelay()

    def apply_quote(self, quote: dict) -> None:
        self.latest[str(quote["curpairCd"])] = quote

    def apply_forming(self, message: dict) -> None:
        try:
            key = _forming_key(str(message["curpairCd"]), int(message["periodMs"]))
        except (KeyError, TypeError, ValueError):
            return
        self.latest_forming[key] = message

    def latest_widget_bar(self, curpair_cd: int, period_ms: int, price: PriceName) -> dict | None:
        message = self.latest_forming.get(_forming_key(str(curpair_cd), period_ms))
        return widget_bar(message, price)

    async def run_ingest(self) -> None:
        for quote in await self.source.snapshot_quotes():
            self.apply_quote(quote)
        for message in await self.source.snapshot_bars():
            self.apply_forming(message)
        async for kind, payload in self.source.listen():
            if kind == "quote":
                self.apply_quote(payload)
                for client in list(self.quote_clients):
                    await _send_json(client, payload)
                continue
            self.apply_forming(payload)
            for listener, bar in self.streamer.outgoing(payload):
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
    """Push the latest tick for every pair, then keep the socket open for pub/sub."""
    hub.quote_clients.add(connection)
    try:
        for message in hub.latest.values():
            await _send_json(connection, message)
        await connection.wait_closed()
    finally:
        hub.quote_clients.discard(connection)


async def handle_stream(connection: ServerConnection, hub: Hub) -> None:
    """Chart candle socket. Client sends subscribe/unsubscribe; we never aggregate ticks."""
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
            hub.streamer.subscribe(pair, period_ms, price, sub.emit_bar)
            current = hub.latest_widget_bar(pair.curpair_cd, period_ms, price)
            if current is not None:
                await sub.emit_bar(current)
    finally:
        for uid in list(subscriptions):
            await drop(uid)


async def router(connection: ServerConnection, hub: Hub) -> None:
    """Path switch. Unknown paths close with 1008 so a mis-proxied request is obvious."""
    path = _path_of(connection)
    if path == "/ws/fx-quotes":
        await handle_fx_quotes(connection, hub)
    elif path == "/ws/stream":
        await handle_stream(connection, hub)
    else:
        await connection.close(1008, "unknown path")


def redis_source_from_env() -> RedisMarketSource:
    host = os.environ.get("REDIS_HOST", "127.0.0.1")
    port = int(os.environ.get("REDIS_PORT", "6379"))
    return RedisMarketSource(host, port)


async def run_server(host: str, port: int, hub: Hub | None = None) -> None:
    hub = hub or Hub(source=redis_source_from_env())
    log.info(
        "FX WebSocket gateway on ws://%s:%s (Redis %s + %s)",
        host,
        port,
        QUOTE_CHANNEL,
        BAR_CHANNEL,
    )
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
