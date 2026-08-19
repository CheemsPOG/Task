# TradingView Advanced Charts — Implementation Notes

This document records what was built for the mentor task: a frontend that integrates TradingView Advanced Charts, plus a Java Spring Boot backend whose APIs the chart datafeed calls.

Reference used while implementing: `charting-library-tutorial/` (Binance-backed Advanced Charts datafeed, widget bootstrap, resolution mapping, and realtime kline streaming). Official docs: [Advanced Charts tutorials](https://www.tradingview.com/charting-library-docs/latest/tutorials/).

TradingView library version in `frontend/charting_library/`: **CL v27.006**.

---

## Architecture

```text
Browser (http://127.0.0.1:5173)
  └── TradingView widget + custom JS datafeed
        ├── REST  /api/*     ──► Spring Boot (port 8080) ──► Binance REST
        └── WS    /ws/stream ──► Spring Boot              ──► Binance WebSocket
```

The browser never talks to Binance directly. The frontend only calls our backend. The backend translates TradingView datafeed requests into Binance Spot REST / WebSocket calls (no API key).

Default ports:

| Process | URL | How to change |
|---|---|---|
| Backend | `http://127.0.0.1:8080` | `server.port` in `backend/src/main/resources/application.yml` |
| Frontend | `http://127.0.0.1:5173` | `PORT` env var when running `node server.mjs` |

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

**2. Frontend** (Node.js, no npm install required for the default server):

```bash
cd frontend
node server.mjs
```

Open `http://127.0.0.1:5173`. The chart loads `Binance:ETH/USDT` on `1D` by default.

`server.mjs` serves the TradingView static files and proxies `/api` + `/ws` to Spring Boot so the browser stays same-origin.

Optional: `npm install` then `npm run vite` if you prefer Vite (same proxy).

---

## Backend (`backend/`)

Spring Boot **4.0.7**, Java **21**, Maven Wrapper. Package: `com.task.chart`.

### What it does

- Serves REST APIs that match the TradingView JS Datafeed methods used by Advanced Charts (`onReady`, `getServerTime`, `searchSymbols`, `resolveSymbol`, `getBars`, `getMarks`, `getTimescaleMarks`).
- Proxies historical OHLCV from Binance `api/v3/klines`, paging backwards with `countBack` (same contract as the tutorial: `countBack` outranks `from`; the bar opening exactly on `to` is dropped).
- Caches Binance `exchangeInfo` for 10 minutes and uses it for symbol search / resolve, including `pricescale` from the `PRICE_FILTER` tick size.
- Exposes a WebSocket at `/ws/stream` that subscribes to Binance `<symbol>@kline_<interval>` and pushes live bars to the chart.
- Enables CORS for local frontend origins.

### REST APIs

All under `/api`.

| Method | Path | Purpose | TradingView datafeed hook |
|---|---|---|---|
| GET | `/api/health` | Liveness check | — |
| GET | `/api/config` | Supported resolutions, exchanges, marks, server time flags | `onReady` |
| GET | `/api/time` | Unix time in **seconds** (Binance `api/v3/time`, local clock fallback) | `getServerTime` |
| GET | `/api/search?query=&exchange=&type=&limit=` | Symbol search over Binance spot pairs | `searchSymbols` |
| GET | `/api/symbols?symbol=` | Resolve ticker metadata (`Binance:ETH/USDT`, `ETH/USDT`, or `ETHUSDT`) | `resolveSymbol` |
| GET | `/api/history?symbol=&resolution=&from=&to=&countBack=` | Historical bars `{ time, open, high, low, close, volume }` | `getBars` |
| GET | `/api/marks?...` | Demo bar marks | `getMarks` |
| GET | `/api/timescale-marks?...` | Demo timescale marks | `getTimescaleMarks` |

History response shape:

```json
{
  "s": "ok",
  "bars": [{ "time": 1787097600000, "open": 1917.85, "high": 1929.94, "low": 1906.0, "close": 1924.5, "volume": 55174.73 }],
  "noData": false,
  "errmsg": null
}
```

`s` is `ok`, `no_data`, or `error`. Empty history returns `{ noData: true }` so the library stops paging.

### WebSocket

- URL: `ws://127.0.0.1:8080/ws/stream`
- Client → server:

```json
{ "action": "subscribe", "uid": "sub-1", "symbol": "Binance:ETH/USDT", "resolution": "1" }
{ "action": "unsubscribe", "uid": "sub-1" }
```

- Server → client:

```json
{ "type": "bar", "uid": "sub-1", "bar": { "time": 0, "open": 0, "high": 0, "low": 0, "close": 0, "volume": 0 } }
{ "type": "reset", "uid": "sub-1" }
{ "type": "error", "uid": "sub-1", "message": "..." }
```

`BinanceKlineStreamer` shares one Binance socket, reference-counts streams, and reconnects while subscribers exist.

### Resolution mapping

Copied from the tutorial. Only **native Binance intervals** are requested from `getBars` / kline streams. The library aggregates the rest.

| UI resolution | Backend asks Binance |
|---|---|
| `1S` | `1s` |
| `1`, `3`, `5`, `15`, `30` | `1m`, `3m`, `5m`, `15m`, `30m` |
| `60`, `120`, `240`, `360`, `480`, `720` | `1h` … `12h` |
| `1D`, `3D`, `1W`, `1M` | `1d`, `3d`, `1w`, `1M` |
| `5S`, `15S`, `30S`, `2`, `4`, `10`, `90`, `180` | not requested; library builds them from multipliers |

Symbol `*_multipliers` declare only those native intervals.

### Main backend files

| File | Role |
|---|---|
| `src/main/java/com/task/chart/ChartBackendApplication.java` | Boot entry, enables `@ConfigurationProperties` |
| `config/AppProperties.java` | CORS origins + Binance URLs |
| `config/WebConfig.java` | `RestClient` for Binance + CORS |
| `config/WebSocketConfig.java` | Registers `/ws/stream` |
| `controller/ChartDataController.java` | REST endpoints |
| `service/BinanceClient.java` | Binance REST (`exchangeInfo`, `time`, `klines`) |
| `service/SymbolCatalog.java` | Cached spot symbol list + tick-size → pricescale |
| `service/ResolutionMapper.java` | TradingView resolution → Binance interval |
| `service/ChartDataService.java` | Config, search, resolve, history paging, demo marks |
| `websocket/ChartStreamHandler.java` | Frontend WS protocol |
| `websocket/BinanceKlineStreamer.java` | Shared Binance kline socket |
| `src/main/resources/application.yml` | Port 8080, Binance base URLs, CORS |
| `src/test/java/.../ResolutionMapperTest.java` | Interval mapping unit tests |
| `src/test/java/.../SymbolCatalogTest.java` | Tick-size → pricescale tests |

Verified locally: `/api/health`, `/api/time`, `/api/symbols?symbol=Binance:ETH/USDT`, `/api/history` (ETH/USDT daily bars), `/api/search?query=BTC`.

---

## Frontend (`frontend/`)

Vanilla HTML + ES modules. TradingView assets already present from `static.rar`:

- `charting_library/` — Advanced Charts runtime (widget, bundles, locales)
- `datafeeds/bundle.js` — unused UDF adapter; we implemented a custom JS datafeed so every request goes through Spring Boot

### What it does

- Boots `TradingView.widget` (standalone build) into `#tv_chart_container`.
- Custom datafeed in `src/datafeed/datafeed.js` calls backend REST for config, time, search, resolve, history, and marks.
- Realtime bars go through `src/datafeed/streaming.js` → backend `/ws/stream` (250ms debounce, reconnect, cache-reset after reconnect).
- Theme toggle + documentation button on the chart header (simplified from the tutorial toolbar).
- Default symbol `Binance:ETH/USDT`, interval `1D`, locale `en`, timezone `Etc/UTC`.
- Enabled: `seconds_resolution`, `custom_resolutions`, `allow_arbitrary_symbol_search_input`.
- Disabled: localStorage chart settings (deterministic like the tutorial).

### Main frontend files

| File | Role |
|---|---|
| `index.html` | Page shell, loads `charting_library.standalone.js` then `src/main.js` |
| `server.mjs` | Zero-dependency static server + `/api` HTTP proxy + `/ws` upgrade proxy |
| `vite.config.js` | Optional Vite dev server with the same proxies |
| `src/main.js` | Widget constructor (`library_path: /charting_library/`) |
| `src/api.js` | `fetch` helper for `/api/*` |
| `src/datafeed/datafeed.js` | TradingView IDatafeedChartApi implementation |
| `src/datafeed/streaming.js` | WebSocket subscribe/unsubscribe + reconnect |
| `src/theme.js` | Light/dark overrides + custom toolbar CSS blob |
| `src/toolbar.js` | Header theme switch and docs link |

Widget constructor payload (see `src/main.js`): `symbol`, `interval`, `fullscreen`, `container`, `datafeed`, `library_path`, `locale`, `timezone`, `theme`, `custom_css_url`, `enabled_features`, `disabled_features`, `overrides`.

Verified in the browser: ETH/USDT daily candlesticks + volume rendered, symbol search / header controls available, only leftover console noise was a missing favicon (now stubbed).

---

## Mapping to the tutorial

Kept from `charting-library-tutorial/`:

- Binance as the market-data source (REST klines + WS kline streams).
- Ticker format `Binance:BASE/QUOTE`.
- `countBack`-driven history paging, exclusive `endTime`, drop the bar at `to`.
- Native vs library-aggregated resolutions and `*_multipliers`.
- `supports_time` + `getServerTime` for the price-scale countdown.
- Theme toolbar pattern and disabled localStorage settings.

Changed for this assignment:

- Datafeed talks to **Spring Boot**, not Binance from the browser.
- Trading Platform route (`/trading`, broker sample, quotes, DOM, news, save/load adapter) was **not** implemented — mentor asked for Advanced Charts FE + Spring Boot APIs.
- Custom symbol-status header widget from the tutorial was omitted to keep the FE focused on library + backend integration.

---

## Request flow (one chart load)

1. Widget `onReady` → `GET /api/config`.
2. `resolveSymbol('Binance:ETH/USDT')` → `GET /api/symbols`.
3. `getBars` (often more than once while paging) → `GET /api/history`.
4. `getServerTime` → `GET /api/time`.
5. `subscribeBars` → WS `{ action: "subscribe" }` → backend subscribes to `ethusdt@kline_1d` (or the mapped interval).
6. Live klines are forwarded as `{ type: "bar" }` and applied to the current candle.
7. Optional `getMarks` / `getTimescaleMarks` overlay demo markers from the backend.

---

## Out of scope / constraints

- No database; Binance is the source of truth.
- Binance public endpoints can be geo-restricted; if history fails, check that `api.binance.com` is reachable from the machine running the backend.
- Marks are demo data, not real on-chain or order events.
- `frontend/datafeeds/bundle.js` is the stock UDF client and is not wired up.
- Spring Boot parent is `4.0.7` (Maven Central). Jackson 3 packages (`tools.jackson.databind`) are used because Boot 4 no longer ships `com.fasterxml.jackson` as the default JSON stack.
