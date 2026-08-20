from __future__ import annotations

import pytest

from market import (
    QuoteBook,
    bar_at,
    find_symbol,
    full_spread,
    parse_price,
    period_millis,
)

DAY = 86_400_000


def test_catalog_resolves_like_java() -> None:
    usd = find_symbol("USD/JPY")
    assert usd is not None
    assert usd.curpair_cd == 1
    assert usd.curpair_name == "USDJPY"
    assert find_symbol("USDJPY") == usd
    assert find_symbol("FX:USD/JPY") == usd
    assert find_symbol("1") == usd
    assert find_symbol("ETH/USDT") is None


def test_parse_price_defaults_to_mid() -> None:
    assert parse_price(None) == "mid"
    assert parse_price("") == "mid"
    assert parse_price("BID") == "bid"
    assert parse_price("ask") == "ask"


def test_period_millis() -> None:
    assert period_millis("1D") == DAY
    assert period_millis("1") == 60_000
    assert period_millis("2") is None


def test_quotes_keep_bid_ask_mid_relationship() -> None:
    book = QuoteBook()
    for _ in range(40):
        book.tick()
    snapshot = book.snapshot()
    assert len(snapshot) == 5
    for quote in snapshot:
        pair = find_symbol(quote["curpairCd"])
        assert pair is not None
        assert quote["curpairCd"] == str(pair.curpair_cd)
        assert isinstance(quote["curpairCd"], str)
        assert quote["bid"] < quote["ask"]
        assert quote["ask"] == pytest.approx(
            quote["bid"] + full_spread(pair.curpair_name), abs=0.001
        )
        assert quote["mid"] == pytest.approx((quote["bid"] + quote["ask"]) / 2.0, abs=1e-7)
        assert quote["high"] >= quote["ask"]
        assert quote["low"] <= quote["bid"]
        assert quote["rateMiliSecondUTC"] > 0


def test_history_bars_are_deterministic_and_shaped() -> None:
    pair = find_symbol("USD/JPY")
    assert pair is not None
    to_ms = 1_767_225_600_000  # 2026-01-01T00:00:00Z
    first = bar_at(pair, DAY, to_ms - DAY, "mid")
    second = bar_at(pair, DAY, to_ms - DAY, "mid")
    assert first == second
    bid = bar_at(pair, DAY, to_ms - DAY, "bid")
    ask = bar_at(pair, DAY, to_ms - DAY, "ask")
    mid = first
    assert bid["close"] < mid["close"] < ask["close"]
    assert ask["open"] == pytest.approx(bid["open"] + full_spread("USDJPY"), abs=0.001)
    assert ask["high"] > mid["high"]
    assert bid["low"] < mid["low"]
