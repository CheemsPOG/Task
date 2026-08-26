from __future__ import annotations

import asyncio
import json

import pytest
import websockets
from websockets.asyncio.server import serve

from server import Hub, InMemoryQuoteSource, router


def _quote(curpair_cd: str, bid: float, ask: float) -> dict:
    return {
        "curpairCd": curpair_cd,
        "rateMiliSecondUTC": 1_700_000_000_000,
        "bid": bid,
        "ask": ask,
        "mid": (bid + ask) / 2.0,
        "high": ask,
        "low": bid,
    }


SEED = [
    _quote("1", 149.800, 149.900),
    _quote("2", 162.370, 162.470),
    _quote("3", 1.08490, 1.08590),
    _quote("4", 1.27130, 1.27230),
    _quote("5", 0.66200, 0.66300),
]


@pytest.fixture
async def ws_hub() -> tuple[str, InMemoryQuoteSource]:
    source = InMemoryQuoteSource(SEED)
    hub = Hub(source=source)
    async with serve(lambda connection: router(connection, hub), "127.0.0.1", 0, origins=None) as server:
        port = server.sockets[0].getsockname()[1]
        ingest = asyncio.create_task(hub.run_ingest())
        for _ in range(50):
            if len(hub.latest) >= 5:
                break
            await asyncio.sleep(0.02)
        try:
            yield f"ws://127.0.0.1:{port}", source
        finally:
            source.close()
            ingest.cancel()
            try:
                await ingest
            except asyncio.CancelledError:
                pass


async def test_fx_quotes_snapshot_from_ingest(ws_hub: tuple[str, InMemoryQuoteSource]) -> None:
    base, _source = ws_hub
    async with websockets.connect(f"{base}/ws/fx-quotes") as ws:
        seen: dict[str, dict] = {}
        while len(seen) < 5:
            quote = json.loads(await asyncio.wait_for(ws.recv(), timeout=2))
            seen[quote["curpairCd"]] = quote
        assert set(seen) == {"1", "2", "3", "4", "5"}
        usd = seen["1"]
        assert usd["bid"] < usd["ask"]
        assert usd["mid"] == pytest.approx((usd["bid"] + usd["ask"]) / 2.0, abs=1e-6)


async def test_fx_quotes_forwards_published_ticks(
    ws_hub: tuple[str, InMemoryQuoteSource],
) -> None:
    base, source = ws_hub
    async with websockets.connect(f"{base}/ws/fx-quotes") as ws:
        seen: set[str] = set()
        while len(seen) < 5:
            quote = json.loads(await asyncio.wait_for(ws.recv(), timeout=2))
            seen.add(quote["curpairCd"])
        updated = _quote("1", 149.810, 149.910)
        source.publish(updated)
        forwarded = json.loads(await asyncio.wait_for(ws.recv(), timeout=2))
        assert forwarded["curpairCd"] == "1"
        assert forwarded["bid"] == 149.810
        assert forwarded["ask"] == 149.910


async def test_fx_quotes_idle_without_ingest() -> None:
    source = InMemoryQuoteSource([])
    hub = Hub(source=source)
    async with serve(lambda connection: router(connection, hub), "127.0.0.1", 0, origins=None) as server:
        port = server.sockets[0].getsockname()[1]
        ingest = asyncio.create_task(hub.run_ingest())
        try:
            async with websockets.connect(f"ws://127.0.0.1:{port}/ws/fx-quotes") as ws:
                with pytest.raises(asyncio.TimeoutError):
                    await asyncio.wait_for(ws.recv(), timeout=0.4)
        finally:
            source.close()
            ingest.cancel()
            try:
                await ingest
            except asyncio.CancelledError:
                pass


async def test_stream_subscribe_unsubscribe_and_error(
    ws_hub: tuple[str, InMemoryQuoteSource],
) -> None:
    base, _source = ws_hub
    async with websockets.connect(f"{base}/ws/stream") as ws:
        await ws.send(
            json.dumps(
                {
                    "action": "subscribe",
                    "uid": "sub-1",
                    "symbol": "USD/JPY",
                    "resolution": "1D",
                    "price": "bid",
                }
            )
        )
        first = json.loads(await asyncio.wait_for(ws.recv(), timeout=2))
        assert first["type"] == "bar"
        assert first["uid"] == "sub-1"
        bar = first["bar"]
        for key in ("time", "open", "high", "low", "close", "volume"):
            assert key in bar
        assert bar["high"] >= max(bar["open"], bar["close"])
        assert bar["low"] <= min(bar["open"], bar["close"])

        await ws.send(json.dumps({"action": "unsubscribe", "uid": "sub-1"}))
        await ws.send(
            json.dumps(
                {
                    "action": "subscribe",
                    "uid": "sub-2",
                    "symbol": "NOPE",
                    "resolution": "1D",
                    "price": "mid",
                }
            )
        )
        error = json.loads(await asyncio.wait_for(ws.recv(), timeout=2))
        assert error["type"] == "error"
        assert error["uid"] == "sub-2"
        assert "Cannot subscribe to NOPE @ 1D" in error["message"]
