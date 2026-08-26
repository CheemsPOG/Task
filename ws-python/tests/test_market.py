from __future__ import annotations

from market import DAY, find_symbol, parse_price, period_millis


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
