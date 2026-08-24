# App structure (reader guide)

Living map of how this demo is built. **Filled now:** process layout, config, database, mentor auth, design docs **120–132** (deep verify + code map below). Later docs (**133–139**) are listed at the bottom.

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
m_ccypairs          currency pair master          docs 123, 124, layout writes (127–128)
m_season            DST / winter session window   doc 123
m_tv_mark           chart marks                   doc 125
m_tv_timescale_mark timescale marks               doc 126
m_tv_chart_layout   saved layouts (per customer)  docs 127–131
m_tv_indicator_template  indicator templates      docs 132–134
m_app_user          local login (BCrypt + customer_no)  mentor auth
t_chart_* (V8)      Peach bar warehouse           doc 121 (13 tables; seeded by MockBarGenerator)
```

**Doc 121 warehouse:** Flyway **V8** creates all 13 `t_chart_*` tables. `ChartCacheWriter` seeds Postgres + Redis from `MockBarGenerator` (not live Peach).

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

## 5.1 Code map — files ↔ design docs 120–132

Paths relative to repo root. **Bold** = primary entry for that doc.

```text
System_Overview_Design/
  System_Overview_Design_120_….md          ← spec 120
  System_Overview_Design_121_….md          ← spec 121
  … 122 … 123 … 124 … 125 … 126 … 127 … 128 … 129 … 130 … 131 … 132 …

backend/
  src/main/resources/
    application.yml                        ← 120, 123, 124 external config (app.tradingview.*)
    application-doc120.yml                 ← 120 test profile only
    db/migration/
      V1__create_m_ccypairs.sql            ← 123, 124, 127, 128 (pair check)
      V2__create_m_season.sql              ← 123 (session)
      V3__create_m_tv_mark.sql             ← 125
      V4__create_m_tv_timescale_mark.sql   ← 126
      V5__create_m_tv_chart_layout.sql     ← 127–131
      V6__create_m_tv_indicator_template.sql ← 132
      V8__create_t_chart_tables.sql        ← 121
  src/main/java/com/task/chart/
    config/
      SecurityConfig.java                  ← shared auth (S-01 JWT stand-in) 120–132
      AppProperties.java                   ← 120, 123, 124 yml binding
      CustomerContext.java                 ← 127–132 customer_no from JWT
    security/
      JwtAuthenticationFilter.java         ← shared auth
      JwtService.java                      ← shared auth
    controller/
      **ChartDataController.java**       ← 120 config, 121 history, 122 time, 123 symbols,
                                           ← 124 search, 125 marks, 126 timescale_marks
      **ChartLayoutController.java**       ← 127 POST, 128 PUT, 129 GET/{id}, 130 GET, 131 DELETE/{id}
      **IndicatorTemplateController.java** ← 132 GET /api/indicator-templates
      AuthController.java                  ← login (mentor JWT; not in 120–132 MD)
    service/
      ChartDataService.java                ← 120–126 interface
      **ChartDataServiceImpl.java**        ← 120–126 implementation
      ChartLayoutService.java              ← 127–131 interface
      **ChartLayoutServiceImpl.java**      ← 127–131 implementation
      IndicatorTemplateService.java        ← 132 interface
      **IndicatorTemplateServiceImpl.java** ← 132 implementation
      MockBarGenerator.java                ← 121 bar seed (mock Peach writer input)
      impl/MockBarGeneratorImpl.java       ← 121
    dto/response/
      DatafeedConfigResponse.java          ← 120
      HistoryResponse.java                 ← 121
      ServerTimeResponse.java              ← 122
      SymbolInfoDto.java                   ← 123
      SearchSymbolDto.java                 ← 124
      MarkDto.java                         ← 125
      TimescaleMarkDto.java                ← 126
      ChartLayoutIdResponse.java           ← 127, 128 response { id }
      ChartLayoutDto.java                  ← 129 response (id, name, timestamp, content)
      ChartLayoutListItemDto.java          ← 130 list item (no content)
      SystemDatetimeResponse.java          ← 131 delete response { t }
      IndicatorTemplateListItemDto.java    ← 132 list item (name only)
    dto/request/
      RegisterChartLayoutRequest.java      ← 127, 128 JSON body
    entity/
      Ccypair.java                         ← 123, 124, 127, 128
      Season.java                          ← 123
      TvMark.java                          ← 125
      TvTimescaleMark.java                 ← 126
      TvChartLayout.java                   ← 127–131
      TvIndicatorTemplate.java             ← 132
    repository/
      CcypairRepository.java               ← 123, 124 (+ 127/128 pair lookup)
      SeasonRepository.java                ← 123
      TvMarkRepository.java                ← 125
      TvTimescaleMarkRepository.java       ← 126
      TvChartLayoutRepository.java         ← 127–131
      TvIndicatorTemplateRepository.java   ← 132
    cache/                                 ← 121 only
      CacheNamespace.java, ChartBarRepository.java, ChartCacheStore.java, ChartCacheWriter.java
    util/
      ResolutionMapper.java                ← 121 history, 125/126/127/128 resolution lists
    constants/
      ErrorCodes.java                      ← 422/404 codes all docs
      MarkSeedWindow.java                  ← 125, 126 test/Postman window constants
  src/test/java/com/task/chart/controller/
    SystemOverviewDesign120Test.java       ← verify 120
    SystemOverviewDesign121Test.java       ← verify 121
    SystemOverviewDesign122Test.java       ← verify 122
    SystemOverviewDesign123Test.java       ← verify 123
    SystemOverviewDesign124Test.java       ← verify 124
    SystemOverviewDesign125Test.java       ← verify 125
    SystemOverviewDesign126Test.java       ← verify 126
    SystemOverviewDesign127Test.java       ← verify 127
    SystemOverviewDesign128Test.java       ← verify 128
    SystemOverviewDesign129Test.java       ← verify 129
    SystemOverviewDesign130Test.java       ← verify 130
    SystemOverviewDesign131Test.java       ← verify 131
    SystemOverviewDesign132Test.java       ← verify 132
    FlywayMigrationTest.java               ← V1–V8 seeds incl. marks tables

