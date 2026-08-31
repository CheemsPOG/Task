# Chart backend — senior mentoring guide

This document is a walkthrough of **this** Spring Boot app (`backend/`, package `com.task.chart`), written so you can maintain it without an AI sitting next to you.

It is **not** a line-by-line tour. Read the architecture first, then follow the learning order, then open the named classes with the “what to look for” questions.

**How to use this file**

1. Read sections 1–5 once without opening code. Get the picture.
2. Follow the **recommended learning order**. Open only the files listed for that step.
3. For each step, answer the “prove you understood it” questions in your own words.
4. When you change production code, use section 2.5 as the comment/review bar.

**What this repo is (one sentence)**

A **TradingView Advanced Charts** demo: Java on `:8080` is the source of truth for login, pair catalog, OHLC history, marks, and saved layouts; a **separate Python process** on `:8081` only relays Redis ticks/bars to the browser; the **Vite frontend** on `:5173` hosts the widget.

Java **does not** serve WebSockets. Python **does not** compute OHLC. Mixing those two facts up is the most common way to debug the wrong process.

---

## 1. What this application does (business)

A FX dealer (CTFX / “Peach” in the design docs) needs a web chart that looks like TradingView:

- Show **USD/JPY, EUR/JPY, EUR/USD, GBP/USD, AUD/USD**.
- Draw **candles** (open/high/low/close) on many timeframes (1 second through 1 month).
- Switch **BID / ASK / MID** (buy price, sell price, midpoint).
- Pin **marks** on the chart (news-style labels).
- Let each customer **save named layouts** (drawings, indicators, which pair is open) so F5 restores *their* chart, not someone else’s.
- Show a **live ticker** in the header while candles update.

This backend is a **local stand-in** for Peach production:

| Production (Peach, not in this repo) | This demo |
|--------------------------------------|-----------|
| Real SSO (design “S-01”) | Username/password → JWT (`demo` / `demo`) |
| Real liquidity-provider ticks | `DemoTickEngine` random-walk around seeded prices |
| Peach Redis + warehouse already filled | Java seeds Postgres + Redis at boot, then ticks every ~333ms |

So when a product person says “the chart is wrong,” you are usually answering one of:

1. **History wrong** → Java `GET /api/history` / Redis `cache_set_*` / `t_chart_*`.
2. **Live candle stuck** → Java `TickIngestWorker` + Redis `peach:bars` + Python `/ws/stream` + frontend `streaming.ts`.
3. **Ticker stuck** → Java `QuoteBus` + Redis `peach:quotes` + Python `/ws/fx-quotes`.
4. **Wrong customer’s layout** → JWT `customer_no` + `m_tv_chart_layout`.

---

## 2. Main modules / features

Think in **product features**, not folders.

### Feature A — Login (extra vs design docs 120–139)

- `POST /api/auth/login` with `{ "username": "demo", "password": "demo" }`.
- Returns a **1-hour access JWT** in JSON.
- Sets an **HttpOnly cookie** `chart_refresh_token` (opaque UUID in Redis).
- `POST /api/auth/refresh` mints a new access JWT from that cookie.
- `POST /api/auth/logout` deletes the Redis UUID and clears the cookie.

Tenant key: JWT claim `customer_no`. `demo` → `1`, `demo2` → `2`. Layouts for customer 1 are invisible to customer 2 (they look like 404, not 403 — that is intentional so you do not leak ids).

### Feature B — TradingView datafeed (design docs 120–126)

The widget is a TypeScript library. It does **not** know Postgres. It calls a documented HTTP shape:

| Doc | HTTP | Purpose |
|-----|------|---------|
| 120 | `GET /api/config` | Resolutions, exchanges, “do we support marks?” |
| 121 | `GET /api/history` | Past candles as arrays `t,o,h,l,c` |
| 122 | `GET /api/time` | Server unix seconds |
| 123 | `GET /api/symbols` | One pair’s metadata (pricescale, session, timezone) |
| 124 | `GET /api/search` | Type-ahead search over pairs |
| 125 | `GET /api/marks` | Pins on the price series |
| 126 | `GET /api/timescale_marks` | Labels on the time axis |

All of these except `/api/health` need `Authorization: Bearer <access JWT>`.

### Feature C — Saved charts and templates (docs 127–139)

| Docs | HTTP | Table |
|------|------|--------|
| 127–131 | `/api/layouts` | `m_tv_chart_layout` |
| 132–135 | `/api/indicator-templates` | `m_tv_indicator_template` |
| 136–139 | `/api/chart-templates` | `m_tv_chart_templates` |

`content` is an opaque JSON blob the widget understands. Java stores and returns it. Java does **not** parse drawings.

### Feature D — Pair catalog for the header ticker (extra)

- `GET /curpairs` (note: **not** under `/api`).
- Same rows as `m_ccypairs`, but JSON uses numeric `curpairCd` = `priority` (`1` = USDJPY).
- Redis ticks use that numeric id as a **string**: `"curpairCd": "1"`.

### Feature E — Live market (not HTTP)

Java writes Redis. Python reads Redis. Browser opens WebSockets **to Vite**, which proxies to Python.

```
DemoTickEngine (fake LP)
  → TickIngestWorker (~333ms)
      → Postgres t_chart_*     (warehouse)
      → Redis cache_set_*      (history hot cache)
      → Redis PUBLISH peach:quotes   → Python /ws/fx-quotes → header
      → Redis PUBLISH peach:bars     → Python /ws/stream    → candles
```

