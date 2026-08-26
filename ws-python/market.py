"""Demo FX catalog, quotes, and historical bars. Mirrors the Java mock logic."""

from __future__ import annotations

import math
import random
import time
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP
from typing import Literal

PriceName = Literal["bid", "ask", "mid"]

# Must match GET /curpairs (m_ccypairs.priority, ccypair_cd, slash display).
PAIRS: tuple[tuple[int, str, str], ...] = (
    (1, "USDJPY", "USD/JPY"),
    (2, "EURJPY", "EUR/JPY"),
    (3, "EURUSD", "EUR/USD"),
    (4, "GBPUSD", "GBP/USD"),
    (5, "AUDUSD", "AUD/USD"),
)

TICK_MS = 333

SECOND = 1_000
MINUTE = 60 * SECOND
HOUR = 60 * MINUTE
DAY = 24 * HOUR

PERIOD_MS: dict[str, int] = {
    "1S": SECOND,
    "1s": SECOND,
    "1": MINUTE,
    "3": 3 * MINUTE,
    "5": 5 * MINUTE,
    "15": 15 * MINUTE,
    "30": 30 * MINUTE,
    "60": HOUR,
    "120": 2 * HOUR,
    "240": 4 * HOUR,
    "360": 6 * HOUR,
    "480": 8 * HOUR,
    "720": 12 * HOUR,
    "D": DAY,
    "1D": DAY,
    "3D": 3 * DAY,
    "W": 7 * DAY,
    "1W": 7 * DAY,
    "M": 30 * DAY,
    "1M": 30 * DAY,
}

_MAX_STEPS: dict[str, str] = {
    "USDJPY": "0.012",
    "EURJPY": "0.014",
    "EURUSD": "0.00012",
    "GBPUSD": "0.00014",
    "AUDUSD": "0.00010",
}

_SEED_MID: dict[str, float] = {
    "USDJPY": 149.850,
    "EURJPY": 162.420,
    "EURUSD": 1.08540,
    "GBPUSD": 1.27180,
    "AUDUSD": 0.66250,
}

_U64 = (1 << 64) - 1


def yen_quote(name: str | None) -> bool:
    return bool(name) and name.endswith("JPY")


def price_scale(name: str) -> int:
    return 1000 if yen_quote(name) else 100_000


def half_spread(name: str) -> float:
    return 0.05 if yen_quote(name) else 0.00050


def full_spread(name: str) -> float:
    return half_spread(name) * 2.0


def seed_mid(name: str) -> float:
    if name in _SEED_MID:
        return _SEED_MID[name]
    return 100.000 if yen_quote(name) else 1.00000


def seed_bid(name: str) -> float:
    return seed_mid(name) - half_spread(name)


def bar_amplitude(name: str) -> float:
    return 0.08 if yen_quote(name) else 0.00080


def outer_wick(name: str) -> float:
    return 0.20 if yen_quote(name) else 0.0020


def quantize(value: Decimal | float | str, scale: int) -> Decimal:
    quant = Decimal(1).scaleb(-scale)
    return Decimal(str(value) if not isinstance(value, Decimal) else value).quantize(
        quant, rounding=ROUND_HALF_UP
    )


def parse_price(raw: str | None) -> PriceName:
    if raw is None or not str(raw).strip():
        return "mid"
    match str(raw).strip().lower():
        case "bid":
            return "bid"
        case "ask":
            return "ask"
        case _:
            return "mid"


def java_string_hash(text: str) -> int:
    h = 0
    for ch in text:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    if h >= 0x80000000:
        h -= 0x100000000
    return h


def mix64(a: int, b: int) -> int:
    x = ((a & _U64) * 0x9E3779B97F4A7C15 ^ (b & _U64)) & _U64
    x ^= x >> 33
    x = (x * 0xFF51AFD7ED558CCD) & _U64
    x ^= x >> 33
    return x


def java_math_round(value: float) -> float:
    return math.floor(value + 0.5)


def round_price(value: float, scale: int) -> float:
    return java_math_round(value * scale) / scale


@dataclass(frozen=True)
class Pair:
    curpair_cd: int
    curpair_name: str
    curpair_display: str

    @property
    def ticker(self) -> str:
        return self.curpair_display

    @property
    def scale(self) -> int:
        return price_scale(self.curpair_name)


PAIRS_BY_CD: dict[int, Pair] = {
    cd: Pair(cd, name, display) for cd, name, display in PAIRS
}


def find_symbol(symbol_name: str | None) -> Pair | None:
    if symbol_name is None or not symbol_name.strip():
        return None
    needle = symbol_name.strip().lower()
    for pair in PAIRS_BY_CD.values():
        if (
            pair.ticker.lower() == needle
            or pair.curpair_display.lower() == needle
            or pair.curpair_name.lower() == needle
            or str(pair.curpair_cd) == needle
            or f"fx:{pair.curpair_display}".lower() == needle
        ):
            return pair
    return None


def period_millis(resolution: str | None) -> int | None:
    if resolution is None:
        return None
    return PERIOD_MS.get(resolution)