frontend/src/
  datafeed/datafeed.ts                     ← 120 onReady, 121 getBars, 122 getServerTime,
                                           ← 123 resolveSymbol, 124 searchSymbols,
                                           ← 125 getMarks, 126 getTimescaleMarks
  api.ts                                   ← Bearer + Accept-Language for all /api calls
  auth.ts                                  ← login overlay → JWT
  save-load-adapter.ts                     ← NOT wired to 127–131 / 132 yet (localStorage)
```

---

## 6. Design docs 120–132

Auth for these (and all other `/api` except health/login): Bearer JWT. Open routes: `GET /api/health`, `POST /api/auth/login`, `GET /curpairs`.

### Shared automated test (120–132)

From `backend/`:

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign120Test,SystemOverviewDesign121Test,SystemOverviewDesign122Test,SystemOverviewDesign123Test,SystemOverviewDesign124Test,SystemOverviewDesign125Test,SystemOverviewDesign126Test,SystemOverviewDesign127Test,SystemOverviewDesign128Test,SystemOverviewDesign129Test,SystemOverviewDesign130Test,SystemOverviewDesign131Test,SystemOverviewDesign132Test" test
```

121 also needs Redis for full context boot: `docker compose up -d redis` (or run `FlywayMigrationTest` separately).

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
| Path | `GET /api/symbols?symbol=` (UDF query param; doc says “request body” but Peach path is unspecified — keep query for the widget) |
| Tables | `m_ccypairs` (V1), `m_season` (V2) |
| Auth | Bearer JWT on `/api/**` (local stub; doc S-01 not wired) |
| Code | `ChartDataController.symbols` → `ChartDataService.resolve` → `ChartDataServiceImpl` |
| DTO | `SymbolInfoDto` (Jackson snake_case field names match TradingView `LibrarySymbolInfo`) |
| FE | `datafeed.ts` → `resolveSymbol` → `GET /api/symbols?symbol=…` |
| Test | `SystemOverviewDesign123Test` |

#### Design intent (mentor talking points)

1. **Doc vs widget:** Peach DTO requires `name` = currency pair CD (`USDJPY`). TradingView’s legend is more readable with a slash. We set **`name` = CD** (doc) and **`ticker` = `USD/JPY`** (widget extra). FE / history already normalize by stripping non-letters, so both forms stay safe.
2. **Normalize then validate:** Accept `USDJPY`, `USD/JPY`, `FX:USD/JPY`. Strip `FX:` prefix and `/`, uppercase, then require **length == 6** → else **422** `CODE:30020`. Checking length on the raw string would break the widget.
3. **Session from season master:** Doc *1 — look up `m_season` where `season_cd` ∈ {1 DST, 2 standard} and now ∈ `[start_at, end_at]`. DST → `app.tradingview.time-summer`; else → `time-winter`. No row → **500** `E_SERVER`.
4. **Config fields from yml:** Same external settings as doc 120/123 “External Configuration Information” (`AppProperties.TradingView`).
5. **Extras kept on purpose:** `ticker`, `listed_exchange`, `format`, `seconds_multipliers`, daily/weekly/monthly flags & multipliers, `data_status`, `provider_symbol`. Peach clients ignore unknown keys; removing them breaks Advanced Charts.

#### Request

| Param | Required | Where | Rules |
|---|---|---|---|
| `symbol` | yes | query | After normalize: length **6**. Blank / wrong length → **422** `CODE:30020` |
| `Authorization` | yes | header | `Bearer <accessToken>`; missing → **401** |

Normalize helper (`ChartDataServiceImpl.normalizeCcypairCd`):

```
trim → UPPER → drop leading "FX:" → remove "/"
```

#### Processing flow