**You cannot understand live candles by reading only controllers.** Ingest is a background job, not a request.

---

## 2.5 How a senior comments code in *this* project (review bar)

You asked for professional comments “in each file.” This backend **already** uses a house style. Do not paste tutorial comments on every getter. Do this instead.

### What is already there (CTFX house style)

Almost every Java type has:

1. Copyright header.
2. Class javadoc: **what this type is**, **which design doc**, **who calls it**, and an explicit **NOT** list (what it is not).
3. A History table (Ver / Date / Author / Comment) — team convention for reviews.
4. Method javadoc when the contract is not obvious (`@param`, `@return`).
5. Short `//` comments on **non-obvious control flow** (why this branch exists), not on `return list;`.

Example of a *useful* “NOT” sentence from `TickIngestWorker`:

> Python only relays those Redis channels. `GET /api/history` reads `ChartCacheStore`; it does not compute OHLC.

That sentence prevents the next engineer from “fixing” candles in the wrong class.

### What a senior adds when they *change* a file

When you open a PR, reviewers look for:

| Add a comment when… | Example in this repo |
|---------------------|----------------------|
| The next person will guess the wrong process | “Python does not compute bars.” |
| A 404 is a security choice, not a bug | “Other tenants' layouts look missing so we do not leak ids.” (`ChartLayoutServiceImpl`) |
| Two similar query params exist | “Widget may send `price=mid` instead of `bid_ask=MID`.” (`ChartDataServiceImpl.history`) |
| ThreadLocals can leak | “ThreadLocals must not leak across Tomcat worker reuse.” (`JwtAuthenticationFilter`) |
| Order of boot matters | “ChartCacheWriter (@Order 100) must finish before ingest (@Order 200).” |
| A table name must never come from the request | `CacheNamespace` javadoc: never pass a request string as a table name |

### What a senior does *not* add

- `// increment i` / `// return the list`
- Repeating the method name in English
- Copying Spring documentation into every `@RestController`
- Comments that go stale (`// TODO fix later` with no ticket)

### File-by-file “reviewer comment map”

When you *maintain* a file, the comment that belongs there is the **invariant**, not the syntax:

| File | Invariant a reviewer expects to see (already mostly in javadoc) |
|------|------------------------------------------------------------------|
| `ChartBackendApplication` | Scheduling is on; ingest is a background writer |
| `SecurityConfig` | Which paths are public vs JWT; this is not Peach SSO |
| `JwtAuthenticationFilter` | Sets `CustomerContext`; always clears in `finally` |
| `CustomerContext` | Request-scoped tenant; not a HTTP session |
| `AuthServiceImpl` | Refresh UUID never in JSON; only cookie + Redis |
| `ChartDataController` | HTTP only; no SQL; history does not write bars |
| `ChartDataServiceImpl` | History reads Redis; last bar is ingest’s forming candle |
| `ChartCacheStore` | ZSET first; warehouse is cold fallback; Python does not read ZSETs |
| `ChartBarRepository` | JDBC into `t_chart_*`; table name from enum only |
| `TickIngestWorker` | Only live OHLC writer; replace `DemoTickEngine` for a real LP |
| `QuoteBus` | Channel/key names must match Python `market.py` |
| `CacheNamespace` | TV `1` = 1 minute = Peach `1M`, not 1 month |
| `ChartLayoutServiceImpl` | Tenant filter; other customer → 404 |
| `GlobalExceptionHandler` | Maps exceptions to `errorCode` + localized `message` |
| `JsonUnauthorizedEntryPoint` | Filter-chain 401 (missing Bearer), not `@RestControllerAdvice` |
| Flyway `V*.sql` | Schema is source of truth; `ddl-auto: none` |

If you add a **new** class, copy that pattern: purpose, caller, NOT-list, History row, then `//` only on the surprising `if`.

---

## 3. What happens from HTTP in to HTTP out

Spring Boot is a **servlet** app. One request = one thread (Tomcat worker) walking a **filter chain**, then a **controller method**, then JSON out.

### Concrete example: `GET /api/history?symbol=GBPUSD&resolution=1D&from=…&to=…&bid_ask=BID`

The browser (via Vite proxy) sends:

```
GET http://127.0.0.1:5173/api/history?...
Authorization: Bearer eyJ...
Accept-Language: en
```

Vite forwards to `http://127.0.0.1:8080/api/history?...`.

**Step-by-step inside Java**

1. **Tomcat** accepts the TCP connection and creates an `HttpServletRequest`.

2. **CORS** (`WebConfig`) — if this were a browser hitting `:8080` directly, the `Origin` header would be checked. With Vite proxy, Origin is often same-origin to `:5173`, so you rarely think about this in local demo.

3. **Spring Security filter chain** (`SecurityConfig`):
   - CSRF is **off** (stateless API + JWT; the widget is not a form site).
   - Session is **STATELESS** — no `JSESSIONID` login.
   - `JwtAuthenticationFilter` runs **before** the old username/password filter.

4. **`JwtAuthenticationFilter.doFilterInternal`**:
   - Reads `Authorization`.
   - If it starts with `Bearer `, `JwtService.parseToken` checks HS256 signature + expiry.
   - On success: puts `ChartPrincipal(username, customerNo)` into `SecurityContextHolder`, and `CustomerContext.set(customerNo)`.
   - On bad token: leaves the request **anonymous** (does not write 401 itself).
   - Then `filterChain.doFilter(...)` continues.
   - **`finally` always runs**: `CustomerContext.clear()` and `SecurityContextHolder.clearContext()`.  
     If you forget that, the next request on the same Tomcat thread could see the previous customer. That is a real incident class.

