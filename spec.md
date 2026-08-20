# TradingView Advanced Charts — Spec

Frontend: TradingView Advanced Charts (CL v27.006) in TypeScript.  
REST backend: Java 21 Spring Boot 4.0.7 (`/api`, `/curpairs`).  
Live sockets: Python (`/ws/stream`, `/ws/fx-quotes`). All market data is **local mock**. There is no Binance or external FX provider.

How to run: see [README.md](./README.md). Java REST `http://127.0.0.1:8080`, Python WebSocket `ws://127.0.0.1:8081`, frontend `http://127.0.0.1:5173` (Vite proxies `/api` and `/curpairs` to Java, `/ws` to Python).

---

## What was built

- Advanced Charts widget (not Trading Platform): candles, volume, interval switcher, symbol search, light/dark theme.
- Default chart: `USD/JPY`, interval `1D`. Exchange name is hidden. Pair is changed with TradingView’s symbol control (no extra pair dropdown).
- Header: **BID / ASK / MID** dropdown + live price for the **current chart symbol**.
- Switching BID / ASK / MID reloads that pair’s candle series (`price=bid|ask|mid` on history and `/ws/stream`).
- Chart symbols are the five `/curpairs` FX pairs. Names come from that API, never a hardcoded `curpairCd → name` map on the frontend.

---

## Architecture

```text
Browser (5173)
  └── Widget + datafeed + FX header
        ├── GET  /api/*         → Java REST (chart config, search, history)
        ├── GET  /curpairs      → Java REST (FX pair catalog)
        ├── WS   /ws/stream     → Python (live candles: bid, ask, or mid)
        └── WS   /ws/fx-quotes  → Python (live BID/ASK quotes, ~3/s)
```

Two WebSockets. Do not mix their payloads.

---

## How mock data works

**Quotes** (`ws-python/market.py`, ~every 333ms):

1. Random-walk **BID**.
2. **ASK = BID + spread** (`BID < ASK`).
3. **MID = (BID + ASK) / 2** (derived; not walked on its own).
4. `high` / `low` on the quote are running session extremes (ask high, bid low).

A future real feed that only has BID and ASK can replace this generator and still emit the same JSON with `mid` computed the same way. The UI does not need to change.

**Candles** (`MockBarGenerator` for REST history, `ws-python` for live bars):

- Built from a **BID price path**. ASK bars use `bid + spread`. MID bars use `(bid + ask) / 2`.
- ASK High is stretched up and BID Low down so the legend H/L is easy to check when switching modes.
- Forming-candle **Close** follows the live quote for the selected mode (header number ≈ latest Close).
- History is deterministic (same symbol, period, `to`, `countBack` → same bars). No database.

Demo pairs and spreads live in `DemoMarket` / `CurrencyPairService`.

---

## Chart REST (`/api`)

| Method | Path | Role |
|---|---|---|
| GET | `/api/health` | Liveness |
| GET | `/api/config` | Resolutions, `supports_time`, no marks |
| GET | `/api/time` | Unix time in seconds |
| GET | `/api/search` | Search the five FX pairs |
| GET | `/api/symbols?symbol=` | Resolve `USD/JPY`, `USDJPY`, or `FX:USD/JPY` |
| GET | `/api/history?symbol=&resolution=&from=&to=&countBack=&price=` | OHLCV. `price` = `bid` \| `ask` \| `mid` (default `mid`) |

History `s` is `ok`, `no_data`, or `error`. The bar opening exactly on `to` is dropped. Native intervals are served; the library aggregates the rest (`2`, `10`, `90`, …).

**Chart live socket** `ws://.../ws/stream`:

```json
{ "action": "subscribe", "uid": "sub-1", "symbol": "USD/JPY", "resolution": "1", "price": "bid" }
{ "action": "unsubscribe", "uid": "sub-1" }
```

```json
{ "type": "bar", "uid": "sub-1", "bar": { "time": 0, "open": 0, "high": 0, "low": 0, "close": 0, "volume": 0 } }
{ "type": "reset", "uid": "sub-1" }
{ "type": "error", "uid": "sub-1", "message": "..." }
```

---

## FX contract (complete prompt)

Use this as the pairing-catalog + quote-stream spec. The backend **implements** it; the frontend **consumes** it.

### Pair catalog

`GET /curpairs` (not under `/api`). Returns the list of currency pairs. This list is the **only** source of truth for pair identity and display names. Do not hard-code `curpairCd` → name.

```json
[
  { "curpairCd": 1, "curpairName": "USDJPY", "curpairDisplay": "USD/JPY" },
  { "curpairCd": 2, "curpairName": "EURJPY", "curpairDisplay": "EUR/JPY" },
  { "curpairCd": 3, "curpairName": "EURUSD", "curpairDisplay": "EUR/USD" },
  { "curpairCd": 4, "curpairName": "GBPUSD", "curpairDisplay": "GBP/USD" },
  { "curpairCd": 5, "curpairName": "AUDUSD", "curpairDisplay": "AUD/USD" }
]
```

