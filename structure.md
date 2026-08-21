# App structure (reader guide)

Living map of how this demo is built. **Filled now:** process layout, config, database, mentor auth, design docs **120–123**. Later docs (**124–139**) are listed at the bottom and will be expanded as we walk those markdown files.

Related files: [`plan.md`](plan.md) (implementation order), [`checklist.md`](checklist.md) (doc vs demo gaps), [`adjust_plan.md`](adjust_plan.md) (JWT + i18n), [`README.md`](README.md) (how to run).

---

## 1. How the app is built

Three processes. The browser only talks to Vite (`5173`); Vite proxies REST to Java and WebSockets to Python.

```text
Browser  →  Vite :5173
              ├── /api, /curpairs  →  Spring Boot :8080  (Postgres :5432)
              └── /ws              →  Python :8081       (live ticks + forming bars)
```

| Piece | Path | Role |
|---|---|---|
| Frontend | `frontend/` | TradingView Advanced Charts widget, login overlay, datafeed (`datafeed.ts`) |
| Java REST | `backend/` | Auth, UDF datafeed (120–126), layout/template REST (127–132) |
| Python WS | `ws-python/` | Demo FX quotes + live bar stream (`/ws/fx-quotes`, `/ws/stream`) |
| Postgres | `docker-compose.yml` | Durable schema via Flyway |

**Intentional split:** historical bars come from **Java mock** (`MockBarGenerator`). Live candle updates come from **Python**. They share the same pair catalog and BID/ASK/MID math (`DemoMarket` / `ws-python/market.py`). History is capped at wall-clock **now** so live ticks are never older than the last history bar.

---

## 2. Mentor notes — what we implemented

Locked in [`adjust_plan.md`](adjust_plan.md). This is a **local JWT stand-in**, not Peach S-01 SSO.

| Mentor request | In this app |
|---|---|
| Spring Security | `SecurityConfig` + `JwtAuthenticationFilter` |
| Bearer token | `Authorization: Bearer <jwt>` on `/api/**` except health + login |
| Boot authenticates | Stateless JWT; 401 JSON `{ errorCode, message }` |
| Auth DB | Flyway **V7** `m_app_user` |
| BCrypt | `BCryptPasswordEncoder`; seed `demo`/`demo2` |
| 200 / 404 / bad field | **200/201** OK; **404** `CODE:30404`; validation **422** `CODE:30020` (Peach status, not HTTP 400) |
| errorCode + message | Always both; `message` from MessageSource |
| Message bundle | `messages.properties` (EN) + `messages_ja.properties` (`Accept-Language`) |
| REST | Layouts/templates/login are REST. Datafeed paths stay **TradingView UDF** (`/config`, `/history`, …) so the widget works |

Headers after login:

```http
Authorization: Bearer <jwt>
Accept-Language: en
```

Login itself needs **no** Bearer: `POST /api/auth/login` with `{ "username", "password" }`.

---

## 3. Configuration variables

Java binds `app.*` in [`AppProperties`](backend/src/main/java/com/task/chart/config/AppProperties.java).

### 3.1 Runtime — `backend/src/main/resources/application.yml`

Used when you `spring-boot:run` against Docker Postgres.