5. **Authorization matcher**:
   - `/api/history` matches `/api/**` → **must be authenticated**.
   - If there was no valid Bearer, Spring calls `JsonUnauthorizedEntryPoint` → HTTP 401 JSON `{ errorCode: E_UNAUTHORIZED, message: ... }`.
   - This 401 does **not** go through `GlobalExceptionHandler`. Two 401 paths exist on purpose (filter vs service throwing `UnauthorizedAppException`).

6. **DispatcherServlet** maps URL + GET to `ChartDataController.history(...)`.
   - Controller **does not** open SQL.
   - It binds query params (`symbol`, `resolution`, `from`, `to`, `countBack`, `price`, `bid_ask`) and calls the service.

7. **`ChartDataServiceImpl.history`**:
   - If `bid_ask` is blank but `price=mid` is set, it fills BID/ASK/MID from `price` (widget quirk).
   - `validateHistoryRequest` — missing symbol, not 6 letters after stripping slashes, bad resolution, `from`/`to` mismatch → `ValidationException` (later 422).
   - `symbolCatalog.find(symbolName)` — unknown pair → `ValidationException` (422, not 404). History is stricter than resolve.
   - `CacheNamespace.fromTvResolution("1D")` → `CACHE_SET_DAY` → Redis key like `peach:cache_set_day:GBPUSD`.
   - `chartCacheStore.query(...)` reads Redis ZSET. If empty, warms from `t_chart_day` (warehouse).
   - Each `CachedChartBar` has bid *and* ask OHLC. `toBarDto(BID)` picks the bid columns.
   - Returns `HistoryResponse` with arrays `t,o,h,l,c` (unix **seconds** in `t`).

8. **Jackson** serializes the record/DTO to JSON. The widget’s `datafeed.ts` converts `t` to milliseconds.

9. If anything threw `ValidationException` / `ResourceNotFoundException` / etc., **`GlobalExceptionHandler`** turns it into `{ errorCode, message }` and the right HTTP status. Controllers do not `try/catch` that.

**What this request does *not* do**

- It does not talk to Python.
- It does not subscribe to Redis pub/sub.
- It does not use `CustomerContext` (history is **global** market data, not per-customer). Layouts would.

### Second example: `POST /api/layouts` (tenant write)

Same steps 1–5, then `ChartLayoutController.register` → `ChartLayoutServiceImpl.register`:

- `CustomerContext.requireCustomerNo()` — missing context is a **500** (`ServerErrorException`), because the filter should have set it. That is a bug, not “please log in.”
- Validates name/content/symbol/resolution.
- Checks `m_ccypairs` is active — unknown pair → **404**.
- `tvChartLayoutRepository.save(...)` INSERT with that `customer_no`.
- `@Transactional` so the INSERT commits when the method returns normally.

---

## 4. Packages and responsibilities

All production code lives under `backend/src/main/java/com/task/chart/`.

```
com.task.chart
├── ChartBackendApplication      ← JVM entry, @EnableScheduling
├── controller                   ← HTTP mapping only
├── service + service.impl       ← business rules
├── repository                   ← Spring Data JPA for m_* tables
├── entity                       ← JPA rows for m_* tables
├── dto.request / dto.response   ← JSON in/out (never expose entity)
├── security                     ← JWT, cookies, tenant ThreadLocal
├── cache                        ← Redis + JDBC warehouse + ingest
├── config                       ← yml bindings, CORS, BCrypt, seed users, OpenAPI
├── exception                    ← typed errors + @RestControllerAdvice
├── constants                    ← error codes, BID/ASK/MID, mark seed window
└── util                         ← ResolutionMapper (TV string ↔ ms / Peach type)
```

There is **no** `websocket` package. WebSockets are Python.

---

## 5. How packages depend on each other

**Allowed direction (keep this in your head):**

```
controller  →  service  →  repository/entity
                 ↓
               cache (history + ingest)
                 ↓
           Redis / JDBC

controller  →  service  →  security (AuthService uses JwtService, cookies)

filter (security)  →  JwtService, CustomerContext

exception  ←  thrown by service
exception handler  →  LocalizedMessageService
```

**Illegal (do not introduce):**

- Controller talking to `EntityManager` or Redis.
- `entity` depending on `controller`.
- Python classes imported from Java (they are a different process).

**Dependency injection** is constructor injection everywhere (`private final Foo foo` + constructor). Spring creates one singleton bean per `@Service` / `@Component` / `@RestController` unless you say otherwise. You never write `new ChartDataServiceImpl(...)` in a controller.

---

## 6. How the application talks to the database

Two styles, on purpose.

### A. Spring Data JPA (the `m_*` master tables)

- `spring.jpa.hibernate.ddl-auto: none` — Hibernate **must not** create tables. Flyway owns schema.
- Interface like `TvChartLayoutRepository extends JpaRepository<TvChartLayout, Long>` plus query methods:

```java
List<TvChartLayout> findByCustomerNoOrderByUpdatedAtDesc(long customerNo);
```

Spring generates the SQL from the method name. You do not write `SELECT` in Java for these.

Entities: `Ccypair`, `Season`, `TvMark`, `TvTimescaleMark`, `TvChartLayout`, `TvIndicatorTemplate`, `TvChartTemplate`, `AppUser`.

### B. JDBC warehouse (the `t_chart_*` bar tables)

