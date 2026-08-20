# TradingView Advanced Charts — Implementation Notes

This document records what was built for the mentor task: a frontend that integrates TradingView Advanced Charts, plus a Java Spring Boot backend whose APIs the chart datafeed calls.

Reference used while implementing: `charting-library-tutorial/` (Advanced Charts datafeed, widget bootstrap, resolution mapping, and realtime bar streaming). Official docs: [Advanced Charts tutorials](https://www.tradingview.com/charting-library-docs/latest/tutorials/).

TradingView library version in `frontend/charting_library/`: **CL v27.006**.

---

## Architecture

```text
Browser (http://127.0.0.1:5173)
  └── TradingView widget + TypeScript datafeed + FX header controls
        ├── REST  /api/*        ──► Spring Boot demo catalog + OHLCV
        ├── WS    /ws/stream    ──► Spring Boot live demo candles
        ├── REST  /curpairs     ──► Spring Boot mock FX pair catalog
        └── WS    /ws/fx-quotes ──► Spring Boot mock FX quote generator
```

The frontend only calls our backend. Chart history, live candles, and BID/ASK/MID quotes are generated locally from the same five demo FX pairs.

The `/curpairs` endpoint and `/ws/fx-quotes` socket follow the mentor-provided FX contract so a real provider can replace them later. BID/ASK/MID values are simulated.

Default ports:

| Process | URL | How to change |
|---|---|---|
| Backend | `http://127.0.0.1:8080` | `server.port` in `backend/src/main/resources/application.yml` |
| Frontend | `http://127.0.0.1:5173` | `PORT` env var when running `npm start` |

If 8080 is already in use, set backend `server.port` (for example `8081`) and start the frontend with `BACKEND_PORT=8081`.

---

## How to run

**1. Backend** (Java 21+; Maven Wrapper is included, no global Maven install needed):

```bash
cd backend
mvnw.cmd spring-boot:run
```

On macOS/Linux: `./mvnw spring-boot:run`.

Confirm: `GET http://127.0.0.1:8080/api/health` → `{"status":"ok","service":"chart-backend"}`.

**2. Frontend** (Node.js; `npm install` once, TypeScript served by Vite):

```bash
cd frontend
npm install
npm start
```

Open `http://127.0.0.1:5173`. The chart loads `USD/JPY` on `1D` by default.

Vite serves the TypeScript sources, TradingView static files, and proxies `/api` + `/ws` to Spring Boot so the browser stays same-origin.

---

## Backend (`backend/`)

Spring Boot **4.0.7**, Java **21**, Maven Wrapper. Package: `com.task.chart`.

### What it does

- Serves REST APIs that match the TradingView JS Datafeed methods used by Advanced Charts (`onReady`, `getServerTime`, `searchSymbols`, `resolveSymbol`, `getBars`).
- Generates historical OHLCV for the demo FX catalog, paging backwards with `countBack` (same contract as the tutorial: `countBack` outranks `from`; the bar opening exactly on `to` is dropped).
- Resolves symbols from the `/curpairs` catalog (`USD/JPY`, `USDJPY`, or `FX:USD/JPY`).
- Exposes a WebSocket at `/ws/stream` that pushes live demo bars. The forming candle close follows the mock FX mid.
- Serves a mock `GET /curpairs` catalog and a separate mock FX WebSocket at `/ws/fx-quotes` (~3 quote ticks per second, BID/ASK/MID/HIGH/LOW).
- Enables CORS for local frontend origins.

### REST APIs

All under `/api`.

| Method | Path | Purpose | TradingView datafeed hook |
|---|---|---|---|
| GET | `/api/health` | Liveness check | — |
| GET | `/api/config` | Supported resolutions, symbol types, marks, server time flags | `onReady` |
| GET | `/api/time` | Unix time in **seconds** (local clock) | `getServerTime` |
| GET | `/api/search?query=&exchange=&type=&limit=` | Symbol search over the demo FX pairs | `searchSymbols` |
| GET | `/api/symbols?symbol=` | Resolve ticker metadata (`USD/JPY`, `USDJPY`, or `FX:USD/JPY`) | `resolveSymbol` |
| GET | `/api/history?symbol=&resolution=&from=&to=&countBack=` | Historical bars `{ time, open, high, low, close, volume }` | `getBars` |

### Mock FX API

`GET /curpairs` is a **mock** currency-pair catalog created for this assignment. It is not an external FX provider. The frontend must use this response as the source of truth for pair names (`curpairCd` → `curpairName` / `curpairDisplay`). The same catalog feeds chart search / resolve.

Example:

```json
[
  { "curpairCd": 1, "curpairName": "USDJPY", "curpairDisplay": "USD/JPY" },
  { "curpairCd": 2, "curpairName": "EURJPY", "curpairDisplay": "EUR/JPY" },
  { "curpairCd": 3, "curpairName": "EURUSD", "curpairDisplay": "EUR/USD" },
  { "curpairCd": 4, "curpairName": "GBPUSD", "curpairDisplay": "GBP/USD" },
  { "curpairCd": 5, "curpairName": "AUDUSD", "curpairDisplay": "AUD/USD" }
]
```

History response shape:

```json
{
  "s": "ok",
  "bars": [{ "time": 1787097600000, "open": 149.81, "high": 149.92, "low": 149.74, "close": 149.85, "volume": 512 }],
  "noData": false,
  "errmsg": null
}
```

`s` is `ok`, `no_data`, or `error`. Empty history returns `{ noData: true }` so the library stops paging.

### WebSocket

- URL: `ws://127.0.0.1:8080/ws/stream`
- Client → server:

```json
{ "action": "subscribe", "uid": "sub-1", "symbol": "USD/JPY", "resolution": "1" }
{ "action": "unsubscribe", "uid": "sub-1" }
```

- Server → client:

```json
{ "type": "bar", "uid": "sub-1", "bar": { "time": 0, "open": 0, "high": 0, "low": 0, "close": 0, "volume": 0 } }
{ "type": "reset", "uid": "sub-1" }
{ "type": "error", "uid": "sub-1", "message": "..." }
```

`MockKlineStreamer` builds live candles from the mock FX mid for each subscribed symbol and resolution.

### Mock FX WebSocket

- URL: `ws://127.0.0.1:8080/ws/fx-quotes` (proxied as `/ws/fx-quotes` from the frontend)
- Separate from `/ws/stream`. Do not mix message formats.
- Mock quotes are generated locally with a random walk. `MID = (BID + ASK) / 2` and `BID < ASK`.
- Updates about 3 times per second. Each message:

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

`curpairCd` `"1"` resolves through `/curpairs` to `USDJPY` / `USD/JPY`. The frontend never hard-codes that mapping.

The mock API and WebSocket follow the mentor-provided contract so a real FX feed can replace the generator later without changing the UI contract.

### Resolution mapping

Only **native intervals** are requested from `getBars` / live streams. The library aggregates the rest.

| UI resolution | Native period |
|---|---|
| `1S` | 1 second |
| `1`, `3`, `5`, `15`, `30` | 1m, 3m, 5m, 15m, 30m |
| `60`, `120`, `240`, `360`, `480`, `720` | 1h … 12h |
| `1D`, `3D`, `1W`, `1M` | 1d, 3d, 1w, 30d |
| `5S`, `15S`, `30S`, `2`, `4`, `10`, `90`, `180` | not requested; library builds them from multipliers |

Symbol `*_multipliers` declare only those native intervals.

### Main backend files

| File | Role |
|---|---|
| `src/main/java/com/task/chart/ChartBackendApplication.java` | Boot entry, enables `@ConfigurationProperties` |
| `config/AppProperties.java` | CORS origins |
| `config/WebConfig.java` | CORS |
| `config/WebSocketConfig.java` | Registers `/ws/stream` and `/ws/fx-quotes` |
| `controller/ChartDataController.java` | REST endpoints |
| `service/CurrencyPairService.java` | Demo FX pair catalog |
| `service/SymbolCatalog.java` | Chart symbols from the FX catalog |
| `service/ResolutionMapper.java` | TradingView resolution → native period |
| `service/MockBarGenerator.java` | Deterministic demo OHLCV |
| `service/MockFxQuoteService.java` | Simulated BID/ASK/MID |
| `service/ChartDataService.java` | Config, search, resolve, history paging |
| `websocket/ChartStreamHandler.java` | Frontend WS protocol |
| `websocket/MockKlineStreamer.java` | Live demo candles from FX mid |
| `src/main/resources/application.yml` | Port 8080, CORS |
| `src/test/java/.../ResolutionMapperTest.java` | Interval mapping unit tests |
| `src/test/java/.../SymbolCatalogTest.java` | Demo pair resolve tests |
| `src/test/java/.../MockBarGeneratorTest.java` | History paging tests |

Verified locally: `/api/health`, `/api/time`, `/api/symbols?symbol=USD/JPY`, `/api/history` (USD/JPY daily bars), `/api/search?query=EUR`.

---

## Frontend (`frontend/`)

Vanilla HTML + TypeScript ES modules. TradingView assets already present from `static.rar`:

- `charting_library/` — Advanced Charts runtime (widget, bundles, locales)
- `datafeeds/bundle.js` — unused UDF adapter; we implemented a custom TypeScript datafeed so every request goes through Spring Boot

### What it does

- Boots `TradingView.widget` (standalone build) into `#tv_chart_container`.
- Custom datafeed in `src/datafeed/datafeed.ts` calls backend REST for config, time, search, resolve, and history.
- Realtime bars go through `src/datafeed/streaming.ts` → backend `/ws/stream` (250ms debounce, reconnect, cache-reset after reconnect).
- Theme toggle on the chart header (simplified from the tutorial toolbar).
- Mock FX header controls (TradingView dropdown, left of the header): BID/ASK/MID selector and a live price for the chart's current symbol. The pair comes from TradingView's change-symbol control; `/curpairs` is used only to map WebSocket `curpairCd` values.
- Default symbol `USD/JPY`, interval `1D`, locale `en`, timezone `Etc/UTC`.
- Legend exchange label is hidden (`mainSeriesProperties.statusViewStyle.showExchange: false`).
- Enabled: `seconds_resolution`, `custom_resolutions`, `allow_arbitrary_symbol_search_input`.
- Disabled: localStorage chart settings (deterministic like the tutorial).

### Main frontend files

| File | Role |
|---|---|
| `index.html` | Page shell, loads `charting_library.standalone.js` then `src/main.ts` |
| `server.ts` / `vite.config.ts` | Vite dev server + `/api` HTTP proxy + `/ws` upgrade proxy |
| `src/main.ts` | Widget constructor (`library_path: /charting_library/`) |
| `src/api.ts` | `fetch` helper for `/api/*` |
| `src/datafeed/datafeed.ts` | TradingView IDatafeedChartApi implementation |
| `src/datafeed/streaming.ts` | WebSocket subscribe/unsubscribe + reconnect |
| `src/theme.ts` | Light/dark overrides + custom toolbar CSS blob |
| `src/toolbar.ts` | Header theme switch |
| `src/fx/quoteStore.ts` | Realtime FX quote state (latest quote per pair, selected pair/mode) |
| `src/fx/fxQuotesSocket.ts` | Mock FX WebSocket client + reconnect |
| `src/fx/quoteToolbar.ts` | BID/ASK/MID dropdown + live price for the chart symbol |

Widget constructor payload (see `src/main.ts`): `symbol`, `interval`, `fullscreen`, `container`, `datafeed`, `library_path`, `locale`, `timezone`, `theme`, `custom_css_url`, `enabled_features`, `disabled_features`, `overrides`.

Verified in the browser: USD/JPY daily candlesticks + volume rendered, symbol search / header controls available.

---

## Mapping to the tutorial

Kept from `charting-library-tutorial/`:

- `countBack`-driven history paging, exclusive `endTime`, drop the bar at `to`.
- Native vs library-aggregated resolutions and `*_multipliers`.
- `supports_time` + `getServerTime` for the price-scale countdown.
- Theme toolbar pattern and disabled localStorage settings.

Changed for this assignment:

- Datafeed talks to **Spring Boot** demo data, not an external exchange from the browser.
- Chart symbols are the five demo FX pairs (`USD/JPY`, …), not crypto tickers.
- Trading Platform route (`/trading`, broker sample, quotes, DOM, news, save/load adapter) was **not** implemented — mentor asked for Advanced Charts FE + Spring Boot APIs.
- Custom symbol-status header widget from the tutorial was omitted to keep the FE focused on library + backend integration.

---

## Request flow (one chart load)

1. Widget `onReady` → `GET /api/config`.
2. `resolveSymbol('USD/JPY')` → `GET /api/symbols`.
3. `getBars` (often more than once while paging) → `GET /api/history`.
4. `getServerTime` → `GET /api/time`.
5. `subscribeBars` → WS `{ action: "subscribe" }` → backend starts live demo candles for that pair and resolution.
6. Live bars are forwarded as `{ type: "bar" }` and applied to the current candle.
7. Header FX controls load `GET /curpairs`, then subscribe to `/ws/fx-quotes` for simulated BID/ASK/MID.

---

## Out of scope / constraints

- No database; demo OHLCV and mock FX quotes are generated in-process.
- Marks APIs are disabled (`supports_marks: false`).
- `frontend/datafeeds/bundle.js` is the stock UDF client and is not wired up.
- Spring Boot parent is `4.0.7` (Maven Central). Jackson 3 packages (`tools.jackson.databind`) are used because Boot 4 no longer ships `com.fasterxml.jackson` as the default JSON stack.
