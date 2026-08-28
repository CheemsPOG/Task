# Mentor presentation script — TradingView chart demo (docs 120–139)

**What this file is:** a **spoken walkthrough**. Same facts as [`structure.md`](structure.md), but written so you can present with **zero assumed context**. Read a section aloud, then demo the **Show** steps.

**Companion files (do not skip):**

| File | Use in the room |
|------|-----------------|
| [`structure.md`](structure.md) | Topology, JWT §6.1, warehouse map |
| [`backend/src/main/java/README.md`](backend/src/main/java/README.md) | Class tree + layering |
| [`checklist.md`](checklist.md) | Done vs Open vs intentional gap |
| [`test.md`](test.md) | Postman/DBeaver per API (same facts as **Part K**, more SQL) |
| This file **Part K** (bottom) | Swagger Try it out for **120–139**: steps, expected JSON, MD match, DBeaver |
| [`README.md`](README.md) | How to start the four processes |
| [`System_Overview_Design/`](System_Overview_Design/) | English specs 120–139 (source of “what Peach asked”) |

**Suggested time:** ~5 min open + demo → ~3 min per datafeed doc (120–126) → ~2 min per CRUD group (127–131, 132–135, 136–139) → extras (auth, quotes, frontend) → gaps. Skip a doc’s **Show** if time is short; never skip saying **what is extra vs the spec**.

---

## How to use each section

Every numbered slice below uses the same four lines:

1. **Say** — what you tell the mentor (plain language).
2. **Spec** — what the markdown in `System_Overview_Design_*` asks.
3. **Code** — first files to open (controller → service → table).
4. **Show / Test** — click, curl, or Maven class that proves it.
5. **Honest** — demo stub, extra field, or not wired to the widget.

---

# Part 0 — Opening (mentor has no context)

## 0.1 One-minute pitch

**Say:**

> This is a **local demo** of TradingView Advanced Charts talking to a CTFX-style backend. Product specs for this slice are **design docs 120–139**: datafeed APIs, chart layouts, indicator templates, and chart templates. We did **not** connect Peach production SSO or a live liquidity provider. Login, history, and live prices are **mocks shaped like Peach** so the widget loads and you can review API contracts.
>
> Four processes: **Postgres + Redis** in Docker, **Java** REST and market ingest on 8080, **Python** WebSocket on 8081 that **only relays Redis**, **Vite** UI on 5173. The browser never talks to 8080/8081 directly — Vite proxies `/api` and `/ws`.

**Show:** open `http://127.0.0.1:5173` (login overlay). Do not log in yet.

## 0.2 What we built vs what Peach production is

| In this repo | Not in this repo |
|--------------|------------------|
| UDF paths `/api/config`, `/history`, `/symbols`, … | Peach SSO (S-01 real token check) |
| Layouts/templates REST 127–139 | Widget Save/Load → Postgres (`ServerSaveLoadAdapter`) |
| Local JWT + refresh cookie | Production bar writer / LP feed |
| Java **TickIngestWorker** mock ticks → Redis | Python inventing prices (removed on purpose) |

**Say:** Extra pieces (login UI, `GET /curpairs`, Python WS, Swagger) exist so the chart runs end-to-end. They are **not** missing 120–139 requirements.

## 0.3 Tech stack (same table as [`structure.md`](structure.md) §2)

**Say:** Nothing here is Peach production. Three apps plus Docker, chosen to match CTFX Java and to keep the widget on UDF URLs.

| Piece | Choice | Why (say this) |
|-------|--------|----------------|
| Backend | **Java 21**, **Spring Boot 4.0.7**, Maven Wrapper | Team CTFX. Boot 4 uses `spring-boot-starter-webmvc` (not old `starter-web`). Tests: `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`. |
| HTTP | Spring MVC `@RestController` | Datafeed keeps **UDF names** (`/config`, `/history`, …) because TradingView calls those URLs. Layouts/templates are resource REST. |
| Persistence | **Spring Data JPA** + **Flyway** | `ddl-auto: none`. Schema only from `V*.sql`. Masters via JPA; `t_chart_*` via **JdbcTemplate** (table name from enum, not the request). |
| Database | **PostgreSQL 16** (Docker) | Tests: **H2** `MODE=PostgreSQL`. |
| Cache | **Redis 7** (Docker) | Doc 121 `cache_set_*`, quote bus, refresh tokens. No Redis volume. |
| Live quotes | **Python 3.10+**, `websockets` + `redis` | Gateway `:8081`. Relays Java ticks **and** forming bars; **no local price or OHLC formula**. |
| Auth | **Spring Security** + **JJWT 0.12.6** + **BCrypt** | S-01 **stand-in**, not Peach SSO. |
| API docs | **springdoc-openapi 3.0.2** | Matches Boot 4. |
| Frontend | **Vite 7** + **TypeScript 5** | **No React.** Vendored Advanced Charts under `frontend/charting_library/`. |
| Compose | `docker-compose.yml` | Postgres volume `chart-pgdata`; Redis rebuilt on Java boot. |

Host needs Java 21+, Node 18+, Python 3.10+, Docker. Maven is the wrapper (`backend/mvnw.cmd`), not a global install.

**Show:** open `backend/pom.xml` (Boot 4.0.7, Java 21) and `frontend/package.json` (no React).

## 0.4 Repository tree (open these folders)

**Say:** Specs live at the repo root. Java is `backend/`. The widget is `frontend/`. The WS gateway is `ws-python/`.

```
Task/
  README.md                 how to run
  present.md                this script
  structure.md              onboarding (same facts, shorter)
  checklist.md              Done / Open vs specs
  test.md                   Postman / DBeaver / Maven per API
  docker-compose.yml        postgres :5432 + redis :6379
  System_Overview_Design/   English docs 120–139
  02_概要設計書/              Japanese originals
  .cursor/rules/            CTFX Java / FE / git
  backend/
    pom.xml                 Boot 4.0.7, Java 21, JJWT, Flyway, springdoc
    mvnw.cmd
    src/main/resources/
      application.yml       port, JWT, Redis, app.tradingview, tick-ms
      db/migration/V1–V9    schema + seeds
      messages*.properties  error text EN/JA
    src/main/java/com/task/chart/   ← class map in backend/.../README.md
  frontend/
    vite.config.ts          proxies /api → 8080, /ws → 8081
    index.html
    src/                    our TS (datafeed, login, quotes)
    charting_library/       TradingView (do not edit)
  ws-python/
    server.py               Redis subscriber → WS (quotes + forming bars)
    market.py               pair catalog + BID/ASK/MID column pick
    tests/
```

**If they ask “where is the Java package tree?”** open [`backend/src/main/java/README.md`](backend/src/main/java/README.md) (full `com.task.chart` file list + what each class does).

## 0.5 Glossary (say these words once)

| Term | Meaning here |
|------|----------------|
| **UDF** | TradingView Universal Data Feed JSON (`/config`, `/history`, …). Paths keep those names because the library calls them. |
| **S-01** | Peach login-status check. We use a **local JWT stand-in**, not Peach SSO. |
| **`customer_no`** | Tenant id in the JWT. `demo` → 1, `demo2` → 2. Layouts/templates are scoped to it. |
| **Warehouse** | Postgres `t_chart_*` (13 tables, doc 121). |
| **`cache_set_*`** | Redis sorted sets; history reads here first. |
| **SSOT** | Single source of truth for **live** prices **and** live candles: Java ingest only. |
| **Quote bus** | `SET peach:quote:{cd}` + `PUBLISH peach:quotes` (header ticks). |
| **Forming-bar bus** | `SET peach:forming:{resolution}:{CD}` + `PUBLISH peach:bars` (chart candles). |

## 0.6 Runtime picture (draw this)