`ChartBarRepository` is **not** a JPA repository. It uses `JdbcTemplate` and **enum-chosen table names** (`CacheNamespace.tableName()` → `t_chart_day`, etc.).

Why: 13 almost-identical tables (one per resolution). JPA entities for all 13 would be noise. SQL injection risk is avoided because the table name never comes from the HTTP query string.

### Redis (not SQL, but “data”)

`StringRedisTemplate`:

| Key / channel | Writer | Reader |
|---------------|--------|--------|
| `peach:{cache_set_*}:{USDJPY}` ZSET | Ingest + boot seeder | `GET /api/history` |
| `peach:quote:{curpairCd}` + PUBLISH `peach:quotes` | `QuoteBus` | Python `/ws/fx-quotes` |
| `peach:forming:{resolution}:{name}` + PUBLISH `peach:bars` | `QuoteBus` | Python `/ws/stream` |
| `peach:auth:refresh:{uuid}` | `RefreshTokenStore` | `AuthServiceImpl.refresh` |

`GET /api/history` does **not** SUBSCRIBE. It is a point-in-time ZRANGE.

---

## 7. Entities / tables and how they relate

**There are almost no foreign keys in SQL.** Relationships are enforced in Java (or not at all).

```
m_ccypairs (PK ccypair_cd = 'USDJPY')
    ↑ application check (active pair) when saving a layout
    ↑ copied into t_chart_*.curpair_cd
    ↑ marks.ccypair_cd

m_app_user.customer_no  ──logical──►  m_tv_chart_layout.customer_no
                                      m_tv_indicator_template.customer_no
                                      m_tv_chart_templates.customer_no
     (no FK; JWT is the join)

m_season                    standalone; one STANDARD row 2020–2099 for session strings

m_tv_mark / m_tv_timescale_mark   global (no customer_no)

t_chart_1, t_chart_60, … t_chart_month   warehouse; PK (curpair_cd, chart_datetime)
```

| Table | Flyway | JPA entity? | Purpose |
|-------|--------|-------------|---------|
| `m_ccypairs` | V1 | `Ccypair` | Pair catalog; `priority` = WS `curpairCd` |
| `m_season` | V2 | `Season` | Doc 123 `session` summer vs winter |
| `m_tv_mark` | V3 | `TvMark` | Doc 125 |
| `m_tv_timescale_mark` | V4 | `TvTimescaleMark` | Doc 126 |
| `m_tv_chart_layout` | V5 | `TvChartLayout` | Saved charts |
| `m_tv_indicator_template` | V6 | `TvIndicatorTemplate` | Study templates |
| `m_app_user` | V7 | `AppUser` | Local login |
| `t_chart_*` (13 tables) | V8 | **No** | Bar warehouse |
| `m_tv_chart_templates` | V9 | `TvChartTemplate` | Chart style templates |

**Pair list you will memorize:**

| `ccypair_cd` | `priority` / WS id | Display |
|--------------|--------------------|---------|
| USDJPY | 1 | USD/JPY |
| EURJPY | 2 | EUR/JPY |
| EURUSD | 3 | EUR/USD |
| GBPUSD | 4 | GBP/USD |
| AUDUSD | 5 | AUD/USD |

**Layout row (the one you will debug most):**

- `id` — widget Save/Load id
- `customer_no` — tenant
- `name` — “My layout”
- `content` — huge JSON
- `ccypair_cd` — `GBPUSD` (no slash)
- `chart_type` — widget resolution string e.g. `1D` (column name is historical; it is not Peach `DAY`)
- `updated_at` — exposed as unix `timestamp` in list APIs

---

## 8. Authentication / authorization / security

This is **authentication of a user**, not Peach SSO, and **authorization is only “logged in” + tenant id**. There is no admin role, no per-endpoint ROLE_ADMIN.

### Access token (JWT)

- Algorithm HS256, secret `app.jwt.secret` (local demo — not a production secret).
- Lifetime 1 hour (`access-expiration-ms: 3600000`).
- Claims: `sub` = username, `customer_no` = long.
- Sent as `Authorization: Bearer …` on every datafeed and layout call.
- Stored in the **browser tab** (`sessionStorage`), so closing the tab drops the access token.

### Refresh token (not a JWT)

- Random UUID.
- Redis key `peach:auth:refresh:{uuid}` holding username + customer_no, TTL 24h.
- Browser never reads it (HttpOnly cookie `chart_refresh_token`).
- `POST /api/auth/refresh` is **public** (no Bearer) so an expired access token can still rotate.

### What “authorization” means here

After login, Spring puts `ROLE_USER` on everyone. `SecurityConfig` only checks **authenticated vs anonymous**.

Real isolation is:

```java
long customerNo = CustomerContext.requireCustomerNo();
// then findByCustomerNo… or compare layout.getCustomerNo() != customerNo → 404
```

If you add a new tenant table and forget that filter, **that is a data leak**. Put it in the first code review.

### Public vs protected (memorize)

**No JWT:** `GET /api/health`, `POST /api/auth/login|refresh|logout`, Swagger UI, OPTIONS.

**JWT required:** everything else under `/api/**`, and `GET /curpairs`.

---

## 9. Where validation happens

Three layers. Do not look in only one.

### Layer 1 — Spring MVC (malformed HTTP)

`GlobalExceptionHandler` catches:

- `MethodArgumentNotValidException` (bean validation annotations — little used here)
- `HttpMessageNotReadableException` (broken JSON body)
- `MethodArgumentTypeMismatchException` (`from=abc` when a `Long` is expected)

