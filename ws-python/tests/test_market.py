"""Unit tests for market.py: pair lookup, price side, period map, BID/ASK/MID projection.

These tests must stay green if someone reintroduces local OHLC aggregation.
"""

from __future__ import annotations

import pytest

from market import DAY, find_symbol, parse_price, period_millis, widget_bar


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


def test_widget_bar_projects_bid_ask_mid_without_aggregating() -> None:
    message = {
        "time": 1_700_000_000_000,
        "volume": 10,
        "bidOpen": 100.0,
        "bidHigh": 102.0,
        "bidLow": 99.0,
        "bidClose": 101.0,
        "askOpen": 100.2,
        "askHigh": 102.2,
        "askLow": 99.2,
        "askClose": 101.2,
    }
    bid = widget_bar(message, "bid")
    assert bid == {
        "time": 1_700_000_000_000,
        "open": 100.0,
        "high": 102.0,
        "low": 99.0,
        "close": 101.0,
        "volume": 10,
    }
    ask = widget_bar(message, "ask")
    assert ask["open"] == 100.2
    mid = widget_bar(message, "mid")
    assert mid["close"] == pytest.approx(101.1)
    assert widget_bar(None, "bid") is None
    assert widget_bar({"time": 1}, "bid") is None