```
GET /api/symbols?symbol=…
        │
        ▼
   JWT filter (401 if missing/invalid)
        │
        ▼
 requireCcypairCd ── blank / len≠6 ──► 422 CODE:30020
        │
        ▼
 m_ccypairs WHERE ccypair_cd=? AND is_deleted=0
        │
        ├── miss ──► 404 CODE:30404
        ▼
 currentSession() from m_season + now
        │
        ├── no row ──► 500 E_SERVER
        ▼
 toSymbolInfo(pair, session) → SymbolInfoDto JSON
```

#### Tables / entities

**`m_ccypairs`** (Flyway V1) — entity `Ccypair`

| Column | Use in 123 |
|---|---|
| `ccypair_cd` | Match `symbol`; response `name` + `provider_symbol` |
| `ccypair_jp` | Response `description` |
| `rate_unit` | `pricescale = 10^rate_unit` (USDJPY `3` → `1000`; EURUSD `5` → `100000`) |
| `is_deleted` | Must be `0` (`Ccypair.ACTIVE`); else 404 |
| `priority` | Not used by resolve (used by search / 124) |

Seeded pairs: `USDJPY`, `EURJPY`, `EURUSD`, `GBPUSD`, `AUDUSD`.

**`m_season`** (Flyway V2) — entity `Season`

| Column | Use in 123 |
|---|---|
| `season_cd` | `1` = `Season.DAYLIGHT_SAVING` → `time-summer`; `2` = `Season.STANDARD` → `time-winter` |
| `start_at` / `end_at` | Instant range must contain `Instant.now()` |

Default seed: one row `season_cd=2` from 2020→2099 (standard / winter session).

Repo call: `SeasonRepository.findBySeasonCdInAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtDesc` with CDs `{1,2}`, both bounds = now; take first row.

#### Response mapping (doc DTO + extras)

| Field | Source | Doc? |
|---|---|---|
| `name` | `ccypair_cd` | Yes |
| `description` | `ccypair_jp` | Yes |
| `timezone` | `app.tradingview.timezone` | Yes |
| `exchange` | `app.tradingview.exchanges` | Yes |
| `minmov` | literal `1` | Yes |
| `pricescale` | `10^rate_unit` | Yes |
| `type` | `app.tradingview.symbols-types` | Yes |
| `session` | summer/winter from season | Yes (*1) |
| `has_intraday` | yml | Yes |
| `visible_plots_set` | yml (`ohlc`) | Yes |
| `supported_resolutions` | yml list | Yes |
| `intraday_multipliers` | yml list | Yes |
| `has_seconds` | yml | Yes |
| `ticker` | slash display (`USD/JPY`) | Extra (widget) |
| `listed_exchange` | same as `exchange` | Extra |
| `format` | `"price"` | Extra |
| `seconds_multipliers` | `ResolutionMapper.SECONDS_MULTIPLIERS` (`["1"]`) | Extra |
| `has_daily` / `daily_multipliers` | `true` / `["1","3"]` | Extra |
| `has_weekly_and_monthly` / weekly / monthly | `true` / `["1"]` | Extra |
| `data_status` | `"streaming"` | Extra |
| `provider_symbol` | `ccypair_cd` | Extra (explicit CD helper) |

Constructor order in `SymbolInfoDto`: `(ticker, name, description, type, exchange, listed_exchange, session, timezone, minmov, pricescale, …)`.

#### External config (`application.yml` → `AppProperties.TradingView`)

| Setting | Reference value |
|---|---|
| `exchanges` | `CTFX` |
| `symbols-types` | `FOREX` |
| `timezone` | `Asia/Tokyo` |
| `has-intraday` | `true` |
| `visible-plots-set` | `ohlc` |
| `has-seconds` | `true` |
| `time-summer` | `0700-3000:2\|0600-3000:345\|0600-2940:6` |
| `time-winter` | `0700-3100:2\|0700-3100:345\|0700-3040:6` |
| `supported-resolutions` | `1S`,`1`,`5`,`15`,`30`,`60`,`120`,`240`,`480`,`1D`,`1W`,`1M` |
| `intraday-multipliers` | `1`,`5`,`15`,`30`,`60`,`120`,`240`,`480` |

#### Errors

| Case | HTTP | `errorCode` |
|---|---|---|
| Missing / invalid token | 401 | (security entry point) |
| Missing / blank / length ≠ 6 after normalize | 422 | `CODE:30020` |
| Unknown or deleted pair | 404 | `CODE:30404` |
| No season covering now | 500 | `E_SERVER` (localized message; `Accept-Language: ja` → Japanese) |

#### Code map (where to look)