| Field | Type | Meaning |
|---|---|---|
| `curpairCd` | number | Pair id. WebSocket sends this as a **string**. |
| `curpairName` | string | Compact code (`USDJPY`) |
| `curpairDisplay` | string | Label (`USD/JPY`) |

### Quote WebSocket

- URL: `ws://127.0.0.1:8081/ws/fx-quotes` (browser uses same-origin `/ws/fx-quotes` via Vite).
- Server pushes; the client does not send subscribe messages.
- On connect, the server sends a **snapshot** (one message per pair), then live ticks.
- Frequency: about **3 times per second** (333ms) for **every** pair.
- Each message is one pair (not an array):

```json
{
  "curpairCd": "1",
  "rateMiliSecondUTC": 1787195533139,
  "bid": 158.456,
  "ask": 158.458,
  "mid": 158.457,
  "high": 158.766,
  "low": 158.036
}
```

| Field | Type | Meaning |
|---|---|---|
| `curpairCd` | string | Must be looked up in `/curpairs` (example `"1"` → USD/JPY) |
| `rateMiliSecondUTC` | number | Quote time, milliseconds UTC |
| `bid` | number | Bid. Always **strictly less than** `ask` |
| `ask` | number | Ask |
| `mid` | number | **`(bid + ask) / 2`**. Mock (and a future adapter) compute this even if the raw feed only has bid/ask |
| `high` | number | Session high of ask (so far) |
| `low` | number | Session low of bid (so far) |

### Mapping and display

1. Call `GET /curpairs` once and keep `curpairCd` → `{ curpairName, curpairDisplay }`.
2. On each WebSocket message, read `curpairCd` (string).
3. Resolve the pair from the catalog. If the code is unknown, ignore the tick.
4. Show the quote on screen (this app: header BID/ASK/MID + live number for the **chart’s current symbol**, matched via `curpairDisplay` / `curpairName`).

Replace `MockFxQuoteService` later with a real provider; keep this JSON and `/curpairs` and the UI stays the same.

---

## Verification checklist

Mentor FX contract:

- [ ] `GET /curpairs` returns the five pairs with `curpairCd`, `curpairName`, `curpairDisplay` (example `1` / `USDJPY` / `USD/JPY`).
- [ ] Frontend loads `/curpairs` and never hard-codes `curpairCd === "1"` → USD/JPY.
- [ ] `/ws/fx-quotes` messages include `curpairCd` as a **string**, plus `rateMiliSecondUTC`, `bid`, `ask`, `mid`, `high`, `low`.
- [ ] Each tick: `bid < ask` and `mid ≈ (bid + ask) / 2`.
- [ ] Ticks arrive about **3 times per second**.
- [ ] Header live number uses the mapped pair for the current chart symbol and the selected BID / ASK / MID field.
- [ ] Changing the TradingView symbol (e.g. to EUR/USD) maps the new pair from `/curpairs` and shows that pair’s quotes.

Chart + mock:

- [ ] Default chart is `USD/JPY` `1D`; no “Binance” label.
- [ ] Symbol search finds the five FX pairs only.
- [ ] BID / ASK / MID reloads candles; ASK Open ≈ BID Open + spread; ASK High > MID High; BID Low < MID Low.
- [ ] Latest candle Close tracks the header number for the selected mode.
- [ ] Theme toggle and interval switcher still work.
- [ ] `/api/health` returns `{"status":"ok","service":"chart-backend"}`.

---

## Main files

| Area | Files |
|---|---|
| Chart REST | `controller/ChartDataController.java`, `service/ChartDataService.java` |
| Pair catalog | `controller/CurrencyPairController.java`, `service/CurrencyPairService.java` |
| Mock quotes | `ws-python/market.py`, `ws-python/server.py` (`/ws/fx-quotes`) |
| Mock candles | `service/DemoMarket.java`, `service/MockBarGenerator.java` (history); `ws-python` live bars (`/ws/stream`) |
| Chart WS | `ws-python/server.py` |
| Widget | `frontend/src/main.ts`, `datafeed/datafeed.ts`, `datafeed/streaming.ts` |
| FX header | `frontend/src/fx/currencyPairs.ts`, `quoteStore.ts`, `fxQuotesSocket.ts`, `quoteToolbar.ts` |

---

## Out of scope

- Trading Platform (`/trading`, broker, DOM, watchlist).
- Database; persistence of quotes in the browser (in-memory only; localStorage chart settings are off).
- Marks APIs (`supports_marks: false`).
- `frontend/datafeeds/bundle.js` (unused UDF client).
