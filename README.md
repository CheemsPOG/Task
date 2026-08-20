# TradingView Advanced Charts

Frontend chart (TradingView Advanced Charts) plus a Java Spring Boot backend. The chart datafeed calls the backend; the backend serves **local demo FX data** (historical OHLCV and live candle updates) for the same currency pairs as `/curpairs`. No external market-data API is used.

## Prerequisites

- **Java 21+** (Java 21–26 are fine)
- **Node.js 18+**

Maven is not required globally. The backend ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Run

Use two terminals.

### 1. Backend

```bash
cd backend
mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Wait until you see `Started ChartBackendApplication`. The API listens on **http://127.0.0.1:8080**.

Quick check:

```bash
curl http://127.0.0.1:8080/api/health
```

Expected: `{"status":"ok","service":"chart-backend"}`.

### 2. Frontend

```bash
cd frontend
npm install
npm start
```

Open **http://127.0.0.1:5173**. The chart starts on `USD/JPY`, interval `1D`.

The frontend server also proxies `/api`, `/curpairs`, and `/ws` to the backend, so the browser only talks to port 5173.

## If a port is already in use

**Backend (8080)** — set another port in `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8081
```

Then start the frontend with that port:

```bash
cd frontend
set BACKEND_PORT=8081
npm start
```

PowerShell:

```powershell
cd frontend
$env:BACKEND_PORT = "8081"
npm start
```

**Frontend (5173)** — pick another port:

```powershell
$env:PORT = "5174"
npm start
```

Then open `http://127.0.0.1:5174`.

## What you should see

- Candles and volume for the selected FX pair (live demo ticks)
- Symbol search for the five demo pairs (for example `EUR/USD`)
- Interval switcher (minutes, hours, daily, weekly, monthly)
- Light / dark theme toggle on the right of the chart header
- FX header controls: BID/ASK/MID dropdown and a live simulated quote that follows the chart symbol

## Demo data

Chart history, live candles, `GET /curpairs`, and `ws://.../ws/fx-quotes` are all **local mock services**. Prices are simulated with a random walk (`BID < ASK`, `MID = (BID + ASK) / 2`, about 3 ticks per second). The forming candle close tracks the mock FX mid so the header quote and the chart stay related. The mock can later be replaced with a real FX feed without changing the header UI contract.

Implementation details (APIs, files, dataflow) are in [spec.md](./spec.md).