| Key | Meaning | Default / example |
|---|---|---|
| `spring.application.name` | Boot app name | `chart-backend` |
| `spring.datasource.url` | JDBC URL | `jdbc:postgresql://${DB_HOST:127.0.0.1}:${DB_PORT:5432}/${DB_NAME:chart}` |
| `spring.datasource.username` / `password` | DB login | `chart` / `chart` (`DB_USER`, `DB_PASSWORD`) |
| `spring.jpa.hibernate.ddl-auto` | Hibernate must **not** create tables | `none` (Flyway owns schema) |
| `spring.jpa.open-in-view` | No OSIV | `false` |
| `spring.flyway.enabled` | Run migrations on start | `true` |
| `spring.flyway.locations` | SQL folder | `classpath:db/migration` |
| `server.port` | REST port | `8080` |
| `app.cors-origins` | Allowed browser origins | Vite `5173` and `3000` |
| `app.jwt.secret` | HMAC key for demo JWT | local string (≥ 256 bits). **Replace outside demo.** |
| `app.jwt.expiration-ms` | Token TTL | `86400000` (24h) |
| `app.tradingview.supports-search` | Doc 120 / widget search | `true` |
| `app.tradingview.supports-marks` | Doc 125 | `true` |
| `app.tradingview.supports-timescale-marks` | Doc 126 | `true` |
| `app.tradingview.supports-time` | Doc 122 | `true` |
| `app.tradingview.exchanges` | Exchange label | `CTFX` |
| `app.tradingview.symbols-types` | Symbol type | `FOREX` |
| `app.tradingview.timezone` | Doc 123 | `Asia/Tokyo` |
| `app.tradingview.has-intraday` | Doc 123 | `true` |
| `app.tradingview.visible-plots-set` | Doc 123 | `ohlc` |
| `app.tradingview.has-seconds` | Doc 123 | `true` |
| `app.tradingview.time-summer` / `time-winter` | Session strings from `m_season` | see yml |
| `app.tradingview.search-default-limit` / `search-max-limit` | Doc 124 | `100` |
| `app.tradingview.supported-resolutions` | Chart intervals | `1S` … `1M` |
| `app.tradingview.intraday-multipliers` | Minute multipliers for resolve | `1` … `480` |

Env overrides for Postgres: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

### 3.2 Tests — `backend/src/test/resources/application.yml`

**Overrides the runtime yml for `mvn test`.** Tests do **not** hit Docker Postgres.

| Key | Meaning |
|---|---|
| `spring.datasource.url` | In-memory **H2** `jdbc:h2:mem:chart` with `MODE=PostgreSQL` so Flyway SQL still runs |
| `spring.datasource.driver-class-name` | `org.h2.Driver` |
| `spring.datasource.username` | `sa` |
| `spring.datasource.password` | empty |
| `spring.jpa.hibernate.ddl-auto` | `none` |
| `spring.flyway.enabled` / `locations` | Same migrations as prod (`V1`–`V7`) |

JWT and `app.tradingview.*` still come from the **main** yml unless a test profile overrides them.

### 3.3 Test profile — `application-doc120.yml`

Used by `SystemOverviewDesign120Test` (`@ActiveProfiles("doc120")`). Pins TradingView flags + the 12 resolutions from design doc 120 so the mapping test does not depend on later yml edits.

---

## 4. Database architecture

Flyway SQL: `backend/src/main/resources/db/migration/`. Hibernate does not create tables.

```text
m_ccypairs          currency pair master          docs 123, 124, layout writes
m_season            DST / winter session window   doc 123
m_tv_mark           chart marks                   doc 125
m_tv_timescale_mark timescale marks               doc 126
m_tv_chart_layout   saved layouts (per customer)  docs 127–131
m_tv_indicator_template  indicator templates      docs 132–134
m_app_user          local login (BCrypt + customer_no)  mentor auth
```

**Not in this DB (locked gap):** Peach `t_chart_1` … `t_chart_month` (doc 121). Bars are generated in memory.

### V1 `m_ccypairs`

| Column | Role |
|---|---|
| `ccypair_cd` PK | Peach CD, e.g. `USDJPY` |
| `ccypair_jp` | Japanese name (legend / search) |
| `rate_unit` | `pricescale = 10^rate_unit` |
| `is_deleted` | `0` = active |
| `priority` | Search sort |

Seed: USDJPY, EURJPY, EURUSD, GBPUSD, AUDUSD.

### V2 `m_season`

| Column | Role |
|---|---|
| `season_cd` | `1` = daylight saving → `time-summer`; `2` = standard → `time-winter` |
| `start_at` / `end_at` | Inclusive window vs “now” |

Seed: `season_cd=2` covering 2020–2099 (winter session for resolve).

### V3–V6 (later docs)

Marks and timescale marks keyed by `ccypair_cd` + `resolution` + unix time. Layouts and indicator templates keyed by `customer_no` from the JWT claim.