```
Browser  :5173  Vite
   /api/**  and /curpairs  →  Java :8080
   /ws/**                  →  Python :8081

Java boot:  MockBarGenerator  →  t_chart_* + Redis cache_set_*
            (short fake history; wipe-on-restart)

Java live:  TickIngestWorker ~333ms
              DemoTickEngine: BID random walk, ASK = BID + spread, MID = (BID+ASK)/2
              → upsert current open bar (Postgres + cache_set_*)
              → SET peach:forming:* + PUBLISH peach:bars     ← same OHLC as history last bar
              → SET peach:quote:*   + PUBLISH peach:quotes   ← header ticks

Python:     SCAN peach:quote:* and peach:forming:*
            SUBSCRIBE peach:quotes → /ws/fx-quotes (header)
            SUBSCRIBE peach:bars   → /ws/stream    (relay Java candle; do not rebuild OHLC)

Docker:     Postgres :5432    Redis :6379
```

**Say (mock pipeline, for the mentor):**

> Two mock writers, both in Java. **Boot:** `MockBarGenerator` fills a short history so `/api/history` has candles on first load. **Runtime:** `DemoTickEngine` is the fake LP. `TickIngestWorker` is the only place that turns a tick into an OHLC bar (`openFromTick` / `applyTick`). Python is a dumb WebSocket: it forwards ticks to the header and forwards **that already-built bar** to TradingView. If the first live candle did not match the last history candle, we used to rebuild OHLC in Python — that is gone.
>
> A real Peach feed later replaces only `DemoTickEngine`. Redis keys, history API, and Python stay.

**Show (SSOT proof, 30 seconds):** with Java running:

```powershell
docker compose exec redis redis-cli SUBSCRIBE peach:quotes
docker compose exec redis redis-cli SUBSCRIBE peach:bars
docker compose exec redis redis-cli GET peach:forming:1D:USDJPY
```

Ticks and forming bars appear. **Stop Java** → both stop. Python can stay up; it does not invent prices or candles.

## 0.7 Start order (if the room is cold)

1. Repo root: `docker compose up -d`
2. `backend`: `.\mvnw.cmd spring-boot:run` until `Started ChartBackendApplication`
3. `ws-python`: `python server.py` (optional until you show live quotes)
4. `frontend`: `npm start` → `http://127.0.0.1:5173`

Health (no token): `GET http://127.0.0.1:8080/api/health` → `{"status":"ok","service":"chart-backend"}`.

Swagger: `http://127.0.0.1:8080/swagger-ui.html` — **Authorize** is access JWT only; it cannot demo refresh cookies (see Part A).

---

# Part A — Shared across almost every API (do this before 120)

These are **not** a numbered design doc, but docs 120–139 all say “confirm token” and share error shapes. Present them first so later docs stay short.

## A.1 Layering (Java)

**Say:** CTFX style: controller is HTTP only; service has rules; JPA for masters; JDBC+Redis for bars. Entities never go on the wire.

**Code:** [`backend/src/main/java/README.md`](backend/src/main/java/README.md) — package table + tree.

**Show:** open `ChartDataController` then `ChartDataServiceImpl` — controller has no SQL.

## A.2 Token authentication (S-01 stand-in)

**Spec (every 120–139 doc):** confirm token validity (Peach S-01).

**What we did:** local dual token.

| Token | Lifetime | Stored | Sent as |
|-------|----------|--------|---------|
| Access JWT | 1 hour | `sessionStorage` `chart_access_token` | `Authorization: Bearer …` |
| Refresh | 1 day | HttpOnly cookie `chart_refresh_token` + Redis `peach:auth:refresh:{uuid}` | Cookie on `/api/auth/refresh` and `/logout` |

**Code:** `AuthController` → `AuthServiceImpl` → `JwtService` + `RefreshTokenStore` + `JwtAuthenticationFilter` + `CustomerContext`.

**Public (no Bearer):** `GET /api/health`, `POST /api/auth/login`, `/refresh`, `/logout`, Swagger, `OPTIONS`. Everything else under `/api/**` and `GET /curpairs` needs a valid access JWT.

**Say:** Logout **revokes refresh** in Redis. The access JWT stays valid until its 1h expiry (stateless JWT; no denylist). That is an accepted demo gap.

**Show (browser):** log in `demo` / `demo` → DevTools Application: cookie + sessionStorage.

**Show (curl):** [`structure.md`](structure.md) §6.1 B.

**Test:** `AuthLoginTest` (`cd backend`; Redis up; `.\mvnw.cmd test -Dtest=AuthLoginTest`).

**Honest:** Not Peach SSO. Seeded users: `demo`/`demo` customer 1, `demo2`/`demo2` customer 2. Flyway V7 `m_app_user`, BCrypt, `AppUserSeedRunner`.

## A.3 Errors and language

**Say:** Body is always `{ "errorCode", "message" }`. `Accept-Language: ja` switches `message`. History **unknown symbol** is UDF `{ "s": "error" }`, not 404.

| HTTP | errorCode | When |
|------|-----------|------|
| 422 | `CODE:30020` | Validation |
| 404 | `CODE:30404` | Missing or other customer’s row |
| 401 | `E_UNAUTHORIZED` | Bad/missing access JWT or refresh cookie |
| 401 | `E_BAD_CREDENTIALS` | Login failed |
| 500 | `E_SERVER` | Unexpected |

**Code:** `GlobalExceptionHandler`, `ErrorCodes`, `messages.properties` / `messages_ja.properties`.

**Datetime:** Peach “update/system datetime” → `{ "t": unix seconds }`. Upsert uses row `updated_at`; delete uses clock now.

## A.4 Database (Flyway V1–V9)

**Say:** Hibernate `ddl-auto: none`. Schema is **only** Flyway. Do not edit an applied `V*` file.

| V | Tables | Used by |
|---|--------|---------|
| 1 | `m_ccypairs` | 123, 124, warehouse keys, `/curpairs` |
| 2 | `m_season` | 123 session |
| 3 | `m_tv_mark` | 125 |
| 4 | `m_tv_timescale_mark` | 126 |
| 5 | `m_tv_chart_layout` | 127–131 |
| 6 | `m_tv_indicator_template` | 132–135 |
| 7 | `m_app_user` | login |
| 8 | 13× `t_chart_*` | 121 |
| 9 | `m_tv_chart_templates` | 136–139 |

**Show:** DBeaver `127.0.0.1:5432` db `chart` / user `chart` / password `chart`.

**Test:** `FlywayMigrationTest`.

**Honest:** Every Java **boot** wipes and reseeds warehouse rows for demo pairs. Redis has no Docker volume — empty after compose down; Java reseeds it.

---

# Part B — Datafeed 120–126 (what the widget calls)

**Say:** TradingView `frontend/src/datafeed/datafeed.ts` is the **client**. All HTTP for 120–126 lands on **one** Java class: `ChartDataController` (`@RequestMapping("/api")`). Rules are `ChartDataServiceImpl`. Bars also touch `cache/*`. Live ticks are **not** in the datafeed controller — they are ingest + Python WS.

| Doc | HTTP method on `ChartDataController` | Service method | FE |
|-----|--------------------------------------|----------------|-----|
| 120 | `config()` `GET /config` | `config()` | `datafeed.ts` `onReady` |
| 121 | `history(...)` `GET /history` | `history(...)` | `getBars` |
| 122 | `time()` `GET /time` | `serverTimeSeconds()` | `getServerTime` |
| 123 | `symbols(...)` `GET /symbols` | `resolve(...)` | `resolveSymbol` |
| 124 | `search(...)` `GET /search` | `search(...)` | `searchSymbols` |
| 125 | `marks(...)` `GET /marks` | `marks(...)` | `getMarks` |
| 126 | `timescaleMarks(...)` `GET /timescale_marks` | `timescaleMarks(...)` | `getTimescaleMarks` |

