"""FX pair catalog and WS helpers. Live prices come from Java ingest via Redis."""

from __future__ import annotations

from dataclasses import dataclass
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

QUOTE_CHANNEL = "peach:quotes"
QUOTE_KEY_PREFIX = "peach:quote:"

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


@dataclass(frozen=True)
class Pair:
    curpair_cd: int
    curpair_name: str
    curpair_display: str

    @property
    def ticker(self) -> str:
        return self.curpair_display


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