### V7 `m_app_user`

| Column | Role |
|---|---|
| `username` unique | Login name |
| `password_hash` | BCrypt |
| `customer_no` | Tenant id copied into JWT `customer_no` |
| `enabled` | Must be true to login |

Startup seed (`AppUserSeedRunner`): `demo` → customer **1**, `demo2` → customer **2**.

---

## 5. Request path (all `/api` except health + login)

1. CORS (GET/POST/PUT/DELETE/OPTIONS).
2. `JwtAuthenticationFilter` reads `Authorization: Bearer`.
3. Spring Security: unauthenticated `/api/**` → **401** `E_UNAUTHORIZED`.
4. Controller → service → repository / mock generator.
5. Errors: `{ "errorCode": "…", "message": "<localized>" }` except UDF history `{ "s": "error" }`.

Frontend: `frontend/src/api.ts` sends Bearer + `Accept-Language`. Login: `frontend/src/auth.ts`.

---

## 6. Design docs 120–123

Auth for these (and all other `/api` except health/login): Bearer JWT. Open routes: `GET /api/health`, `POST /api/auth/login`, `GET /curpairs`.

### Shared automated test

From `backend/`:

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign120Test,SystemOverviewDesign121Test,SystemOverviewDesign122Test,SystemOverviewDesign123Test,AuthLoginTest" test
```

Login helper used by those classes: `TestAuthSupport.bearerDemo(mockMvc)` (`demo` / `demo`).

Postman login (once per session):

```http
POST http://127.0.0.1:8080/api/auth/login
Content-Type: application/json

{"username":"demo","password":"demo"}
```

Copy `accessToken`. Later calls: **Authorization** tab → Type **Bearer Token**, or header `Authorization: Bearer <token>`. Login request itself: **No Auth**.

---

### 120 — Get datafeed configuration

| | |
|---|---|
| Doc | [`System_Overview_Design_120_….md`](System_Overview_Design/System_Overview_Design_120_Get_Datafeed_Configuration_Data_(TV).md) |
| Path | `GET /api/config` |
| Tables | none (yml only) |
| Code | `ChartDataController.config` → `ChartDataService.config` |
| Test | `SystemOverviewDesign120Test` |

Widget `onReady` uses this. Extra field `supports_group_request: false` so the library uses `/search`.

**Postman:** `GET /api/config` + Bearer → **200**. Flags true, 12 resolutions, `exchanges`/`symbols_types` CTFX/FOREX. No token → **401**.

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign120Test" test
```

---

### 121 — Get bars

| | |
|---|---|
| Doc | [`System_Overview_Design_121_….md`](System_Overview_Design/System_Overview_Design_121_Get_Bars_(TV).md) |
| Path | `GET /api/history` |
| Warehouse | Flyway **V8** — all 13 `t_chart_*` tables |
| Hot cache | Redis ZSET `peach:{cache_set_*}:{CD}` |
| Code | `ChartBarRepository` + `ChartCacheStore` + `ChartCacheWriter` → `history(…)` |
| Test | `SystemOverviewDesign121Test`, `FlywayMigrationTest` (Redis on `6379`) |

#### Architecture (Phases 1+2)

```
MockBarGenerator ──► ChartCacheWriter ──► t_chart_* (Postgres)
                              │
                              └──► Redis cache_set_*  ◄── GET /api/history (sync lock)
```

| Doc piece | Implementation |
|---|---|
| 13 tables | `t_chart_1` … `t_chart_month` (bid_/ask_ OHLC + `chart_datetime`) |
| Cache namespaces | Redis keys named `cache_set_1s` … `cache_set_month` |
| Mapping table | `CacheNamespace` (chart_type → table → cache name) |
| Writer thread | `ChartCacheWriter` seeds boot; `@Scheduled` upserts DB+Redis |
| Sync | JVM lock per namespace around Redis read/write |
| Validation | `bid_ask` required; CD length 6; from↔to paired; 422 |
| Bar DTO | `{ s, t, o, h, l, c }` (+ widget `bars[]`); MID = avg |
| `nextTime` | max datetime &lt; `from` (weekend gaps on day+) |