---

## 120 — Get datafeed configuration

**Path:** `GET /api/config`  
**Table:** none

**Say:** First call the library makes. Flags tell it whether search, marks, time, and which resolutions exist. Values come from `app.tradingview` in `application.yml`, not from a table.

**Spec:** `supports_search/marks/timescale_marks/time`, exchanges CTFX, types FOREX, resolutions `1S,1,5,15,30,60,120,240,480,1D,1W,1M`. Token check.

**Code:** `ChartDataController.config()` → `ChartDataServiceImpl.config()` → `DatafeedConfigResponse` from `app.tradingview`. DTO: `dto/response/DatafeedConfigResponse.java`.

**Show:** Swagger after Authorize, or Network tab after login. Expect flags `true`, 12 resolutions, CTFX/FOREX.

**Test:** `SystemOverviewDesign120Test`.

**Honest extra:** we send `supports_group_request: false` so the library uses `/search` (required; doc omits the field).

---

## 121 — Get bars (warehouse + Redis + ingest SSOT)

**Path:** `GET /api/history`  
**Tables:** `t_chart_*` (V8)  
**Cache:** Redis `peach:cache_set_*:{CD}`

This is the **longest** datafeed section. Split it: (1) what the spec asks, (2) how history is read, (3) how data is written.

### B.121.1 What the spec asks

**Say:** Return OHLC for a pair, chart type, time range, and **BID / ASK / MID**. Peach uses 13 tables (1 second through month). Cache and writer must not race. Response is columnar `t[], o[], h[], l[], c[]` plus status `s`.

**Spec details:** `bid_ask` required; `symbol` length 6; `from`/`to` paired; `to >= from`; MID averages BID and ASK columns.

### B.121.2 How we read (no stitch)

**Say:** History **only reads Redis** (warm from Postgres on miss). We **removed** stitching a fake “current bar” from a second in-process walker. The last candle is whatever ingest last upserted.

**Code:** `ChartDataController.history(...)` → `ChartDataServiceImpl.history(...)` → `ChartCacheStore.query(...)` → `HistoryResponse.ok(bars)`. Dual JSON: Peach columns **and** widget `bars[]`. DTO: `HistoryResponse.java`, `BarDto.java`.

**Show:** after login, change interval on the chart; Network `GET /api/history?...&bid_ask=MID` → 200, `s=ok`, both `t[]` and `bars[]`.

**Test:** `SystemOverviewDesign121Test`.

**Honest extras:** widget needs `bars[]`; Peach columns are extra. We accept `USD/JPY` then normalize to `USDJPY` so the library does not 422 (spec says length 6).

### B.121.3 How we write — boot seed vs live ingest

**Say two sentences:**

> **Boot:** `ChartCacheWriter` (`@Order(100)`) uses `MockBarGenerator` to fill a **short** history into all 13 tables and Redis. Every restart **replaces** those rows. That is not years of ticks.
>
> **Runtime:** `TickIngestWorker` (`@Order(200)`, every `app.chart-cache.tick-ms` = 333) is the **only live price and candle engine**. It steps a mock BID walk (`DemoTickEngine`: ASK = BID + spread, MID = (BID+ASK)/2), upserts the **current open bar** on every namespace, publishes that bar on `peach:bars`, then publishes the tick on `peach:quotes`.

**Code:**

| Class | Role |
|-------|------|
| `CacheNamespace` | TV resolution → table + Redis prefix |
| `ChartCacheWriter` | Boot seed only |
| `DemoTickEngine` | Mock LP |
| `QuoteBus` | `peach:quote:*` + `peach:quotes`; `peach:forming:*` + `peach:bars` |
| `TickIngestWorker` | `tick()` → upsert + forming bus + quote bus |
| `FormingBarMessage` | WS/history handoff payload (`time` in ms) |
| `ChartBarRepository` | JDBC upsert |

**Show:**

```powershell
docker compose exec redis redis-cli GET peach:quote:1
docker compose exec redis redis-cli GET peach:forming:1D:USDJPY
docker compose exec redis redis-cli SUBSCRIBE peach:quotes
docker compose exec redis redis-cli SUBSCRIBE peach:bars
```

**Test:** `TickIngestWorkerTest` (Redis required). Scheduling is **off** in tests; the test calls `tick()` directly.

**Honest:** Not a Peach LP. Wipe-on-boot still applies to historical warehouse rows. Python does **not** write `t_chart_*` and does **not** rebuild the forming candle.

Namespace cheat sheet (say “thirteen types, one enum”):

| TV | Table | Redis prefix |
|----|-------|----------------|
| 1S | `t_chart_1` | `peach:cache_set_1s:{CD}` |
| 1 | `t_chart_60` | `peach:cache_set_1m:{CD}` |
| … | … | … |
| 1D / 1W / 1M | `t_chart_day` / `_week` / `_month` | `cache_set_day` etc. |

---

## 122 — Get server time

**Path:** `GET /api/time`  
**Table:** none

**Say:** Unix seconds, no milliseconds. The library uses it to align the time scale.

**Spec:** field `t`.

**Code:** `ChartDataController.time()` → `ChartDataServiceImpl.serverTimeSeconds()` → `ServerTimeResponse` `{ "t", "serverTime" }` (same unix).

**Show:** Swagger or Network; `t` within a few seconds of now.

**Test:** `SystemOverviewDesign122Test`.

**Honest extra:** `serverTime` is for the library; spec only names `t`. Doc does not mention auth; we still gate `/api/**`.

---

## 123 — Get symbol information

**Path:** `GET /api/symbols?symbol=`  
**Tables:** `m_ccypairs`, `m_season`

**Say:** One pair’s TradingView symbol info: name, pricescale, session, timezone. Session comes from `m_season` vs now (summer vs winter hours).

**Spec:** token; symbol length 6; active `is_deleted=0`; map CD, Japanese name, `rate_unit` → `pricescale`.

**Code:** `ChartDataController.symbols(...)` → `ChartDataServiceImpl.resolve(...)` → `SymbolCatalog` + `m_ccypairs` / `m_season` → `SymbolInfoDto`.

**Show:** `symbol=USDJPY` and `symbol=USD%2FJPY` both 200, `name=USDJPY`, `ticker=USD/JPY`, `pricescale=1000`. `ETH` → 422. `ETHUSD` → 404.

**Test:** `SystemOverviewDesign123Test`.

**Honest extras:** library fields (`ticker`, `format`, …). Slash form accepted after normalize.

---

## 124 — Get symbol list (search)

**Path:** `GET /api/search`  
**Table:** `m_ccypairs`

**Say:** Type-ahead for the symbol search box. Partial match on CD or Japanese name, `is_deleted=0`, sort `priority` ASC.

**Spec:** `query` optional max 10; `limit` 1–100.

**Code:** `ChartDataController.search(...)` → `ChartDataServiceImpl.search(...)` → `CcypairRepository` → `SearchSymbolDto[]`.

**Show:** widget search `EUR` → EURJPY / EURUSD. Empty query still returns pairs (limit).

**Test:** `SystemOverviewDesign124Test`.

**Honest extras:** ticker/filter fields the widget wants.

---

## 125 — Marks (pins on the chart)

**Path:** `GET /api/marks`  
**Table:** `m_tv_mark` (V3)

**Say:** Event pins on candles. Query `symbol`, `resolution`, `from`, `to`. Seeded three USDJPY 1D marks in a **fixed unix window**. If Postman `from`/`to` miss that window, the list is empty — that is expected.

**Window:** `1787011200`–`1787270400` (constant `MarkSeedWindow`).

**Code:** `ChartDataController.marks(...)` → `ChartDataServiceImpl.marks(...)` → `TvMarkRepository` → `MarkDto`. Entity: `TvMark.java`.