| Concern | Location |
|---|---|
| HTTP entry | `ChartDataController.symbols(@RequestParam symbol)` |
| Service API | `ChartDataService.resolve(String)` |
| Validate + normalize | `ChartDataServiceImpl.requireCcypairCd` / `normalizeCcypairCd` |
| DB lookup | `CcypairRepository.findByCcypairCdAndIsDeleted` |
| Session | `ChartDataServiceImpl.currentSession` |
| DTO build | `ChartDataServiceImpl.toSymbolInfo` / `displayTicker` / `priceScale` |
| Response type | `dto.response.SymbolInfoDto` |
| Error codes | `constants.ErrorCodes` + `GlobalExceptionHandler` |
| Widget call | `frontend/src/datafeed/datafeed.ts` → `resolveSymbol` |

#### Doc 123 compliance checklist

| MD requirement | Status |
|---|---|
| Token authentication (S-01 referenced) | Done as local JWT stub |
| `symbol` required, length 6 → else 422 `CODE:30020` | Done (after normalize) |
| Load `m_ccypairs` by CD + `is_deleted=0` | Done |
| Miss → 404 `CODE:30404` | Done |
| Map CD → `name`, JP → `description`, `pricescale`, `minmov=1` | Done |
| Config fields from external settings | Done (yml) |
| Session from `m_season` DST/standard + trading hours | Done |
| No season → system error 500 `E_SERVER` | Done |
| Query vs “request body” | Intentional UDF query |
| Extra LibrarySymbolInfo fields | Kept (not in MD table) |

#### Verify

```powershell
cd backend
.\mvnw.cmd "-Dtest=SystemOverviewDesign123Test" test
```

**Postman** (after login Bearer):

```http
GET http://127.0.0.1:8080/api/symbols?symbol=USDJPY
Authorization: Bearer <token>
```

Expect **200**: `name=USDJPY`, `ticker=USD/JPY`, `description=米ドル/円`, `pricescale=1000`, `exchange=CTFX`, `type=FOREX`, `session` = winter string (default seed), `supported_resolutions` length 12.

Also try: `symbol=USD/JPY` → 200 same `name`; `symbol=ETH` → 422; `symbol=ETHUSD` → 404; no token → 401.

**UI:** login → chart resolves symbol → Network `/api/symbols` shows CD in `name`; header/legend can still show slash via `ticker`.

---

### 124 — Get symbol list (search)

| | |
|---|---|
| Doc | [`System_Overview_Design_124_….md`](System_Overview_Design/System_Overview_Design_124_Get_Symbol_List_(TV).md) |
| Path | `GET /api/search?query=&limit=` |
| Table | `m_ccypairs` (V1) |
| Code | `ChartDataController.search` → `ChartDataServiceImpl.search` → `CcypairRepository.searchActive` |
| DTO | `SearchSymbolDto` (`symbol`, `description`, `type`, `exchange` + widget extras) |
| FE | `datafeed.ts` → `searchSymbols` |
| Test | `SystemOverviewDesign124Test` |

#### Request params

| Param | Required | Rules | Code |
|---|---|---|---|
| `query` | no | max **10** chars → else **422** | `needle.length() > 10` |
| `limit` | no | default **100**, range **1–100** → else **422** | `resolveSearchLimit` + yml |
| `exchange` | no | Extra: if set and ≠ `CTFX` → **200 `[]`** | `matchesConfiguredFilter` |
| `type` | no | Extra: if set and ≠ `FOREX` → **200 `[]`** | same |
| Bearer | yes | missing → **401** | `SecurityConfig` |

Empty `query` → all active pairs (up to `limit`), sorted **`priority` ASC**, `is_deleted=0`.

#### Response mapping

| Doc field | Source | Notes |
|---|---|---|
| `symbol` | `ccypair_cd` | Done |
| `description` | `ccypair_jp` | Done |
| `type` | yml `FOREX` | Done |
| `exchange` | yml `CTFX` | Done |
| `ticker`, `full_name` | slash display | **Extra** — widget SYMBOL column |

#### Doc 124 compliance (deep verify)

| MD step / rule | Status | Evidence |
|---|---|---|
| §1 Token (S-01) | Done (JWT stub) | `124Test` missing token → 401 |
| §2 `query` max 10 → 422 | **Done** | `queryLongerThanTenReturns422` |
| §2 `limit` 1–max → 422 | **Done** | `limitZeroReturns422`, `limitAboveMaxReturns422` |
| §3 default `limit=100` | **Done** | yml + empty query returns 5 pairs |
| §4 match CD or JP partial | **Done** | `query=USD`, `query=円`, `USD/JPY` |
| §4 `is_deleted=0` | **Done** | `deletedPairIsExcluded` |
| §4 sort `priority` asc | **Done** | empty query order USDJPY…AUDUSD |
| DTO four fields | **Done** | `mapsDocFieldsAndWidgetExtras` |
| Query param vs “body” | **Keep** | UDF convention |
| Widget `exchange`/`type` filter | **Extra** | not in MD |

**Gaps vs strict MD:** none blocking demo. Extras are intentional for TradingView.