All become **422** `CODE:30020` with message key `error.bad_request`.

`LoginRequest` is a record **without** `@NotBlank`. Empty body is handled in `AuthServiceImpl` as validation/bad credentials, not Jakarta Validation.

### Layer 2 — Service methods (business rules)

This is where **most** 422s come from. Pattern: throw `new ValidationException()` with **no message**. The handler always sends the same localized validation text. You cannot attach “resolution 10 is invalid for marks” to the JSON today without changing that design.

Examples:

- `ChartDataServiceImpl.validateHistoryRequest` — symbol length 6, resolution in `HISTORY_RESOLUTIONS`, `bid_ask` parseable, `from`/`to` both present or both absent.
- `ChartDataServiceImpl.search` — query longer than 10 characters.
- `ChartLayoutServiceImpl.validateUpsertBody` — name length, blank content, resolution in **marks** list (no `10` minute).

### Layer 3 — Domain parse helpers

- `PriceComponent.fromBidAsk("NOPE")` throws `IllegalArgumentException`, caught and turned into `ValidationException`.
- `ResolutionMapper.isHistoryResolution` / `isMarksResolution`.

**404 vs 422 (easy to mix up)**

| Situation | Status | Why |
|-----------|--------|-----|
| `GET /api/symbols?symbol=NOPE` | 404 | Unknown pair (`ResourceNotFoundException`) |
| `GET /api/history?symbol=NOPE` | 422 | History treats unknown catalog hit as validation |
| `GET /api/layouts/999` other customer’s id | 404 | Hide existence |
| Layout symbol not in `m_ccypairs` | 404 | Pair missing |
| Non-numeric layout id `abc` | 422 | Path parse |

When debugging, do not “fix” history to 404 without reading the tests (`SystemOverviewDesign121Test`).

---

## 10. Where exceptions are handled

**Controllers never catch business exceptions.** That is deliberate.

| Thrown | HTTP | errorCode | Who throws |
|--------|------|-----------|------------|
| `ValidationException` | 422 | `CODE:30020` | Services |
| `ResourceNotFoundException` | 404 | `CODE:30404` | Services |
| `BadCredentialsAppException` | 401 | `E_BAD_CREDENTIALS` | `AuthServiceImpl` wrong password |
| `UnauthorizedAppException` | 401 | `E_UNAUTHORIZED` | Bad/missing refresh cookie |
| `ServerErrorException` | 500 | `E_SERVER` | Missing `CustomerContext`, missing `m_season` row |
| Filter anonymous on protected URL | 401 | `E_UNAUTHORIZED` | `JsonUnauthorizedEntryPoint` |

Messages: `messages.properties` / `messages_ja.properties` via `LocalizedMessageService`, language from `Accept-Language`.

If you `e.printStackTrace()` in a controller, you are fighting this design.

---

## 11. Where transactions are used

`@Transactional` is on **service impl methods that touch JPA**, not on controllers, not on ingest.

| Class | Typical methods | Why |
|-------|-----------------|-----|
| `ChartLayoutServiceImpl` | register/update/delete write; get/list `readOnly` | One INSERT/UPDATE/DELETE per call |
| `ChartTemplateServiceImpl` | same | |
| `IndicatorTemplateServiceImpl` | same | |
| `CurrencyPairServiceImpl` | list/find `readOnly` | Simple selects |
| `AuthServiceImpl.login` | `readOnly` | Only reads `m_app_user`; Redis refresh is **outside** JPA |

**Not transactional:**

- `ChartDataServiceImpl` — Redis + JPA reads; no `@Transactional`. Fine for reads. A mid-request Redis fail is not rolled back because there is nothing to roll back.
- `TickIngestWorker.tick` — many pairs × many resolutions every 333ms. Each upsert is its own JDBC statement. A failed pair should not abort the whole tick (you would confirm in the worker’s loop if you change it).

**Mental model:** `@Transactional` = “this method’s JPA work is one unit of commit.” Redis `PUBLISH` is **not** in that unit. If you save a layout and then fail to publish a quote, those are independent.

---

## 12. Configuration that actually matters

File: `backend/src/main/resources/application.yml`.

| Key | Why you care |
|-----|----------------|
| `spring.datasource.*` | Postgres `chart` DB; docker-compose defaults |
| `spring.data.redis.*` | Same Redis Python uses |
| `spring.jpa.hibernate.ddl-auto: none` | Never let Hibernate invent schema |
| `spring.flyway.locations` | `classpath:db/migration` |
| `server.port: 8080` | Vite proxies here |
| `app.cors-origins` | Direct hits to Java from Vite origins |
| `app.jwt.secret` | **Demo only**; rotating it logs everyone out |
| `app.jwt.access-expiration-ms` | 1h access token |
| `app.jwt.refresh-expiration-ms` | 24h cookie/Redis |
| `app.chart-cache.tick-ms` | 333 — live update cadence |
| `app.tradingview.*` | Copied into `GET /api/config` and symbol session strings |

Environment overrides: `DB_HOST`, `REDIS_HOST`, etc.

Tests use extra yaml under `src/test/resources` (H2, fake JWT) — if a test fails only in CI, check those profiles.

---

## 13. Most important entry points

**Process start**

1. `ChartBackendApplication.main` → Spring container.
2. Flyway runs V1…V9.
3. `AppUserSeedRunner` — ensure `demo` / `demo2` exist.
4. `ChartCacheWriter` `@Order(100)` — seed `t_chart_*` + Redis from `MockBarGenerator`.
5. `TickIngestWorker` `@Order(200)` — align demo LP, publish snapshots.
6. `@Scheduled` `tick()` starts.