**Show:** widget on USD/JPY 1D in that date range → pins at **top** of candles. Or Postman with the seed window.

**Test:** `SystemOverviewDesign125Test`.

**Honest:** Table has **no** `customer_no`; marks are global demo seeds, not per-token. Extra font fields for the library. `supports_marks` on 120 is true.

---

## 126 — Timescale marks (labels on the time axis)

**Path:** `GET /api/timescale_marks`  
**Table:** `m_tv_timescale_mark` (V4)

**Say:** Same pattern as 125, but labels on the **time axis**, not on the candle.

**Code:** `ChartDataController.timescaleMarks(...)` → `ChartDataServiceImpl.timescaleMarks(...)` → `TvTimescaleMarkRepository` → `TimescaleMarkDto`.

**Show:** same seed window; labels under the chart.

**Test:** `SystemOverviewDesign126Test`.

**Honest:** Tooltip may be an array for the library. Same global-seed / no customer column as 125.

---

# Part C — Chart layouts 127–131

**Say:** Saved **chart layouts** (drawings, symbol, interval) per customer. Table `m_tv_chart_layout` (V5). REST and widget Save/Load are **done** (`ServerSaveLoadAdapter`).

**Code:** `ChartLayoutController` → `ChartLayoutServiceImpl` → `TvChartLayout` / `TvChartLayoutRepository`.  
**FE:** `frontend/src/save-load-adapter.ts` → `/api/layouts`.

| Doc | HTTP | Behavior |
|-----|------|----------|
| **127** | `POST /api/layouts` | Insert; we return **201** `{ "id" }` (id extra vs a bare datetime) |
| **128** | `PUT /api/layouts/{id}` | Update; other customer → 404 |
| **129** | `GET /api/layouts/{id}` | Full DTO |
| **130** | `GET /api/layouts` | List for token customer |
| **131** | `DELETE /api/layouts/{id}` | Hard delete; `{ "t": now }` |

**Show:** Swagger with Bearer: POST a layout as `demo`, GET list, then login as `demo2` and GET the same id → 404 (tenant).

**Test:** `SystemOverviewDesign127Test` … `131Test`.

**Honest:** Drawings persist inside layout `content`. Drawing-tool templates have no Peach API.

---

# Part D — Indicator (study) templates 132–135

**Say:** TradingView **study** templates (indicator presets). Unique `(customer_no, name)`, name max 64. Table `m_tv_indicator_template` (V6). REST + widget (`study_templates` + adapter) **done**.

**Code:** `IndicatorTemplateController` → `IndicatorTemplateServiceImpl` → `TvIndicatorTemplate`.

| Doc | HTTP | Behavior |
|-----|------|----------|
| **132** | `GET /api/indicator-templates` | Names only, **ASC**, no `content` |
| **133** | `POST /api/indicator-templates` | Upsert: insert all columns; update **content only**; `{ "t": updated_at }` |
| **134** | `GET /api/indicator-templates/{name}` | `{ name, content }`; name > 64 → 422; other customer → 404 |
| **135** | `DELETE /api/indicator-templates/{name}` | Hard delete; `{ "t": now }`; other customer 404 and **row kept** |

**Show:** Swagger POST then GET by name. Spaces in name: URL-encode.

**Test:** `SystemOverviewDesign132Test` … `135Test`.

---

# Part E — Chart templates 136–139

**Say:** These are **chart** templates (theme/layout preset), **not** 127 layouts and **not** 132 study templates. Table name in the MD is **plural** `m_tv_chart_templates` (V9). REST noun `/api/chart-templates`. Same upsert/list/get/delete shape as 132–135.

**Code:** `ChartTemplateController` → `ChartTemplateServiceImpl` → `TvChartTemplate`.

| Doc | HTTP | Behavior |
|-----|------|----------|
| **136** | `GET /api/chart-templates` | Names for token customer, ASC, no content |
| **137** | `POST /api/chart-templates` | Body `name` + `content`; upsert; `{ "t": updated_at }` |
| **138** | `GET /api/chart-templates/{name}` | `{ name, content }` |
| **139** | `DELETE /api/chart-templates/{name}` | `{ "t": Instant.now() }` |

**Show:** Chart settings (gear) → **Template** — save/apply names from Postgres. Swagger fallback; encode spaces (`My%20Dark`).

**Test:** `SystemOverviewDesign136Test` … `139Test`.

**FE:** `ServerSaveLoadAdapter` + `chart_template_storage` in `main.ts` (`JSON.stringify` / parse).

---

# Part F — Extra surfaces (not in 120–139 — still present them)

Do **not** skip these. Mentors will see them in the UI and Swagger.

## F.1 `GET /curpairs`

**Say:** Catalog for the **header quote** stream. Same master as 123/124 (`m_ccypairs`). `curpairCd` = `priority` (JSON **number**). JWT required.

**Code:** `CurrencyPairController` → `CurrencyPairServiceImpl`.

**Show:** `GET http://127.0.0.1:8080/curpairs` with Bearer → five rows USDJPY … AUDUSD.

**Test:** `CurrencyPairControllerTest`.

**Honest:** Not in the design-doc folder. WS `curpairCd` is a **string** (`"1"`) — easy mix-up; call it out.

## F.2 Python WebSocket gateway

**Say:** Python is **not** a second market. It snapshots Redis, subscribes, and forwards. Ticks go to the header. Forming bars go to the chart. It does **not** fold ticks into OHLC (that used to cause a freeze/plunge when the first live candle replaced Java’s last history bar).

| Path | Who consumes it | Redis |
|------|-----------------|-------|
| `/ws/fx-quotes` | Header BID/ASK/MID (`fxQuotesSocket.ts`) | `peach:quote:*` / `peach:quotes` |
| `/ws/stream` | Widget `subscribeBars` (`streaming.ts`) | `peach:forming:*` / `peach:bars` — same candle as `GET /api/history` last bar |

**Code:** `ws-python/server.py` (`RedisMarketSource`, `Hub.run_ingest`, `BarRelay`). `market.py`: five-pair catalog + `widget_bar()` (BID/ASK/MID column pick only). Catalog must match `m_ccypairs.priority`.

**Show:** Java up → header numbers move and the last candle updates without jumping to a new open. Stop Java → header **and** candle **freeze** (Python still running).

**Test:** `cd ws-python` → `pytest` (`test_stream_relays_java_forming_bar_not_tick_ohlc` proves a tick alone does not change the candle; `test_fx_quotes_idle_without_ingest` proves no self-generation).

**Honest:** WS is public (no JWT). Catalog not loaded from Java. Out of scope: moving WS into Java.

## F.3 Frontend (what the mentor sees)

**Say:** Not React. Vanilla TypeScript + vendored TradingView under `frontend/charting_library/` (do not edit the library).

| File | Role |
|------|------|
| `main.ts` | Login or silent refresh → widget `USD/JPY` `1D` |
| `auth.ts` / `api.ts` | Bearer + cookie; 401 → refresh once |
| `datafeed/datafeed.ts` | Docs 120–126 HTTP |
| `datafeed/streaming.ts` | Live bars from `/ws/stream` |
| `fx/quoteToolbar.ts` | Maps WS `curpairCd` via `/curpairs` |
| `save-load-adapter.ts` | **Done** — 127–139 REST (Postgres) |
| `toolbar.ts` / `theme.ts` | Logout, light/dark |

**Show after login:** candles, interval switcher, BID/ASK/MID dropdown (reloads history for that side), live quote, theme, Logout.

Two encodings (say this once): chart uses `USDJPY`; quotes use `curpairCd` `"1"`.

## F.4 Swagger / OpenAPI

**URL:** `http://127.0.0.1:8080/swagger-ui.html`  
**Code:** `OpenApiConfig`, springdoc 3.0.2.