#### Doc 121 compliance checklist

| MD requirement | Status |
|---|---|
| Receive bid_ask, symbol, chart type, from*, to* | Done |
| Retrieve cache data (then respond) | Done (Redis; warm from DB if empty) |
| Synchronize API read vs writer | Done |
| Minute vs day/week/month table routing | Done via `CacheNamespace` |
| All 13 `t_chart_*` tables | Done (V8) |
| Cache mapping chart_type ↔ table ↔ `cache_set_*` | Done |
| Validation table + 422 `CODE:30020` | Done |
| resolution → Peach chart_type | Done |
| Filter CD / from / to / all; sort ASC | Done |
| BID/ASK/MID from bid_/ask_ columns | Done |
| `no_data` + empty arrays + `nextTime` | Done |
| Ignore separate “Peach API” doc extras | Done (only what 121 writes) |

**Extras (widget safety, not in MD):** `bars[]` with ms `time`; FE sends Peach-shaped query params; `volume` column.

#### Verify

```bash
docker compose up -d
cd backend
.\mvnw.cmd "-Dtest=SystemOverviewDesign121Test,FlywayMigrationTest" test
```

```sql
SELECT COUNT(*) FROM t_chart_day WHERE curpair_cd = 'USDJPY';
```

```bash
docker compose exec redis redis-cli ZCARD peach:cache_set_day:USDJPY
```

Postman (full host + real unix seconds):

```powershell
$to = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$from = $to - (20 * 86400)
"http://127.0.0.1:8080/api/history?symbol=USDJPY&resolution=1D&from=$from&to=$to&bid_ask=MID"
```

---

### 122 — Get server time

| | |
|---|---|
| Doc | [`System_Overview_Design_122_….md`](System_Overview_Design/System_Overview_Design_122_Get_Server_Time_(TV).md) |
| Path | `GET /api/time` |
| Tables | none |
| Code | `ChartDataController.time` |
| Test | `SystemOverviewDesign122Test` |

Doc field `t` = unix seconds, no ms. Extra `serverTime` (same value) for `datafeed.ts`.

**Postman:** `GET /api/time` + Bearer → `{ "t": 1…, "serverTime": 1… }` within a few seconds of now.

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign122Test" test
```

---

### 123 — Get symbol information

| | |
|---|---|
| Doc | [`System_Overview_Design_123_….md`](System_Overview_Design/System_Overview_Design_123_Get_Symbol_Information_(TV).md) |
| Path | `GET /api/symbols?symbol=` |
| Tables | `m_ccypairs`, `m_season` |
| Code | `ChartDataController.symbols` → `resolve` |
| Test | `SystemOverviewDesign123Test` |

Blank `symbol` → **422**. Unknown / deleted pair → **404** `CODE:30404`. No season row covering now → **500** `E_SERVER`. Accepts `USDJPY` and `USD/JPY`. Chart `name`/`ticker` use slash form so the header is readable; CD is `provider_symbol`.

**Postman:** `GET /api/symbols?symbol=USDJPY` + Bearer → **200**, `pricescale=1000`, `exchange=CTFX`. `ETHUSD` → **404**. `Accept-Language: ja` on 500 → Japanese `message`.

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign123Test" test
```

---

## 7. Docs 124–139 (to be expanded here)

| Doc | Path (current) | In `structure.md` |
|---|---|---|
| 124 search | `GET /api/search` | later |
| 125 marks | `GET /api/marks` | later |
| 126 timescale marks | `GET /api/timescale_marks` | later |
| 127–131 layouts | `/api/layouts` REST | later |
| 132 indicator list | `GET /api/indicator-templates` | later |
| 133–134 indicator upsert/get | planned in `plan.md` | later |
| 135–139 delete + chart templates | out of current `plan.md` slice | later |

Until those sections exist, use [`checklist.md`](checklist.md) for status and test class names.