#### Verify 124

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign124Test" test
```

Postman: `GET /api/search` + Bearer → 5 pairs. `query=USD` → subset. `query=ABCDEFGHIJK` (11 chars) → 422. `limit=0` → 422.

---

### 125 — Get marks list

| | |
|---|---|
| Doc | [`System_Overview_Design_125_….md`](System_Overview_Design/System_Overview_Design_125_Get_Marks_List_(TV).md) |
| Path | `GET /api/marks?symbol=&resolution=&from=&to=` |
| Table | `m_tv_mark` (V3) |
| Code | `ChartDataController.marks` → `ChartDataServiceImpl.marks` |
| DTO | `MarkDto` |
| FE | `datafeed.ts` → `getMarks`; config `supports_marks: true` (120) |
| Test | `SystemOverviewDesign125Test` |

#### Request params

| Param | Required | Rules |
|---|---|---|
| `symbol` | yes | non-blank; normalized (`USD/JPY` → `USDJPY`) |
| `resolution` | yes | must be in marks list (**no `10`**) |
| `from`, `to` | yes | unix seconds; `to >= from` |
| All missing/invalid | | **422** `CODE:30020` |

**Note:** MD says `symbol` length 6. App validates **blank only**, not length after normalize — same as pre-123-alignment marks path. Wrong CD → empty `[]`, not 404.

#### DB filter (doc §3)

`ccypair_cd` = symbol, `resolution` = param, `mark_at` ∈ `[from, to]` inclusive, ordered ASC. No rows → **`[]`** (not 404).

Seed window (V3): `MarkSeedWindow.FROM=1787011200`, `TO=1787270400` — three USDJPY `1D` marks `m1`–`m3`.

#### Response mapping

| Doc field | DB column | Extra |
|---|---|---|
| `id` | `id` | |
| `time` | `mark_at` | |
| `color` | `color` | |
| `label` | `label` | |
| `text` | `mark_text` | |
| `labelFontColor` | — | `#ffffff` (library) |
| `minSize` | — | `14` (library) |

#### Doc 125 compliance (deep verify)

| MD rule | Status | Evidence |
|---|---|---|
| Token | Done (JWT stub) | 401 without Bearer |
| All params required | **Done** | four missing-param tests |
| `resolution=10` → 422 | **Done** | `unsupportedResolutionReturns422` |
| `to < from` → 422 | **Done** | `toBeforeFromReturns422` |
| Filter + DTO mapping | **Done** | `seedWindowReturnsBuyAndSellMarks` |
| Empty range → `[]` | **Done** | `rangeWithNoMarksReturnsEmptyArray` |
| `symbol` length 6 → 422 | **Partial** | not enforced; `ETH` may query empty |
| Accept slash symbol | **Extra** | `displaySymbolStillReturnsMarks` |

#### Verify 125

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign125Test" test
```

Postman: `GET /api/marks?symbol=USDJPY&resolution=1D&from=1787011200&to=1787270400` → 3 marks. Wrong resolution `60` → `[]`. UI: pins at top of chart.

---

### 126 — Get timescale marks list

| | |
|---|---|
| Doc | [`System_Overview_Design_126_….md`](System_Overview_Design/System_Overview_Design_126_Get_Timescale_Marks_List_(TV).md) |
| Path | `GET /api/timescale_marks?symbol=&resolution=&from=&to=` |
| Table | `m_tv_timescale_mark` (V4) |
| Code | `ChartDataController.timescaleMarks` → `ChartDataServiceImpl.timescaleMarks` |
| DTO | `TimescaleMarkDto` |
| FE | `datafeed.ts` → `getTimescaleMarks`; config `supports_timescale_marks: true` |
| Test | `SystemOverviewDesign126Test` |

Same validation and filter rules as **125** (`validateMarksRequest` shared). Seed marks `tm1`–`tm3` in same window.

#### Response mapping

| Doc field | Source | Notes |
|---|---|---|
| `id`, `color`, `label`, `time` | DB | `time` = `timescale_mark_at` |
| `tooltip` | DB string | **Partial:** JSON is **`["…"]`** because TradingView `TimescaleMark.tooltip` is `string[]` |
| `labelFontColor` | — | **Extra** `#ffffff` |

#### Doc 126 compliance (deep verify)

| MD rule | Status |
|---|---|
| Token + validation (same as 125) | **Done** |
| Filter + DTO | **Done** |
| `tooltip` scalar in MD | **Partial** (array in JSON) |
| `symbol` length 6 | **Partial** (same as 125) |
| Config flag enabled | **Done** | `supportsTimescaleMarksTrue` |

