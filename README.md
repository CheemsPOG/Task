# TradingView Advanced Charts

Frontend chart (TradingView Advanced Charts), a Java Spring Boot REST backend, and a Python WebSocket server. The chart datafeed calls Java for history and Python for live ticks. Both serve **local demo FX data** for the same currency pairs as `/curpairs`. No external market-data API is used.

How the app is built (config, database, APIs 120–139, how to test): [`structure.md`](structure.md).

## Prerequisites

- **Java 21+** (Java 21–26 are fine)
- **Python 3.10+**
- **Node.js 18+**
- **Docker** (PostgreSQL + Redis for Java REST)

Maven is not required globally. The backend ships with the Maven Wrapper (`mvnw` / `mvnw.cmd`).

## Run

Use four terminals (Postgres + Redis first).

### 0. PostgreSQL + Redis

From the repo root (Docker Desktop must be running):

```bash
docker compose up -d
```

That starts:

| Service | Port | Use |
|---|---|---|
| Postgres | `5432` | Flyway / JPA |
| Redis | `6379` | Doc 121 Peach `cache_set_*` bar cache |

DBeaver: host `127.0.0.1`, port `5432`, database `chart`, user `chart`, password `chart`.

Redis CLI peek (after Java has seeded):

```bash
docker compose exec redis redis-cli ZCARD peach:cache_set_day:USDJPY
docker compose exec redis redis-cli ZRANGE peach:cache_set_day:USDJPY 0 2 WITHSCORES
```

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

Chart datafeed routes under `/api` (except `/health`, `/api/auth/login`, `/api/auth/refresh`, and `/api/auth/logout`) require:

```http
Authorization: Bearer <jwt>
Accept-Language: en
```

Login (no Bearer; also sets HttpOnly refresh cookie — use `-c`/`-b` with curl if testing refresh):

```bash
curl -s -c cookies.txt -X POST http://127.0.0.1:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"demo\",\"password\":\"demo\"}"
```

Response includes `accessToken` (1h), `expiresIn: 3600`, and `refreshExpiresIn: 86400`. Refresh token is in the cookie only.

Refresh (cookie only, no Bearer):

```bash
curl -s -b cookies.txt -c cookies.txt -X POST http://127.0.0.1:8080/api/auth/refresh
```

Logout:

```bash
curl -s -b cookies.txt -X POST http://127.0.0.1:8080/api/auth/logout
```

Then:

```bash
curl -s http://127.0.0.1:8080/api/config -H "Authorization: Bearer <accessToken>"
```

Without Bearer → **401** `{ "errorCode": "E_UNAUTHORIZED", "message": "..." }`.  
With `Accept-Language: ja` → Japanese `message`.

Demo users: `demo` / `demo` (customer 1), `demo2` / `demo2` (customer 2).

### Swagger UI (mentor API review)

After Java is running, open one of:

| What | URL |
|------|-----|
| Swagger UI | http://127.0.0.1:8080/swagger-ui.html |
| Same UI (Baeldung default path) | http://127.0.0.1:8080/swagger-ui/index.html |
| OpenAPI JSON | http://127.0.0.1:8080/v3/api-docs |
| OpenAPI YAML | http://127.0.0.1:8080/v3/api-docs.yaml |

This lists every REST endpoint (docs 120–139), grouped by tag. You can try them in the browser.

1. Expand **Auth** → `POST /api/auth/login` → **Try it out** (example body is pre-filled: `demo` / `demo`).
2. **Execute** → copy `accessToken` from the response.
3. Click **Authorize** (lock icon at the top) → paste the token only (no `Bearer ` prefix) → **Authorize** → **Close**.
4. Expand **Datafeed (120–126)** or **Chart layouts (127–131)** → **Try it out** → **Execute**. Swagger sends `Authorization: Bearer …` for you.

Swagger uses the access token only. The browser app at `:5173` also stores a 1-day HttpOnly refresh cookie for silent re-login and server-side logout (`POST /api/auth/refresh`, `POST /api/auth/logout`).