**Show:** tags Auth, Datafeed 120–126, layouts, indicator templates, chart templates, currency pairs. Login → copy `accessToken` → Authorize (token only, no `Bearer ` prefix).

**Test:** `OpenApiDocsTest`.

**Honest:** Swagger does not exercise refresh cookies. Full Try-it-out script for **120–139** (expected JSON, MD gaps, DBeaver) is **Part K** at the bottom of this file.

## F.5 Health

`GET /api/health` — public liveness. Not a design doc.

---

# Part G — How we prove it (tests)

**Say:** Java tests need **Redis** (cache writer + refresh tokens + ingest snapshot). Tests use H2 `MODE=PostgreSQL`; scheduling **off**.

```powershell
cd backend
.\mvnw.cmd test
```

Per-doc: `SystemOverviewDesign120Test` … `139Test`. Also `AuthLoginTest`, `TickIngestWorkerTest`, `CurrencyPairControllerTest`, `FlywayMigrationTest`.

```powershell
cd ws-python
python -m pytest
```

```powershell
cd frontend
npm run typecheck
```

Postman/DBeaver per API: [`test.md`](test.md). Checkpoint table: [`checklist.md`](checklist.md).

---

# Part H — What is still Open (say this at the end)

1. **Peach S-01 SSO** — local JWT + refresh cookie only.
2. **Real LP / Peach bar pipeline** — `DemoTickEngine` + wipe-on-boot seed.
3. **Drawing templates** — no Peach table; layout drawings persist in `content`.
4. **Python catalog** — hardcoded five pairs; Java `/curpairs` reads the DB.
5. **WS auth** — socket is public.
6. **History dual JSON** — `bars[]` for the library; Peach columns extra.
7. **Access JWT denylist** — logout does not kill the 1h access token.
8. **Strict length-6 symbols** — slash form accepted for the widget.

When those change, update `structure.md`, `present.md`, `test.md`, and `checklist.md` together.

---

# Part I — Suggested live demo sequence (15–20 minutes)

| Min | Do this | Proves |
|-----|---------|--------|
| 0–3 | Pitch + **tech stack** (0.3) + **repo tree** (0.4) + topology (0.6) | Scope + “why Java/Redis/Python” |
| 2–4 | Login `demo`/`demo`; show cookie + sessionStorage | Auth stand-in |
| 4–6 | Chart loads; Network `/api/config`, `/symbols`, `/history` | 120, 123, 121 read |
| 6–8 | Redis `SUBSCRIBE peach:quotes` **and** `peach:bars`; stop Java briefly | SSOT / Python gateway |
| 8–10 | Restart Java; BID/ASK/MID switch; live header | Quotes + history side |
| 10–12 | Swagger 130 list / 132 list (empty or seeded) | 127–139 exist |
| 12–14 | Say layouts **not** in widget; open `save-load-adapter.ts` | Honest gap |
| 14–16 | Logout; refresh 401 | Refresh revoke |
| 16–18 | `.\mvnw.cmd test -Dtest=TickIngestWorkerTest,AuthLoginTest` if time | Automated proof |

---

# Part J — If they ask “where is that in the code?”

Hand them this section **and** [`backend/src/main/java/README.md`](backend/src/main/java/README.md) (full class tree). Open the **controller method** first, then the **service method**.

## J.1 “Where is the datafeed?” (docs 120–126)

**One sentence:** HTTP in `ChartDataController`; rules in `ChartDataServiceImpl`; the widget client in `frontend/src/datafeed/datafeed.ts`. Live candles are **not** in that controller — see J.3.

| They say | Open this file | Then this method | Path |
|----------|----------------|------------------|------|
| “Where is 120?” | `controller/ChartDataController.java` | `config()` | `GET /api/config` |
| “Where is 121 / history / bars?” | same + `cache/ChartCacheStore.java` | `history(...)` then `query(...)` | `GET /api/history` |
| “Where is 122 / time?” | `ChartDataController` | `time()` | `GET /api/time` |
| “Where is 123 / symbol info?” | same | `symbols(...)` → service `resolve(...)` | `GET /api/symbols` |
| “Where is 124 / search?” | same | `search(...)` | `GET /api/search` |
| “Where is 125 / marks?” | same | `marks(...)` | `GET /api/marks` |
| “Where is 126 / timescale?” | same | `timescaleMarks(...)` | `GET /api/timescale_marks` |
| “Where does the widget call it?” | `frontend/src/datafeed/datafeed.ts` | `onReady` / `getBars` / `resolveSymbol` / … | proxied `/api/...` |
| “Health?” | `ChartDataController` | `health()` | `GET /api/health` (public) |

Config flags for 120 are **yml**, not a table: `backend/src/main/resources/application.yml` → `app.tradingview` → `AppProperties` → `ChartDataServiceImpl.config()`.

## J.2 Layouts and templates (127–139)

| They say | Controller | Service impl | Table / entity |
|----------|------------|--------------|----------------|
| 127–131 layouts | `ChartLayoutController` `/api/layouts` | `ChartLayoutServiceImpl` | `m_tv_chart_layout` / `TvChartLayout` |
| 132–135 studies | `IndicatorTemplateController` `/api/indicator-templates` | `IndicatorTemplateServiceImpl` | `m_tv_indicator_template` |
| 136–139 chart templates | `ChartTemplateController` `/api/chart-templates` | `ChartTemplateServiceImpl` | `m_tv_chart_templates` |
| “Does the chart use this?” | **Yes** — `ServerSaveLoadAdapter` in `main.ts` |

## J.3 Live prices (not a 120–139 doc, but they will ask)

| They say | File | Method / role |
|----------|------|----------------|
| “Who invents BID/ASK?” | `cache/DemoTickEngine.java` | `stepAll()` / `SimulatedQuote.step()` |
| “Who is the SSOT / ingest?” | `cache/TickIngestWorker.java` | `tick()` (~333ms), `upsertOpenBar(...)` |
| “Who writes Redis quotes?” | `cache/QuoteBus.java` | `publish(...)` → `SET peach:quote:*` + `PUBLISH peach:quotes` |
| “Who writes the live candle?” | `cache/QuoteBus.java` | `publishForming(...)` → `SET peach:forming:*` + `PUBLISH peach:bars` |
| “Who seeds old candles?” | `cache/ChartCacheWriter.java` + `MockBarGeneratorImpl` | `seedAll()` on boot only |
| “Who reads bars for 121?” | `cache/ChartCacheStore.java` | `query(...)` |
| “Python prices / OHLC?” | **There are none.** | `ws-python/server.py` `RedisMarketSource.listen()` relays quotes **and** bars |
| “Header quote?” | `frontend/src/fx/fxQuotesSocket.ts` | `/ws/fx-quotes` |
| “Live candle on the chart?” | `frontend/src/datafeed/streaming.ts` | `/ws/stream` (Java forming bar) |

## J.4 Auth, errors, extras

| They say | File |
|----------|------|
| Login / refresh / logout | `AuthController` → `AuthServiceImpl` → `JwtService`, `RefreshTokenStore` |
| Bearer filter | `JwtAuthenticationFilter`, `SecurityConfig` |
| Tenant | `CustomerContext` (`customer_no`) |
| `/curpairs` | `CurrencyPairController` → `CurrencyPairServiceImpl` |
| Error JSON | `GlobalExceptionHandler` + `ErrorCodes` |
| CORS | `WebConfig` |
| Swagger tags | `OpenApiConfig` |
| Pair catalog in memory | `SymbolCatalogImpl` (from `m_ccypairs`) |
| TV resolution → ms | `util/ResolutionMapper.java` |
| BID/ASK/MID enum | `constants/PriceComponent.java` |

## J.5 Frontend files (if they click the chart)