#### Verify 126

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign126Test" test
```

Postman: same window as 125 on `/api/timescale_marks`. UI: labels on **time axis**.

---

### 127 — Register chart layout

| | |
|---|---|
| Doc | [`System_Overview_Design_127_….md`](System_Overview_Design/System_Overview_Design_127_Register_Chart_Layout_(TV).md) |
| Path | `POST /api/layouts` (JSON body) |
| Tables | `m_tv_chart_layout` (V5), `m_ccypairs` (pair check) |
| Code | `ChartLayoutController.register` → `ChartLayoutServiceImpl.register` |
| Request | `RegisterChartLayoutRequest` (`name`, `content`, `symbol`, `resolution`) |
| Response | `ChartLayoutIdResponse` → `{ "id": n }` |
| Tenant | `customer_no` from JWT → `CustomerContext` |
| Test | `SystemOverviewDesign127Test` |

#### Body validation

| Field | Rules | Code |
|---|---|---|
| `name` | required, max **64** | `validateUpsertBody` |
| `content` | required, non-blank | same |
| `symbol` | required; normalize slash → CD | `normalizeCcypairCd` |
| `resolution` | required; marks resolution list | `ResolutionMapper.isMarksResolution` |
| invalid | **422** | |

Pair must exist with `is_deleted=0` → else **404** `CODE:30404`.

#### Register mapping (doc update conditions)

| Column | Value |
|---|---|
| `customer_no` | JWT claim |
| `name` | body `name` |
| `content` | body `content` |
| `ccypair_cd` | normalized `symbol` |
| `chart_type` | body `resolution` |
| `updated_at` | now (**Extra** — needed for doc 129 `timestamp`) |

Response: **201 Created** `{ "id": … }` (REST; MD says return layout id).

#### Doc 127 compliance (deep verify)

| MD rule | Status | Evidence |
|---|---|---|
| Token | Done | 401 |
| Body validation | **Done** | name/content/symbol/resolution tests |
| Active pair check | **Done** | unknown/deleted → 404 |
| Register columns | **Done** | `happyPathPersistsRowAndReturnsId` + DBeaver |
| `symbol` length 6 strict | **Partial** | `USD/JPY` accepted |
| Response bare id vs `{id}` | **Extra wrapper** | Peach may want bare number |
| SaveLoadAdapter wired | **Open** | still localStorage |

#### Verify 127

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign127Test" test
```

Postman:

```http
POST http://127.0.0.1:8080/api/layouts
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My layout","content":"{\"pane\":1}","symbol":"USDJPY","resolution":"1D"}
```

Expect **201** `{ "id": 1 }`. SQL: `SELECT * FROM m_tv_chart_layout WHERE id=1` → `customer_no=1`, `ccypair_cd=USDJPY`, `chart_type=1D`.

---

### 128 — Update chart layout

| | |
|---|---|
| Doc | [`System_Overview_Design_128_….md`](System_Overview_Design/System_Overview_Design_128_Update_Chart_Layout_(TV).md) |
| Path | `PUT /api/layouts/{id}` (same JSON body as 127) |
| Tables | `m_tv_chart_layout`, `m_ccypairs` |
| Code | `ChartLayoutController.update` → `ChartLayoutServiceImpl.update` |
| Test | `SystemOverviewDesign128Test` |

#### Path + body

| Check | Result |
|---|---|
| Path `{id}` non-numeric (S-11) | **422** `CODE:30020` |
| Layout id not found | **404** |
| Other customer's layout | **404** (Extra tenant guard) |
| Body validation | same as 127 |

#### Update mapping — **known MD conflict**

Doc **update conditions** table says `content` = **keep existing** `[1].content`.  
App **writes body `content`** via `TvChartLayout.applyUpdate` so TradingView save-overwrite works.

| Column | App behavior |
|---|---|
| `name` | body `name` |
| `content` | **body `content`** (intentional vs MD table) |
| `ccypair_cd` | normalized `symbol` |
| `chart_type` | body `resolution` |
| `updated_at` | bumped |

Response: **200** `{ "id": same }`.

#### Doc 128 compliance (deep verify)

| MD rule | Status | Evidence |
|---|---|---|
| §2 Path id numeric | **Done** | `nonNumericIdReturns422` |
| §3 Body validation | **Done** | blank name, bad resolution |
| §4 Layout exists | **Done** | unknown id → 404 |
| §5 Active pair | **Done** | `ETHUSD` → 404 |
| §6 Update + return id | **Done** | `happyPathUpdatesRowAndReturnsSameId` |
| §6 content keep existing | **Intentional gap** | we overwrite content |
| Tenant isolation | **Extra** | `otherCustomerReturns404` |
| `symbol` length 6 | **Partial** | slash accepted |