If the UI says “Failed to load remote configuration”, open `/v3/api-docs` first. A JSON document means docs are up; then reload Swagger UI.

#### Mentor on another PC

The mentor does **not** install Swagger. They open the UI in a browser against **your** running backend.

On the machine that hosts Java + Docker:

1. `docker compose up -d` then `cd backend` and `mvnw.cmd spring-boot:run` (leave it running).
2. Find this PC’s LAN IP (Windows: `ipconfig` → IPv4, e.g. `192.168.1.20`).
3. Allow inbound **TCP 8080** in Windows Firewall if the mentor cannot connect.
4. Mentor opens **http://192.168.1.20:8080/swagger-ui.html** and follows the login + Authorize steps above.

Both PCs must be on the same network (or you must port-forward 8080). Postgres and Redis stay on the host; the mentor only needs port 8080.

Do **not** expose 8080 to the public internet — the JWT secret and `demo`/`demo` users are local demo credentials.

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

Open **http://127.0.0.1:5173**. A login overlay appears first — use **Demo** (`demo` / `demo`) or type credentials, then the chart loads on `USD/JPY`, interval `1D`. If you logged in within the last day, reopening the tab may skip login via silent refresh (HttpOnly cookie). **Logout** on the chart header revokes the refresh token server-side and reloads to the login form.

The frontend server proxies `/api` and `/curpairs` to Java (8080) and `/ws` to Python (8081), so the browser only talks to port 5173. Auth cookies are forwarded through the Vite proxy (`credentials: 'include'`).

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


# How to Run

## This PC

### 1. Start PostgreSQL + Redis

From the repository root:

~~~bash
docker compose up -d
~~~

### 2. Start Java

Open a new terminal:

~~~powershell
cd "d:\Personal Projects\New\Task\backend"
.\mvnw.cmd spring-boot:run
~~~

Wait for:

~~~text
Started ChartBackendApplication
~~~

### 3. Open API Documentation

Open Swagger UI in your browser:

- **UI:** http://127.0.0.1:8080/swagger-ui.html
- **Alternative UI:** http://127.0.0.1:8080/swagger-ui/index.html
- **JSON catalog:** http://127.0.0.1:8080/v3/api-docs
- **YAML:** http://127.0.0.1:8080/v3/api-docs.yaml

You should see the following tags:

- **Auth**
- **Datafeed** (120–126)
- **Chart layouts** (127–131)
- **Indicator templates** (132–135)
- **Chart templates** (136–139)
- **Currency pairs**

These represent the implemented API list.

### 4. Get an Access Token

1. Go to **Auth → `POST /api/auth/login`**.
2. Click **Try it out**.
3. The request body is already configured with:

~~~json
{
  "username": "demo",
  "password": "demo"
}
~~~

4. Click **Execute**.
5. Copy the `accessToken` from the response.
6. Click **Authorize** in the top-right corner.
7. Paste the token **without** `Bearer `.
8. Click **Authorize**, then **Close**.

### 5. Call a Protected API

For example:

**Datafeed → `GET /api/config`**

1. Click **Try it out**.
2. Click **Execute**.
3. You should receive:

~~~text
200 OK
~~~

Without authorization, the same request should return:

~~~text
401 Unauthorized
~~~

---

# Mentor on Another PC

The mentor only needs a web browser.

On your PC, keep both **Docker** and **Spring Boot** running.

### 1. Find Your Local IP Address

Open Command Prompt or PowerShell:

~~~powershell
ipconfig
~~~

Find your **IPv4 Address**, for example:

~~~text
192.168.1.20
~~~

### 2. Allow TCP Port 8080

If necessary, allow **TCP port 8080** through Windows Firewall.

### 3. Open Swagger

Your mentor can then open:

~~~text
http://192.168.1.20:8080/swagger-ui.html
~~~

The mentor can use the same authentication process:

~~~text
Username: demo
Password: demo
~~~

Then use **Authorize** with the returned `accessToken`.

### Requirements

The mentor and your PC must be on the **same LAN**.

Alternatively, port forwarding can be used if appropriate.

> **Security:** Do not expose port `8080` directly to the public Internet.