| File | Why it exists |
|------|----------------|
| `frontend/src/main.ts` | Boot: login or silent refresh, then create the TradingView widget |
| `frontend/src/datafeed/datafeed.ts` | **Datafeed adapter** — all 120–126 HTTP |
| `frontend/src/datafeed/streaming.ts` | `subscribeBars` → Python relays Java forming bars |
| `frontend/src/api.ts` | `fetch` + Bearer + one silent refresh on 401 |
| `frontend/src/auth.ts` | login/refresh/logout, sessionStorage |
| `frontend/src/login.ts` | Overlay UI |
| `frontend/src/fx/currencyPairs.ts` | `GET /curpairs` |
| `frontend/src/fx/quoteStore.ts` | In-memory quotes by `curpairCd` |
| `frontend/src/fx/quoteToolbar.ts` | Header BID/ASK/MID |
| `frontend/src/save-load-adapter.ts` | **Not** 127–139 REST yet |
| `frontend/vite.config.ts` | Proxies `/api` and `/ws` |

## J.6 One-line answers

| Question | Answer |
|----------|--------|
| Who creates live prices? | `TickIngestWorker` + `DemoTickEngine` |
| Who writes Redis quotes? | `QuoteBus.publish` |
| Who writes the live candle? | `QuoteBus.publishForming` (same OHLC as history last bar) |
| Who reads history? | `ChartCacheStore` via `ChartDataServiceImpl.history` (no stitch) |
| Who seeds old candles? | `ChartCacheWriter` + `MockBarGenerator` |
| Who is Python? | `ws-python/server.py` relay (`peach:quotes` + `peach:bars`) |
| Who is tenant? | JWT `customer_no` → `CustomerContext` |
| Who maps 120 flags? | `app.tradingview` → `ChartDataServiceImpl.config()` |

---

# Part K — Swagger lab: test docs 120–139

**What this section is:** how a mentor (or you) proves **120–139** from the browser with **Try it out**, what JSON to expect, whether that JSON **matches the design MD**, and which **DBeaver** query to run when a table is involved.

**Same facts as** [`test.md`](test.md). Use this file when you are already in Swagger; use `test.md` when you want Postman copy-paste.

**Swagger cannot prove:** TradingView widget Save/Load UX (use the chart + Network; REST is in Try it out), Python `/ws/*`, or the HttpOnly refresh cookie (Authorize is access JWT only).

---

## K.0 Before Execute

### K.0.1 Java must be up

Docker Postgres + Redis, then:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Wait for `Started ChartBackendApplication`. Swagger is **on 8080**, not Vite 5173.

### K.0.2 Open Swagger

`http://127.0.0.1:8080/swagger-ui.html`

Tags (top to bottom): **Auth** → **Datafeed (120–126)** → **Chart layouts (127–131)** → **Indicator templates (132–135)** → **Chart templates (136–139)** → **Currency pairs** (extra, not a 120–139 doc).

### K.0.3 Get a token (do this first)

1. Tag **Auth** → `POST /api/auth/login` → Try it out.
2. Body:

```json
{ "username": "demo", "password": "demo" }
```

3. Execute. **Expect 200:**

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshExpiresIn": 86400
}
```

4. Copy **`accessToken` only** (not `Bearer `).

| User | Password | `customer_no` in JWT / DB |
|------|----------|---------------------------|
| `demo` | `demo` | `1` |
| `demo2` | `demo2` | `2` (use for tenant 404) |

**MD match:** login is **not** in 120–139. It is a **Stub** for Peach S-01.

### K.0.4 Authorize

Green **Authorize** (lock) at the top → paste the token → Authorize → Close.

Every 120–139 Execute now sends `Authorization: Bearer <token>`.

**Prove it:** Execute `GET /api/config` **before** Authorize → **401** `{ "errorCode": "E_UNAUTHORIZED", "message": "..." }`. After Authorize → **200**.

### K.0.5 Legend (MD vs this demo)

| Word | Meaning |
|------|---------|
| **Complete** | Required MD fields/status/table are implemented |
| **Extra** | Extra JSON fields or behaviour the widget/demo needs; MD table does not list them |
| **Partial** | Intentional difference from a strict MD line |
| **Open** | REST works in Swagger; the **chart UI** does not call it |
| **Stub** | Local JWT, not Peach S-01 |

**Global (every 120–139 call):** auth is **Stub**. Japanese error *copy* does **not** match the Peach envelopes (`パラメータが不正です。` vs app `リクエストが不正です。`, etc.). Codes **do** match: `CODE:30020`, `CODE:30404`, `E_UNAUTHORIZED`, `E_SERVER`. 500 body has **no** `"status": 500` field.

### K.0.6 DBeaver (when a table exists)

| Field | Value |
|-------|-------|
| Host | `127.0.0.1` |
| Port | `5432` |
| Database | `chart` |
| User | `chart` |
| Password | `chart` |

120 and 122 have **no** table.

---

## K.120 — `GET /api/config`

**Swagger:** Datafeed → **120 Get datafeed configuration**. No params.

**Execute.** **Expect 200:**

```json
{
  "supports_search": true,
  "supports_group_request": false,
  "supports_marks": true,
  "supports_timescale_marks": true,
  "supports_time": true,
  "supported_resolutions": ["1S", "1", "5", "15", "30", "60", "120", "240", "480", "1D", "1W", "1M"],
  "exchanges": [{ "value": "CTFX", "name": "CTFX", "desc": "CTFX" }],
  "symbols_types": [{ "name": "FOREX", "value": "FOREX" }]
}
```

Check: four `supports_*` (except group) are `true`; resolutions length **12**; no `10` in the list.

**MD match:** **Complete** on the required flags/exchanges/types/12 resolutions. **Extra:** `supports_group_request: false` (widget must use `/search`). **Stub:** JWT.

**DBeaver:** none (yml `app.tradingview`).

---

## K.121 — `GET /api/history`

**Swagger:** **121 Get bars**.

| Param | Value to paste |
|-------|----------------|
| `symbol` | `USDJPY` |
| `resolution` | `1D` |
| `from` | unix seconds ~20 days ago (e.g. now minus `1728000`) |
| `to` | unix seconds now |
| `bid_ask` | `MID` |
| `countBack` | leave empty or `300` (**Extra**) |

**Execute.** **Expect 200** `"s": "ok"`; arrays `t`, `o`, `h`, `l`, `c` same length; `t[]` is **seconds** (10 digits). Also **Extra** `bars[]` with `time` in **milliseconds**.

**No data in range:** set `from`/`to` far in the future empty window, or a Saturday-only window with no bars → `"s": "no_data"`, empty arrays, `nextTime` = prior bar unix seconds.

**Bad `bid_ask`:** omit it or `FOO` → **422** `CODE:30020`.

**Unknown symbol** `ETHUSD` → **200** `{ "s": "error", "errmsg": "unknown_symbol" }` (UDF, **not** 404).

**MD match:** **Complete** on BID/ASK/MID columns, paired `from`/`to`, 13 warehouse tables, `no_data`+`nextTime`, columnar arrays. **Extra:** `bars[]`, `countBack`, `noData`/`errmsg`. **Partial:** unknown symbol is UDF error, not REST 404; data is **mock** (`MockBarGenerator` + ingest), not Peach LP. Slash `USD/JPY` is accepted.

**DBeaver:**

```sql
SELECT COUNT(*) AS bar_count
FROM t_chart_day
WHERE curpair_cd = 'USDJPY';
```

Expect `bar_count > 0` after Java boot.

```sql
SELECT curpair_cd, chart_datetime, bid_close, ask_close, volume
FROM t_chart_day
WHERE curpair_cd = 'USDJPY'
ORDER BY chart_datetime DESC
LIMIT 5;
```

`chart_datetime` should match API `t[]` (seconds). MID close in JSON ≈ `(bid_close + ask_close) / 2`.

---

## K.122 — `GET /api/time`

**Swagger:** **122 Get server time**. No params.

**Execute.** **Expect 200:**

```json
{ "t": 172..., "serverTime": 172... }
```

`t === serverTime`; both unix **seconds** (less than `10000000000`); within a few seconds of wall clock.

**MD match:** **Complete** on `t` as seconds. **Extra:** `serverTime` (frontend reads this). Original MD does not require auth; app still needs JWT (**Stub**).

**DBeaver:** none.

---

## K.123 — `GET /api/symbols`

**Swagger:** **123 Get symbol information**. `symbol=USDJPY`.

**Execute.** **Expect 200:** `name=USDJPY`, `description=米ドル/円`, `pricescale=1000`, `timezone=Asia/Tokyo`, `exchange=CTFX`, `type=FOREX`, `minmov=1`, `has_intraday=true`, `has_seconds=true`, 12 `supported_resolutions`. Session is the **winter** string while seed `m_season.season_cd=2` covers 2020–2099: `0700-3100:2|0700-3100:345|0700-3040:6`.

Also **Extra** library fields: `ticker=USD/JPY`, `listed_exchange`, multipliers, `data_status`, `provider_symbol`.

`symbol=ETH` (too short) → **422**. `symbol=ETHUSD` (unknown) → **404** `CODE:30404`.

**MD match:** **Complete** on master lookup, `pricescale=10^rate_unit`, session from season, required UDF fields. **Extra:** ticker/slash and library extras. **Partial:** table name `m_season` / `start_at` (not `M_SEASON` / `start_date`).

**DBeaver:**

```sql
SELECT ccypair_cd, ccypair_jp, rate_unit, is_deleted
FROM m_ccypairs
WHERE ccypair_cd = 'USDJPY';
```

Expect `rate_unit=3`, `is_deleted=0`.

```sql
SELECT season_cd, start_at, end_at
FROM m_season
WHERE start_at <= NOW() AND end_at >= NOW();
```

Expect at least one row (`season_cd=2` on default seed).

---

## K.124 — `GET /api/search`

**Swagger:** **124 Get symbol list**. Leave `query` empty (or `USD`); `limit` empty (default 100).

**Execute.** **Expect 200** array of **5**, order by `priority`:

| `symbol` | `description` |
|----------|----------------|
| USDJPY | 米ドル/円 |
| EURJPY | ユーロ/円 |
| EURUSD | ユーロ/米ドル |
| GBPUSD | 英ポンド/米ドル |
| AUDUSD | 豪ドル/米ドル |

Each item also has `type=FOREX`, `exchange=CTFX`, plus **Extra** `ticker` / `full_name` (`USD/JPY`, …).

`query=ABCDEFGHIJK` (11 chars) → **422**. `limit=0` → **422**.

**MD match:** **Complete** on LIKE CD/JP, `is_deleted=0`, priority sort, query max 10, limit 1–100. **Extra:** `ticker`, `full_name`, optional `exchange`/`type` filters.

**DBeaver:**

```sql
SELECT ccypair_cd, ccypair_jp, is_deleted, priority
FROM m_ccypairs
WHERE is_deleted = 0
ORDER BY priority ASC;
```

Expect 5 rows in the same order as Swagger.

---

## K.125 — `GET /api/marks`

**Swagger:** **125 Get marks list**.

| Param | Value |
|-------|--------|
| `symbol` | `USDJPY` |
| `resolution` | `1D` |
| `from` | `1787011200` |
| `to` | `1787270400` |

**Execute.** **Expect 200**, length **3**:

| id | time | color | label | text |
|----|------|-------|-------|------|
| m1 | 1787011200 | green | B | Buy signal |
| m2 | 1787097600 | red | S | Sell signal |
| m3 | 1787184000 | green | B | Buy signal follow-up |

**Extra** on each: `labelFontColor`, `minSize`. `resolution=10` → **422**. Blank symbol → **422**. Unknown CD → **200** `[]` (**Partial** vs “length 6 always 422”).

**MD match:** **Complete** on query (CD + resolution + time range) and required fields. **Extra:** font/size, seed row m3. **Partial:** invalid length only enforced when blank; column is `resolution` not `chart_type`.

**DBeaver:**

```sql
SELECT id, ccypair_cd, resolution, mark_at, color, label, mark_text
FROM m_tv_mark
WHERE ccypair_cd = 'USDJPY' AND resolution = '1D'
  AND mark_at BETWEEN 1787011200 AND 1787270400
