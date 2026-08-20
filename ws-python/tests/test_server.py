from __future__ import annotations

import asyncio
import json

import pytest
import websockets
from websockets.asyncio.server import serve

from market import TICK_MS
from server import Hub, router


@pytest.fixture
async def ws_base() -> str:
    hub = Hub()
    async with serve(lambda connection: router(connection, hub), "127.0.0.1", 0, origins=None) as server:
        port = server.sockets[0].getsockname()[1]
        tick = asyncio.create_task(hub.run_ticks())
        try:
            yield f"ws://127.0.0.1:{port}"
        finally:
            tick.cancel()
            try:
                await tick
            except asyncio.CancelledError:
                pass


async def test_fx_quotes_snapshot_and_payload(ws_base: str) -> None:
    async with websockets.connect(f"{ws_base}/ws/fx-quotes") as ws:
        seen: dict[str, dict] = {}
        while len(seen) < 5:
            quote = json.loads(await asyncio.wait_for(ws.recv(), timeout=2))
            seen[quote["curpairCd"]] = quote
        assert set(seen) == {"1", "2", "3", "4", "5"}
        usd = seen["1"]
        assert usd["bid"] < usd["ask"]
        assert usd["mid"] == pytest.approx((usd["bid"] + usd["ask"]) / 2.0, abs=1e-6)
        for key in ("rateMiliSecondUTC", "bid", "ask", "mid", "high", "low"):
            assert key in usd


async def test_fx_quotes_about_three_ticks_per_second(ws_base: str) -> None:
    async with websockets.connect(f"{ws_base}/ws/fx-quotes") as ws:
        seen: set[str] = set()
        while len(seen) < 5:
            quote = json.loads(await asyncio.wait_for(ws.recv(), timeout=2))
            seen.add(quote["curpairCd"])
        count = 0
        deadline = asyncio.get_running_loop().time() + 1.1
        while asyncio.get_running_loop().time() < deadline:
            await asyncio.wait_for(ws.recv(), timeout=2)
            count += 1
        # Five pairs per tick at ~3 ticks/s → about 15 messages/s.
        assert 8 <= count <= 25
        assert TICK_MS == 333


async def test_stream_subscribe_unsubscribe_and_error(ws_base: str) -> None:
    async with websockets.connect(f"{ws_base}/ws/stream") as ws:
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
