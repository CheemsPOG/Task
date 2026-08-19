# TradingView Advanced Charts

Frontend chart (TradingView Advanced Charts) plus a Java Spring Boot backend. The chart datafeed calls the backend; the backend loads **real Binance spot market data** (historical klines and live candle updates). No Binance API key is required.

## Prerequisites

- **Java 21+** (Java 21–26 are fine)
- **Node.js 18+** (used only to serve the frontend; no `npm install` needed)

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
node server.mjs
```

Open **http://127.0.0.1:5173**. The chart starts on `Binance:ETH/USDT`, interval `1D`.

The frontend server also proxies `/api` and `/ws` to the backend, so the browser only talks to port 5173.

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
node server.mjs
```

PowerShell:

```powershell
cd frontend
$env:BACKEND_PORT = "8081"
node server.mjs
```

**Frontend (5173)** — pick another port:

```powershell
$env:PORT = "5174"
node server.mjs
```

Then open `http://127.0.0.1:5174`.

## What you should see

- Candles and volume for the selected Binance pair (live data while the market is updating)
- Symbol search for other Binance spot pairs (for example `BTC/USDT`)
- Interval switcher (minutes, hours, daily, weekly, monthly)
- Light / dark theme toggle on the right of the chart header

Implementation details (APIs, files, dataflow) are in [spec.md](./spec.md).