**HTTP**

| You are debugging… | Open this first |
|--------------------|-----------------|
| Widget cannot load | `ChartDataController` + JWT |
| Login | `AuthController` → `AuthServiceImpl` |
| Save/Load | `ChartLayoutController` |
| Header pairs | `CurrencyPairController` |
| 401 with no body from service | `JsonUnauthorizedEntryPoint` |
| 422/404 JSON | `GlobalExceptionHandler` |

**Non-HTTP (still “entry”)**

- `TickIngestWorker.tick` — live prices.
- `QuoteBus.publish` / `publishForming` — contract with Python.

---

## 14. Where the real business logic lives

If you only had two hours, read these:

1. **`ChartDataServiceImpl`** — docs 120–126: search/resolve/history/marks, BID/ASK/MID projection, 422 rules.
2. **`TickIngestWorker` + `CachedChartBar` + `CacheNamespace`** — how a tick becomes a candle.
3. **`ChartLayoutServiceImpl`** — tenant CRUD; 404 hiding.
4. **`AuthServiceImpl` + `JwtAuthenticationFilter` + `CustomerContext`** — who is “the user.”
5. **`ChartCacheStore`** — why Redis empty ≠ no_data forever (warehouse warm).

`DemoTickEngine` is **fake market physics**, not FX product rules. Replace it later; do not build features inside it.

`MockBarGeneratorImpl` is **boot history only**. After seed, ingest owns the last bar.

---

## 15. Boilerplate / framework vs business requirements

### Framework / plumbing (you rarely change unless upgrading Spring)

- `pom.xml` starters (Web, JPA, Security, Flyway, Redis, Validation, springdoc)
- `ChartBackendApplication`
- `PasswordConfig` (BCrypt bean)
- `OpenApiConfig` / `@Operation` on controllers (mentor Swagger)
- `WebConfig` CORS
- Constructor injection, `@RestController`, `@Service`
- `JpaRepository` method-name queries
- `GlobalExceptionHandler` *structure* (the **codes** are business)

### Business / design-doc (you change when product changes)

- Which resolutions exist (`ResolutionMapper`, `CacheNamespace`, yml, Flyway `t_chart_*`)
- History JSON shape `{ s, t, o, h, l, c }`
- `bid_ask` vs `price`
- Tenant `customer_no` on layouts/templates
- Redis channel names `peach:quotes` / `peach:bars` (Python must match)
- Mark seed windows
- Session strings `time-summer` / `time-winter`
- Demo users and `customer_no` 1 vs 2

### Intentional demo fakes (do not treat as Peach production)

- `DemoTickEngine`
- `MockBarGenerator`
- `app.jwt.secret` in yaml
- `AppUserSeedRunner`

---

## 16. Architectural patterns in *this* code

| Pattern | Where you see it |
|---------|------------------|
| **Controller → Service → Repository** | `ChartLayoutController` → `ChartLayoutServiceImpl` → `TvChartLayoutRepository` |
| **DTO vs entity** | HTTP never returns `TvChartLayout`; it returns `ChartLayoutDto` / `ChartLayoutListItemDto` |
| **Constructor DI** | Every service; Spring wires the graph |
| **Interface + Impl** | CTFX naming: `ChartDataService` / `ChartDataServiceImpl` (one impl each) |
| **Filter for cross-cutting auth** | `JwtAuthenticationFilter` |
| **ThreadLocal request context** | `CustomerContext` (tenant without passing `long customerNo` through every method) |
| **@ControllerAdvice** | `GlobalExceptionHandler` |
| **Scheduled worker** | `TickIngestWorker` |
| **ApplicationRunner boot sequence** | Seed cache `@Order(100)`, then ingest `@Order(200)` |
| **Cache-aside** | Redis ZSET, miss → SQL warehouse, then fill Redis |
| **Pub/sub fan-out** | Redis PUBLISH → another process (Python) |
| **Enum as strategy** | `CacheNamespace`, `PriceComponent` |
| **Stateless API** | JWT; `SessionCreationPolicy.STATELESS` |

**Not used:** hexagonal ports, CQRS, Kafka, Spring WebSocket, multi-module Maven.

---

# Recommended learning / review order

Do not start at `entity` or `dto`. Start at the **edge** you can hit with curl, then walk inward.

### Step 0 — Run the three processes (30 min)

You cannot learn ingest from a stopped Redis.

1. Postgres + Redis (docker-compose).
2. `backend` `./mvnw spring-boot:run` → `:8080`.
3. `ws-python` `python server.py` → `:8081`.
4. `frontend` `npm start` → `:5173`.

Prove: `GET /api/health` is 200 without a token. `GET /api/config` is 401 without a token.

### Step 1 — One HTTP success path (1–2 hours)

Read in this order:

1. `SecurityConfig` (public vs authenticated)
2. `AuthController` + `AuthServiceImpl.login` (stop before refresh if tired)
3. `JwtAuthenticationFilter` (Bearer → `CustomerContext`)
4. `ChartDataController.config` + `ChartDataServiceImpl.config`
5. `application.yml` `app.tradingview`

**Prove:** curl login, then curl `/api/config` with the JWT. Change `supports-marks` in yml, restart, see config JSON change.

**Before Step 2:** You must be able to explain why `/api/health` works without login and `/api/config` does not.