def _bid_at(pair: Pair, period_ms: int, time_ms: int) -> float:
    seed = seed_bid(pair.curpair_name)
    steps = time_ms // period_ms
    hashed = java_string_hash(pair.ticker)
    wave = (
        math.sin((steps + hashed) * 0.013) * 0.004
        + math.sin((steps + hashed) * 0.0031) * 0.008
        + math.sin((steps + hashed) * 0.0007) * 0.012
    )
    noise = ((mix64(hashed, steps) & 0xFFFF) / 65535.0 - 0.5) * 0.003
    return max(seed * (1.0 + wave + noise), seed * 0.2)


def _price_at(pair: Pair, period_ms: int, time_ms: int, price: PriceName) -> float:
    bid = _bid_at(pair, period_ms, time_ms)
    name = pair.curpair_name
    if price == "bid":
        return bid
    if price == "ask":
        return bid + full_spread(name)
    return bid + half_spread(name)


def _ordinal(price: PriceName) -> int:
    return {"bid": 0, "ask": 1, "mid": 2}[price]


def bar_at(pair: Pair, period_ms: int, time_ms: int, price: PriceName) -> dict:
    scale = pair.scale
    open_ = round_price(_price_at(pair, period_ms, time_ms, price), scale)
    close = round_price(_price_at(pair, period_ms, time_ms + period_ms, price), scale)
    p1 = round_price(_price_at(pair, period_ms, time_ms + period_ms // 3, price), scale)
    p2 = round_price(_price_at(pair, period_ms, time_ms + 2 * period_ms // 3, price), scale)
    high = max(open_, close, p1, p2)
    low = min(open_, close, p1, p2)
    mix = mix64(
        java_string_hash(pair.ticker) + _ordinal(price) * 97,
        time_ms // period_ms,
    )
    inner = bar_amplitude(pair.curpair_name) * (0.15 + 0.20 * ((mix & 1023) / 1023.0))
    outer = outer_wick(pair.curpair_name)
    if price == "ask":
        high = round_price(high + outer, scale)
        low = round_price(low - inner, scale)
    elif price == "bid":
        high = round_price(high + inner, scale)
        low = round_price(low - outer, scale)
    else:
        high = round_price(high + inner, scale)
        low = round_price(low - inner, scale)
    if low <= 0:
        low = round_price(min(open_, close) * 0.5, scale)
    volume = float(80 + (mix & 2047))
    return {
        "time": time_ms,
        "open": open_,
        "high": high,
        "low": low,
        "close": close,
        "volume": volume,
    }


class SimulatedQuote:
    def __init__(self, pair: Pair, rng: random.Random | None = None) -> None:
        self.pair = pair
        self.scale = 3 if yen_quote(pair.curpair_name) else 5
        self.spread = quantize(full_spread(pair.curpair_name), self.scale)
        default_step = "0.010" if yen_quote(pair.curpair_name) else "0.00010"
        self.max_step = quantize(
            _MAX_STEPS.get(pair.curpair_name, default_step), self.scale
        )
        self.bid = quantize(seed_bid(pair.curpair_name), self.scale)
        self.ask = Decimal(0)
        self.mid = Decimal(0)
        self.high = Decimal(0)
        self.low = Decimal(0)
        self._rng = rng or random.Random()
        self._apply_ask_from_bid()
        self.high = self.ask
        self.low = self.bid

    def _tick(self) -> Decimal:
        return Decimal(1).scaleb(-self.scale)

    def _apply_ask_from_bid(self) -> None:
        self.ask = quantize(self.bid + self.spread, self.scale)
        if self.bid >= self.ask:
            self.ask = self.bid + self._tick()
        self.mid = quantize((self.bid + self.ask) / Decimal(2), self.scale)

    def step(self) -> None:
        gaussian = self._rng.gauss(0.0, 1.0)
        delta = self.max_step * Decimal(str(gaussian / 3.0))
        self.bid = quantize(self.bid + delta, self.scale)
        if self.bid <= 0:
            self.bid = self.max_step
        self._apply_ask_from_bid()
        if self.ask > self.high:
            self.high = self.ask
        if self.bid < self.low:
            self.low = self.bid

    def price(self, component: PriceName) -> float:
        if component == "bid":
            return float(self.bid)
        if component == "ask":
            return float(self.ask)
        return float(self.mid)

    def to_message(self) -> dict:
        return {
            "curpairCd": str(self.pair.curpair_cd),
            "rateMiliSecondUTC": int(time.time() * 1000),
            "bid": float(self.bid),
            "ask": float(self.ask),
            "mid": float(self.mid),
            "high": float(self.high),
            "low": float(self.low),
        }


class QuoteBook:
    def __init__(self, rng: random.Random | None = None) -> None:
        self.quotes = {
            pair.curpair_cd: SimulatedQuote(pair, rng) for pair in PAIRS_BY_CD.values()
        }

    def snapshot(self) -> list[dict]:
        return [quote.to_message() for quote in self.quotes.values()]

    def tick(self) -> list[dict]:
        messages = []
        for quote in self.quotes.values():
            quote.step()
            messages.append(quote.to_message())
        return messages

    def current_price(self, curpair_cd: int, component: PriceName) -> float:
        return self.quotes[curpair_cd].price(component)