ORDER BY mark_at ASC;
```

Expect 3 rows.

---

## K.126 — `GET /api/timescale_marks`

**Swagger:** **126 Get timescale marks list**. Same params as 125.

**Execute.** **Expect 200**, length **3**: `tm1` / `tm2` / `tm3`. `tooltip` is an **array** e.g. `["Buy event"]` (MD allows string or array). **Extra:** `labelFontColor`.

**MD match:** **Complete** on query and required fields. Tooltip-as-array is allowed for Advanced Charts.

**DBeaver:**

```sql
SELECT id, ccypair_cd, resolution, timescale_mark_at, color, label, tooltip
FROM m_tv_timescale_mark
WHERE ccypair_cd = 'USDJPY' AND resolution = '1D'
  AND timescale_mark_at BETWEEN 1787011200 AND 1787270400
ORDER BY timescale_mark_at ASC;
```

Expect 3 rows.

---

## K.127 — `POST /api/layouts`

**Swagger:** Chart layouts → **127 Register chart layout**. Body:

```json
{
  "name": "My Daily Strategy",
  "content": "{\"panes\":[{\"sources\":[]}]}",
  "symbol": "USDJPY",
  "resolution": "1D"
}
```

**Execute.** **Expect 201** `{ "id": n }`. Note `n`. `ETHUSD` → **404**. `resolution=10` → **422**. Name 65 chars → **422**.

**MD match:** **Complete** on body rules, pair check, insert `customer_no`/`ccypair_cd`/`chart_type`, return id. Status **201** is allowed (MD says 200/201). **Partial:** slash symbols accepted. **Open:** chart Save does **not** call this.

**DBeaver** (use your `id`):

```sql
SELECT id, customer_no, name, content, ccypair_cd, chart_type, updated_at
FROM m_tv_chart_layout
WHERE id = :id;
```

Expect `customer_no=1`, `ccypair_cd=USDJPY`, `chart_type=1D`.

---

## K.128 — `PUT /api/layouts/{id}`

**Swagger:** **128 Update chart layout**. Path `id` = id from 127. Body:

```json
{
  "name": "Renamed",
  "content": "{\"pane\":2}",
  "symbol": "EURUSD",
  "resolution": "60"
}
```

**Execute.** **Expect 200** `{ "id": same }`. Path `abc` → **422**. `999999` → **404**. Authorize as `demo2` on demo’s id → **404**.

**MD match:** **Complete** on numeric id, body, pair check, update name/symbol/resolution/`updated_at`. **Partial:** some Peach update-conditions say keep old `content`; **this app overwrites content** (needed for TV save). **Extra:** tenant 404. **Open:** widget.

**DBeaver:** same SELECT as 127; expect `name=Renamed`, `ccypair_cd=EURUSD`, `chart_type=60`, newer `updated_at`.

---

## K.129 — `GET /api/layouts/{id}`

**Swagger:** **129 Get chart layout**. Path id from 127.

**Execute.** **Expect 200:**

```json
{
  "id": 1,
  "name": "Renamed",
  "timestamp": 172...,
  "content": "{\"pane\":2}"
}
```

Must **not** include `symbol`, `resolution`, or `customer_no`. Non-numeric id → **422**. Missing/other tenant → **404**.

**MD match:** **Complete** on DTO filter and 404. **Extra:** tenant check. **Open:** widget `getChartContent`.

**DBeaver:**

```sql
SELECT id, name, content,
       EXTRACT(EPOCH FROM updated_at)::bigint AS timestamp_unix