### Step 2 — History (the hard one) (half a day)

1. `CacheNamespace` — TV `1` vs Peach `1M` vs table `t_chart_60`
2. `ResolutionMapper.periodMillis` / `toPeachChartType`
3. `ChartDataServiceImpl.history` + `validateHistoryRequest`
4. `ChartCacheStore.query` (Redis then warehouse)
5. `CachedChartBar.toBarDto` (BID/ASK/MID)
6. `HistoryResponse`

**Prove:** same `from`/`to` with `bid_ask=BID` vs `ASK` returns different `c` (close). Empty range returns `s=no_data` not 404.

**Before Step 3:** You must know history **reads** ingest’s last bar, it does not compute it.

### Step 3 — Ingest (half a day)

1. `ChartCacheWriter` (boot seed)
2. `MockBarGeneratorImpl.peachBarAt` (deterministic past)
3. `TickIngestWorker.tick`
4. `DemoTickEngine` (only to know it is fake)
5. `QuoteBus` channel names
6. Skim `ws-python/market.py` `QUOTE_CHANNEL` / `BAR_CHANNEL` — they must match

**Prove:** stop Python, history still works. Stop Java ingest (or Redis), live header dies.

**Before Step 4:** You must not look for OHLC math in Python.

### Step 4 — Symbols, search, season, marks (2–3 hours)

1. `Ccypair` + V1 SQL
2. `ChartDataServiceImpl.resolve` / `search`
3. `Season` + `currentSession`
4. `TvMark` / `TvTimescaleMark` repositories

**Prove:** `USD/JPY` and `USDJPY` both resolve. Soft-deleted pair does not.

### Step 5 — Layouts (2–3 hours)

1. `CustomerContext.requireCustomerNo`
2. `ChartLayoutController` (HTTP verbs)
3. `ChartLayoutServiceImpl` (404 for other tenant)
4. `TvChartLayout` entity + V5

**Prove:** login `demo`, save layout, login `demo2`, that id is 404.

### Step 6 — Templates (1 hour)

Same shape as layouts: `IndicatorTemplateServiceImpl`, `ChartTemplateServiceImpl`. Read **one** fully; skim the other.

### Step 7 — Auth refresh/logout + errors (1–2 hours)

1. `RefreshTokenStore`
2. `AuthCookieSupport`
3. `GlobalExceptionHandler` + `ErrorCodes` + `messages.properties`
4. `JsonUnauthorizedEntryPoint`

**Prove:** expire access token, refresh cookie still works; logout then refresh is 401.

### Step 8 — Only then: tests as documentation

`backend/src/test/java/com/task/chart/controller/SystemOverviewDesign12*.java` are the spec. When a test name says “422 when bid_ask missing,” that is the product rule.

---

# Package-by-package (problem, why, classes, deps, what to know first)

## `com.task.chart` (root)

- **Problem:** JVM needs a `main` and Spring needs a scan root.
- **Why:** `@SpringBootApplication` scans this package and children. `@EnableScheduling` is what makes ingest run.
- **Important:** `ChartBackendApplication`
- **Depends on:** `config.AppProperties`
- **Depended on by:** nothing in-app (the OS starts it)
- **Know first:** nothing — this is step 1 after running the app

## `controller`

- **Problem:** Map URL + HTTP method + query/body to a Java method. Stay thin.
- **Why:** TradingView and the frontend speak HTTP, not Java interfaces.
- **Important:** `ChartDataController`, `AuthController`, `ChartLayoutController`, then `CurrencyPairController`, `IndicatorTemplateController`, `ChartTemplateController`
- **Depends on:** `service.*`, `dto.*`
- **Depended on by:** Spring MVC (not your code)
- **Know first:** JWT filter exists; controllers assume the user is already authenticated except auth/health
- **Trap:** Putting SQL here. The javadoc says “HTTP only; no SQL” on purpose.

## `service` / `service.impl`

- **Problem:** All rules that are not “this URL exists.”
- **Why:** Controllers stay boring; tests can `@WebMvcTest` or `@SpringBootTest` against HTTP while logic sits here.
- **Important:** `ChartDataServiceImpl`, `AuthServiceImpl`, `ChartLayoutServiceImpl`, `CurrencyPairServiceImpl`, `SymbolCatalogImpl`, `MockBarGeneratorImpl`
- **Depends on:** `repository`, `entity`, `cache`, `security`, `config.AppProperties`, `exception`, `util.ResolutionMapper`
- **Depended on by:** `controller`, `TickIngestWorker` (currency pairs)
- **Know first:** How a controller method looks; that exceptions are unchecked and mapped later

## `repository`

- **Problem:** Type-safe access to `m_*` tables.
- **Why:** Avoid handwritten SQL for simple CRUD/search.
- **Important:** `CcypairRepository.searchActive`, `TvChartLayoutRepository.findByCustomerNo…`, `AppUserRepository`, `SeasonRepository`
- **Depends on:** `entity`
- **Depended on by:** `service.impl`
- **Know first:** Entity field names; `Ccypair.ACTIVE = 0`

## `entity`

- **Problem:** Java objects that match `m_*` columns.
- **Why:** JPA needs a class per table it manages. Warehouse tables are **not** here.
- **Important:** `Ccypair`, `AppUser`, `TvChartLayout`, `Season`
- **Depends on:** JPA only
- **Depended on by:** `repository`, `service.impl`
- **Know first:** Flyway created the tables; `ddl-auto` is none

## `dto.request` / `dto.response`

