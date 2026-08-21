# TradingView Advanced Charts

Frontend chart (TradingView Advanced Charts), a Java Spring Boot REST backend, and a Python WebSocket server. The chart datafeed calls Java for history and Python for live ticks. Both serve **local demo FX data** for the same currency pairs as `/curpairs`. No external market-data API is used.

How the app is built (config, database, APIs 120–123, how to test): [`structure.md`](structure.md).

## Prerequisites

- **Java 21+** (Java 21–26 are fine)
- **Python 3.10+**
- **Node.js 18+**
- **Docker** (PostgreSQL for Java REST)

Maven is not required globally. The backend ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Run

Use four terminals (Postgres first).

### 0. PostgreSQL

From the repo root (Docker Desktop must be running):

```bash
docker compose up -d
```

DBeaver: host `127.0.0.1`, port `5432`, database `chart`, user `chart`, password `chart`.

Flyway creates `m_ccypairs` and `m_season` the first time Java starts.

### 1. Java REST backend

```bash
cd backend
mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
cd backend
./mvnw spring-boot:run
```

Wait until you see `Started ChartBackendApplication`. REST listens on **http://127.0.0.1:8080**.

Quick check:

```bash
curl http://127.0.0.1:8080/api/health
```

Expected: `{"status":"ok","service":"chart-backend"}` (no auth header).

Chart datafeed routes under `/api` (except `/health` and `/api/auth/login`) require:

```http
Authorization: Bearer <jwt>
Accept-Language: en
```

Login (no Bearer):

```bash
curl -s -X POST http://127.0.0.1:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"demo\",\"password\":\"demo\"}"
```

Then:

```bash
curl -s http://127.0.0.1:8080/api/config -H "Authorization: Bearer <accessToken>"
```

Without Bearer → **401** `{ "errorCode": "E_UNAUTHORIZED", "message": "..." }`.  
With `Accept-Language: ja` → Japanese `message`.

Demo users: `demo` / `demo` (customer 1), `demo2` / `demo2` (customer 2).

### 2. Python WebSocket server

```bash
cd ws-python
python -m pip install -r requirements.txt
python server.py
```

Live sockets listen on **ws://127.0.0.1:8081** (`/ws/fx-quotes` and `/ws/stream`).

### 3. Frontend

```bash
cd frontend
npm install
npm start
```

Open **http://127.0.0.1:5173**. A login overlay appears first — use **Demo** (`demo` / `demo`) or type credentials, then the chart loads on `USD/JPY`, interval `1D`. **Logout** is on the chart header (reloads to the login form).

The frontend server proxies `/api` and `/curpairs` to Java (8080) and `/ws` to Python (8081), so the browser only talks to port 5173.

## If a port is already in use

**Java REST (8080)** — set another port in `backend/src/main/resources/application.yml`:

```yaml
server:
  port: 8090
```

Then start the frontend with that port:

```bash
cd frontend
set BACKEND_PORT=8090
npm start
```

PowerShell:

```powershell
cd frontend
$env:BACKEND_PORT = "8090"
npm start
```

**Python WebSocket (8081)** — start with another port:

```powershell
cd ws-python
$env:WS_PORT = "8082"
python server.py
```

Then start the frontend with `$env:WS_PORT = "8082"`.

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
- Light / dark theme toggle and **Logout** on the right of the chart header
- Login overlay before the chart (local JWT; not Peach S-01)
- FX header controls: BID/ASK/MID dropdown and a live simulated quote that follows the chart symbol. Switching BID/ASK/MID reloads that pair's candles from that price side.

## Demo data

Chart history, live candles, `GET /curpairs`, and `ws://.../ws/fx-quotes` are **local mocks**. The generator walks **BID**, sets **ASK = BID + spread**, and **MID = (BID + ASK) / 2** (~3 ticks per second). The forming candle close follows the selected BID/ASK/MID. A real FX feed can replace the generator later if it keeps the same `/curpairs` + quote JSON.

Details, FX WebSocket contract, and a verification checklist: [spec.md](./spec.md).