FROM m_tv_chart_layout
WHERE id = :id;
```

API `timestamp` ≈ `timestamp_unix`.

---

## K.130 — `GET /api/layouts`

**Swagger:** **130 Get chart layout list**. No path id.

**Execute.** **Expect 200** array, `updated_at DESC`, each item `{ id, name, resolution, symbol, timestamp }`, **no** `content`. Empty user → `[]`.

**MD match:** **Complete**. **Open:** widget `getAllCharts`.

**DBeaver:**

```sql
SELECT id, customer_no, name, ccypair_cd AS symbol, chart_type AS resolution,
       EXTRACT(EPOCH FROM updated_at)::bigint AS timestamp
FROM m_tv_chart_layout
WHERE customer_no = 1
ORDER BY updated_at DESC;
```

Same order as Swagger.

---

## K.131 — `DELETE /api/layouts/{id}`

**Swagger:** **131 Delete chart layout**. Use an id you created in 127 (or POST another “ToDelete” first).

**Execute.** **Expect 200** `{ "t": <unix seconds now> }`. Then GET same id → **404**. Path `abc` → **422**. demo2 deleting demo’s row → **404** and the row **stays**.

**MD match:** **Complete** on hard delete and `{ t }`. **Open:** widget. **Extra:** JSON wrapper `{ t }` (not a bare number).

**DBeaver:**

```sql
SELECT COUNT(*) FROM m_tv_chart_layout WHERE id = :id;
```

Expect `1` before delete, `0` after.

---

## K.132 — `GET /api/indicator-templates`

**Swagger:** Indicator templates → **132 Get indicator template list**.

**Execute** as `demo` on a fresh DB → **200** `[]`. After 133 below → names only, **name ASC**, no `content`.

**MD match:** **Complete** on tenant + names only + empty `[].` Sort is **name ASC** (MD did not specify). **Open:** widget study templates.

**DBeaver:**

```sql
SELECT customer_no, name, content, updated_at
FROM m_tv_indicator_template
WHERE customer_no = 1
ORDER BY name ASC;
```

`content` is in DB only.

---

## K.133 — `POST /api/indicator-templates`

**Swagger:** **133 Register or update indicator template**. Body:

```json
{
  "name": "Triple EMA Crossover",
  "content": "{\"studies\":[{\"name\":\"EMA\"}]}"
}
```

**Execute twice** (wait 1s). **Expect 200** both times `{ "t": unix }` from row `updated_at`. Second call updates content only; list still has **one** name. Blank name / 65-char name / empty content → **422**.

**MD match:** **Complete** on upsert by `(customer_no, name)` and `{ t }`. **Extra:** always **200** (MD allows 201 on first insert). **Open:** widget.

**DBeaver:**

```sql
SELECT id, customer_no, name, content,
       EXTRACT(EPOCH FROM updated_at)::bigint AS t
FROM m_tv_indicator_template
WHERE customer_no = 1 AND name = 'Triple EMA Crossover';
```

One row; `t` ≈ API `t`.

---

## K.134 — `GET /api/indicator-templates/{name}`

**Swagger:** **134 Get indicator template**. Path `name` = `Triple EMA Crossover` (Swagger encodes spaces).

**Execute.** **Expect 200:**

```json
{
  "name": "Triple EMA Crossover",
  "content": "{\"studies\":[{\"name\":\"EMA\"}]}"
}
```

Unknown name → **404**. Name longer than 64 → **422**.

**MD match:** **Complete**. **Open:** widget.

**DBeaver:** same SELECT as 133; API `content` = DB `content`.

---

## K.135 — `DELETE /api/indicator-templates/{name}`

**Swagger:** **135 Delete indicator template**. Same path name.

**Execute.** **Expect 200** `{ "t": now }`. GET again → **404**. Missing name → **404**.

**MD match:** **Complete** hard delete + `{ t }`. **Open:** widget.

**DBeaver:** `COUNT(*)` for that name: `1` then `0`.

---

## K.136 — `GET /api/chart-templates`

**Swagger:** Chart templates → **136 Get chart template list**.

Same shape as 132: `[{ "name": "..." }]` or `[]`, **name ASC**, tenant `customer_no`. Table is **plural** `m_tv_chart_templates` (as spec).

**MD match:** **Complete**. **Open:** widget chart-template Save/Load.

**DBeaver:**

```sql
SELECT customer_no, name, content, updated_at
FROM m_tv_chart_templates
WHERE customer_no = 1
ORDER BY name ASC;
```

---

## K.137 — `POST /api/chart-templates`

**Swagger:** **137 Register or update chart template**. Body:

```json
{
  "name": "Dark Neon Theme",
  "content": "{\"chartproperties\":{\"paneProperties\":{\"background\":\"#131722\"}}}"
}
```

**Execute.** **Expect 200** `{ "t": unix }`. Upsert same as 133. Validation 422 same rules.

**MD match:** **Complete** upsert + `{ t }`. Always **200**. **Open:** widget.

**DBeaver:**

```sql
SELECT id, customer_no, name, content,
       EXTRACT(EPOCH FROM updated_at)::bigint AS t
FROM m_tv_chart_templates
WHERE customer_no = 1 AND name = 'Dark Neon Theme';
```

---

## K.138 — `GET /api/chart-templates/{name}`

**Swagger:** **138 Get chart template**. Path `Dark Neon Theme`.

**Expect 200** `{ "name", "content" }`. Missing → **404**. Name > 64 → **422**.

**MD match:** **Complete**. **Open:** widget.

**DBeaver:** same SELECT as 137.

---

## K.139 — `DELETE /api/chart-templates/{name}`

**Swagger:** **139 Delete chart template**.

**Expect 200** `{ "t": now }`. GET → **404**.

**MD match:** **Complete**. **Open:** widget.

**DBeaver:** `COUNT(*)` 1 then 0 for that name.

---

## K.140 Suggested Execute order in the room

1. Login + Authorize (`demo`).
2. 120 config → 122 time → 123 USDJPY → 124 search → 121 history MID.
3. 125 / 126 with seed window `1787011200`–`1787270400`.
4. 127 POST → note `id` → 130 list → 129 GET → 128 PUT → 129 GET again → 131 DELETE.
5. 133 POST → 132 list → 134 GET → 135 DELETE.
6. 137 POST → 136 list → 138 GET → 139 DELETE.
7. Login as `demo2`, Authorize, GET a `demo` layout id → **404**.

Say once: **127–139 REST is Complete; the chart Save/Load UI is Open.**

---

## K.141 One-page MD match (Swagger body)

| Doc | REST vs MD | Why not 100% if not |
|-----|------------|---------------------|
| 120 | Complete + Extra | Extra `supports_group_request` |
| 121 | Complete + Extra + Partial | Extra `bars[]` / `countBack`; UDF error not 404; mock bars |
| 122 | Complete + Extra | Extra `serverTime`; JWT required |
| 123 | Complete + Extra | Extra ticker / library fields |
| 124 | Complete + Extra | Extra `ticker` / `full_name` |
| 125 | Complete + Extra + Partial | Extra mark fields; bad CD → `[]`; extra seed m3 |
| 126 | Complete + Extra | Tooltip array; extra font color |
| 127 | Complete + Partial | 201; slash symbol; widget Save → POST |
| 128 | Complete + Partial | Overwrites `content`; tenant 404; widget Save → PUT |
| 129–131 | Complete | Widget Load/Remove; 131 `{ t }` wrapper |
| 132–135 | Complete | Indicator Templates menu; 133 always 200 |
| 136–139 | Complete | Chart settings Template; table name plural as spec |

**Never Complete:** Peach S-01 (all **Stub**). Error Japanese sentences (**Partial** copy, codes Complete).