- **Problem:** JSON contract. Changing a field name **breaks the widget**.
- **Why:** Never serialize JPA entities (lazy loading, extra fields, password hashes).
- **Important:** `HistoryResponse`, `LoginResponse`, `ErrorResponse`, `FxQuoteMessage`, `FormingBarMessage`, `ChartLayoutDto`
- **Depends on:** almost nothing
- **Depended on by:** controller, service, cache (`QuoteBus` publishes DTOs)
- **Know first:** Widget `datafeed.ts` / `auth.ts` field names

## `security`

- **Problem:** Who is calling, and which `customer_no` to use.
- **Why:** Docs 127–139 are tenant-scoped; datafeed is authenticated even though market data is global.
- **Important:** `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`, `CustomerContext`, `RefreshTokenStore`, `AuthCookieSupport`, `JsonUnauthorizedEntryPoint`, `ChartPrincipal`
- **Depends on:** `config.AppProperties`, `exception.ServerErrorException` (from `requireCustomerNo`)
- **Depended on by:** filter chain; `AuthServiceImpl`; layout/template services
- **Know first:** HTTP request lifecycle step 4–5 above
- **Trap:** Storing tenant only in `SecurityContext` and forgetting `CustomerContext` — services use `CustomerContext`

## `cache`

- **Problem:** Fast history + live OHLC + notifying Python.
- **Why:** Doc 121 is Redis-shaped; TradingView will call history often; ticks must not recompute all history.
- **Important:** `TickIngestWorker`, `ChartCacheStore`, `ChartBarRepository`, `QuoteBus`, `CacheNamespace`, `CachedChartBar`, `ChartCacheWriter`, `DemoTickEngine`
- **Depends on:** Redis, JDBC, `CurrencyPairService`, DTOs `FxQuoteMessage` / `FormingBarMessage`
- **Depended on by:** `ChartDataServiceImpl` (read), Python (indirect)
- **Know first:** Feature E diagram; `GET /api/history` does not publish

## `config`

- **Problem:** Bind yaml, CORS, BCrypt, seed users, Swagger.
- **Why:** Secrets and feature flags should not be hardcoded in services (JWT secret still is in yaml for demo).
- **Important:** `AppProperties`, `WebConfig`, `AppUserSeedRunner`, `PasswordConfig`
- **Depends on:** yaml, `entity.AppUser`
- **Depended on by:** almost everything that reads `app.*`
- **Know first:** How to start the app

## `exception`

- **Problem:** One JSON error shape for the frontend.
- **Why:** Widget and login overlay parse `message` / `errorCode`.
- **Important:** `GlobalExceptionHandler`, then the five `*Exception` types
- **Depends on:** `LocalizedMessageService`, `ErrorCodes`, `ErrorResponse`
- **Depended on by:** services throw; Spring calls the handler
- **Know first:** Controllers do not catch

## `constants`

- **Problem:** Magic strings for errors and price side.
- **Why:** Docs specify `CODE:30020`; do not type it in ten files.
- **Important:** `ErrorCodes`, `PriceComponent`
- **Depends on:** nothing
- **Depended on by:** handler, services, ingest
- **Know first:** HTTP error JSON

## `util`

- **Problem:** TradingView resolution strings are not milliseconds and are not Peach `chart_type`.
- **Why:** One mapping table so history, marks, layouts, and Redis keys do not drift.
- **Important:** `ResolutionMapper` only
- **Depends on:** nothing
- **Depended on by:** `ChartDataServiceImpl`, `CacheNamespace`, `ChartLayoutServiceImpl`
- **Know first:** Widget sends `"1"` for one **minute**

---

# Project mental model (explain this to a junior)

Imagine a **shop window** (the TradingView widget) and a **back office** (this Spring Boot app).

The shop window only knows how to say:

- “Give me the list of products” → `/api/search`, `/api/symbols`
- “Give me yesterday’s prices” → `/api/history`
- “Save my window dressing” → `/api/layouts`
- “Who am I?” → login, then a **visitor badge** (JWT) on every later question

The back office:

1. Checks the badge at the door (`JwtAuthenticationFilter`).
2. For prices, looks on a **fast shelf** (Redis). If the shelf is empty, it walks to the **warehouse** (`t_chart_*`).
3. A **clerk in the back** (`TickIngestWorker`) writes new prices on the shelf every third of a second and **shouts down the hall** (Redis PUBLISH). A **different building** (Python) hears the shout and updates the live ticker and the live candle. The clerk who answers `/api/history` does not shout; they only read the shelf.
4. For saved windows, the badge has a **customer number**. Drawers are labeled with that number. If you ask for someone else’s drawer, the clerk says “not found,” not “that belongs to demo2.”

Your job as maintainer:

- **Wrong historical candle** → shelf/warehouse/Java history, not Python.
- **Frozen live candle** → clerk + shout + Python + browser socket.
- **Wrong person’s layout** → badge `customer_no` and the drawer query.
- **401** → badge missing/expired (filter) or refresh cookie (auth service).
- **422** → the question was malformed (symbol, resolution, body).
- **404** → we refuse to admit the thing exists (or it really does not).

Spring Boot’s job is to **route HTTP to the right clerk** and **keep the clerk from mixing up customers**. It is not a magic chart engine. The chart engine is TradingView in the browser plus the numbers you store in Redis and Postgres.

When you are stuck, say out loud: **“Is this a request, a tick, or a WebSocket relay?”** Then open only that column of the system. That is how a senior debugs this codebase.