#### Verify 128

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign128Test" test
```

Round-trip: POST (127) → note `id` → PUT with new name/content/symbol/resolution → **200** same id → SQL row updated, `updated_at` newer.

---

### 129 — Get chart layout

| | |
|---|---|
| Doc | [`System_Overview_Design_129_….md`](System_Overview_Design/System_Overview_Design_129_Get_Chart_Layout_(TV).md) |
| Path | `GET /api/layouts/{id}` |
| Table | `m_tv_chart_layout` (V5) |
| Code | `ChartLayoutController.get` → `ChartLayoutServiceImpl.get` → `ChartLayoutDto` |
| Tenant | `requireOwnedLayout` — row must match JWT `customer_no` |
| Test | `SystemOverviewDesign129Test` |

#### Path + retrieval

| Check | Result |
|---|---|
| Path `{id}` non-numeric (S-11) | **422** `CODE:30020` |
| Layout id not found | **404** `CODE:30404` |
| Other customer's layout | **404** (Extra tenant guard) |
| Missing Bearer | **401** |

Lookup: `TvChartLayoutRepository.findById` then compare `customer_no` to `CustomerContext.get()`.

#### Response mapping (doc chart layout DTO)

| Doc field | DB column | Notes |
|---|---|---|
| `id` | `id` | Done |
| `name` | `name` | Done |
| `timestamp` | `updated_at` | unix seconds via `getEpochSecond()` |
| `content` | `content` | Done |

**Not returned on GET** (unlike list DTO 130): `symbol`, `resolution`, `customer_no`.

#### Doc 129 compliance (deep verify)

| MD rule | Status | Evidence |
|---|---|---|
| §1 Token (S-01) | Done (JWT stub) | `missingTokenReturns401` |
| §2 Path id numeric | **Done** | `nonNumericIdReturns422` |
| §3 Layout exists | **Done** | `unknownIdReturns404` |
| §4 DTO id/name/timestamp/content | **Done** | `happyPathReturnsDtoAfterRegister` |
| Tenant isolation | **Extra** | `otherCustomerReturns404` |
| Round-trip with 128 | **Done** | `registerUpdateGetRoundTrip` — timestamp bumps after PUT |
| SaveLoadAdapter `getChartContent` | **Open** | still localStorage |

#### Verify 129

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign129Test" test
```

Postman round-trip:

```http
POST http://127.0.0.1:8080/api/layouts
Authorization: Bearer <token>
Content-Type: application/json

{"name":"RoundTrip","content":"{\"pane\":1}","symbol":"USDJPY","resolution":"1D"}
```

Note `id` from **201**, then:

```http
GET http://127.0.0.1:8080/api/layouts/{id}
Authorization: Bearer <token>
```

Expect **200**: `{ "id", "name", "timestamp", "content" }` only. After PUT (128), GET shows updated `name`/`content` and `timestamp` ≥ before.

SQL:

```sql
SELECT id, name, content,
       EXTRACT(EPOCH FROM updated_at)::bigint AS timestamp_unix
FROM m_tv_chart_layout
WHERE id = {id};
```

API `timestamp` ≈ `timestamp_unix`.

---

### 130 — Get chart layout list

| | |
|---|---|
| Doc | [`System_Overview_Design_130_….md`](System_Overview_Design/System_Overview_Design_130_Get_Chart_Layout_List_(TV).md) |
| Path | `GET /api/layouts` (no `{id}`) |
| Table | `m_tv_chart_layout` (V5) |
| Code | `ChartLayoutController.list` → `ChartLayoutServiceImpl.list` → `ChartLayoutListItemDto` |
| Repo | `findByCustomerNoOrderByUpdatedAtDesc(customerNo)` |
| Test | `SystemOverviewDesign130Test` |

#### Retrieval rules

| MD rule | App behavior |
|---|---|
| Filter by token `customer_no` | Done via `CustomerContext` |
| Sort `updated_at` DESC | Done in repository query |
| Empty customer | **200 `[]`** (not 404) |

#### Response mapping (list item DTO)

| Doc field | DB column | Notes |
|---|---|---|
| `id` | `id` | Done |
| `name` | `name` | Done |
| `resolution` | `chart_type` | Done |
| `symbol` | `ccypair_cd` | Done |
| `timestamp` | `updated_at` | unix seconds |
| `content` | — | **Omitted** (by design — load via 129) |

#### Doc 130 compliance (deep verify)

| MD rule | Status | Evidence |
|---|---|---|
| §1 Token | Done | `missingTokenReturns401` |
| §2 Filter by customer | **Done** | `returnsOnlyCurrentCustomerLayouts` |
| §2 Sort update datetime DESC | **Done** | `sortsByUpdatedAtDescending`, `updatedLayoutMovesTowardFront` |
| List DTO five fields | **Done** | no `content` in JSON |
| Empty list | **Done** | `emptyListWhenCustomerHasNoLayouts` |
| SaveLoadAdapter `getAllCharts` | **Open** | still localStorage |

#### Verify 130

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign130Test" test
```

Postman: create two layouts (127), update one (128), then `GET /api/layouts` + Bearer → updated layout first; each item has `id`, `name`, `resolution`, `symbol`, `timestamp`; no `content`.

SQL:

```sql
SELECT id, customer_no, name, ccypair_cd AS symbol, chart_type AS resolution,
       EXTRACT(EPOCH FROM updated_at)::bigint AS timestamp
FROM m_tv_chart_layout
WHERE customer_no = 1
ORDER BY updated_at DESC;
```

Order and field values should match Postman list.

---

### 131 — Delete chart layout

| | |
|---|---|
| Doc | [`System_Overview_Design_131_….md`](System_Overview_Design/System_Overview_Design_131_Delete_Chart_Layout_(TV).md) |
| Path | `DELETE /api/layouts/{id}` |
| Table | `m_tv_chart_layout` (V5) |
| Code | `ChartLayoutController.delete` → `ChartLayoutServiceImpl.delete` → `SystemDatetimeResponse` |
| Test | `SystemOverviewDesign131Test` |

#### Path + delete flow

| Check | Result |
|---|---|
| Path `{id}` non-numeric | **422** |
| Layout not found | **404** |
| Other customer's layout | **404**; row **kept** |
| Success | Hard delete via `repository.delete(layout)` |

Response: **200** `{ "t": unixSeconds }` where `t` = system datetime at delete time (`SystemDatetimeResponse`).

#### Doc 131 compliance (deep verify)

| MD rule | Status | Evidence |
|---|---|---|
| §1 Token | Done | `missingTokenReturns401` |
| §2 Path id numeric | **Done** | `nonNumericIdReturns422` |
| §3 Layout exists | **Done** | `unknownIdReturns404` |
| §4 Hard delete | **Done** | GET after delete → 404 |
| Return system datetime | **Done** | `happyPathDeletesAndReturnsSystemDatetime` |
| Tenant isolation | **Extra** | `otherCustomerReturns404AndRowRemains` |
| Response bare number vs `{t}` | **Extra wrapper** | Peach may want bare unix |
| SaveLoadAdapter `removeChart` | **Open** | still localStorage |

#### Verify 131

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign131Test" test
```

Postman: POST (127) → note `id` → `DELETE /api/layouts/{id}` + Bearer → **200** `{ "t": … }` → GET same id → **404** → GET list → id absent.

SQL before/after:

```sql
SELECT COUNT(*) FROM m_tv_chart_layout WHERE id = {id};
```

Expect `1` then `0`.

---

### 132 — Get indicator template list

| | |
|---|---|
| Doc | [`System_Overview_Design_132_….md`](System_Overview_Design/System_Overview_Design_132_Get_Indicator_Template_List_(TV).md) |
| Path | `GET /api/indicator-templates` |
| Table | `m_tv_indicator_template` (V6) |
| Code | `IndicatorTemplateController.list` → `IndicatorTemplateServiceImpl.list` → `IndicatorTemplateListItemDto` |
| Repo | `findByCustomerNoOrderByNameAsc(customerNo)` |
| Test | `SystemOverviewDesign132Test` |

V6 schema: `customer_no`, `name` (unique per customer), `content`, `updated_at`. **No Flyway seed** — table starts empty; tests seed rows; Postman needs manual INSERT or test run.

#### Retrieval rules

| MD rule | App behavior |
|---|---|
| Filter by token `customer_no` | Done |
| DTO `name` only | Done — `content` not exposed on list |
| Empty customer | **200 `[]`** |
| Sort order in MD | not specified — app sorts **name ASC** |

#### Response mapping

| Doc field | DB column | Notes |
|---|---|---|
| `name` | `name` | only field returned |

#### Doc 132 compliance (deep verify)

| MD rule | Status | Evidence |
|---|---|---|
| §1 Token | Done | `missingTokenReturns401` |
| §2 Filter by customer | **Done** | `returnsOnlyCurrentCustomerNamesSortedAsc` |
| DTO name only | **Done** | no `content` / `customer_no` in JSON |
| Empty list | **Done** | `emptyListWhenCustomerHasNoTemplates` |
| Sort name ASC | **Extra** | MD silent; stable UX |
| Upsert API (133) / get by name (134) | **Open** | not implemented |
| SaveLoadAdapter `getAllStudyTemplates` | **Open** | still localStorage |

#### Verify 132

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign132Test" test
```

Postman (seed in DBeaver first):

```sql
INSERT INTO m_tv_indicator_template (customer_no, name, content, updated_at) VALUES
  (1, 'Alpha', '{"study":1}', NOW()),
  (1, 'Zulu', '{"study":2}', NOW()),
  (2, 'Other', '{"study":3}', NOW());
```

```http
GET http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
```

Expect **200**: `[{ "name": "Alpha" }, { "name": "Zulu" }]` — sorted A→Z; no `Other` when logged in as `demo`.

SQL:

```sql
SELECT customer_no, name, content
FROM m_tv_indicator_template
WHERE customer_no = 1
ORDER BY name ASC;
```

---

## 7. Docs 133–139 (to be expanded here)

| Doc | Path (current) | In `structure.md` |
|---|---|---|
| 133 indicator upsert | `POST /api/indicator-templates` | later (planned in `plan.md`) |
| 134 get indicator by name | `GET /api/indicator-templates/{name}` | later |
| 135–139 delete + chart templates | out of current `plan.md` slice | later |

Until those sections exist, use [`checklist.md`](checklist.md) and [`test.md`](test.md) for status and manual verify steps.
