# Java source map — `com.task.chart`

**Audience:** a mentor who has not opened this backend before.

This file is the **class-level map**. Runtime story (ports, JWT, ingest SSOT, how to start): repo root [`structure.md`](../../../structure.md). Spoken walkthrough of every API: [`present.md`](../../../present.md).

**How to read a request:** HTTP arrives at a `controller` → business rules in `service.impl` → data in `repository` (JPA masters) or `cache` (bars + live quotes). Controllers never run SQL. HTTP never returns a JPA `entity` — only `dto.request` / `dto.response`.

---

## Repo root (where this package sits)

The Java tree below is only `backend/src/main/java/com/task/chart/`. The rest of the product:

```
Task/
  README.md                 how to run
  present.md                mentor script
  structure.md              onboarding
  checklist.md / test.md
  docker-compose.yml        postgres + redis
  System_Overview_Design/   specs 120–139
  backend/                  this Spring Boot app (:8080)
    src/main/java/com/task/chart/   ← everything listed in “File tree”
    src/main/resources/application.yml
    src/main/resources/db/migration/V1–V9
    src/test/java/com/task/chart/   SystemOverviewDesign120Test … 139Test
  frontend/                 Vite + TradingView (:5173)
    src/datafeed/datafeed.ts        widget calls 120–126 here
  ws-python/                Redis → WebSocket gateway (:8081)
```

---

## If they ask “where is doc 120 / the datafeed?”

**Datafeed (120–126) is not scattered.** One controller, one service. The widget client is TypeScript, not Java.

| Doc | HTTP | `ChartDataController` method | `ChartDataServiceImpl` method | Then |
|-----|------|------------------------------|-------------------------------|------|
| — | `GET /api/health` | `health()` | — (inline DTO) | public |
| **120** | `GET /api/config` | `config()` | `config()` | `app.tradingview` yml → `DatafeedConfigResponse` |
| **121** | `GET /api/history` | `history(...)` | `history(...)` | `ChartCacheStore.query` (Redis); last bar from ingest |
| **122** | `GET /api/time` | `time()` | `serverTimeSeconds()` | `ServerTimeResponse` |
| **123** | `GET /api/symbols` | `symbols(...)` | `resolve(...)` | `SymbolCatalog` + `m_ccypairs` / `m_season` |
| **124** | `GET /api/search` | `search(...)` | `search(...)` | `CcypairRepository` |
| **125** | `GET /api/marks` | `marks(...)` | `marks(...)` | `TvMarkRepository` |
| **126** | `GET /api/timescale_marks` | `timescaleMarks(...)` | `timescaleMarks(...)` | `TvTimescaleMarkRepository` |

**Widget:** `frontend/src/datafeed/datafeed.ts` (`onReady` → 120, `getBars` → 121, `getServerTime` → 122, `resolveSymbol` → 123, `searchSymbols` → 124, `getMarks` → 125, `getTimescaleMarks` → 126). Live bars: `frontend/src/datafeed/streaming.ts` → Python, **not** `ChartDataController`.

**Other docs:**

| Docs | Controller | Service impl |
|------|------------|----------------|
| 127–131 | `ChartLayoutController` `/api/layouts` | `ChartLayoutServiceImpl` |
| 132–135 | `IndicatorTemplateController` `/api/indicator-templates` | `IndicatorTemplateServiceImpl` |
| 136–139 | `ChartTemplateController` `/api/chart-templates` | `ChartTemplateServiceImpl` |
| Auth | `AuthController` `/api/auth/*` | `AuthServiceImpl` |
| `/curpairs` | `CurrencyPairController` | `CurrencyPairServiceImpl` |

Live prices: `TickIngestWorker.tick()` — not in `ChartDataServiceImpl`.

---

Root of Java sources: `backend/src/main/java/com/task/chart/`

---

## Layering (CTFX)

| Layer | Package | Allowed to |
|-------|---------|------------|
| HTTP | `controller` | Parse query/body, call one service, return DTO |
| Rules | `service` + `service.impl` | Validation, tenant (`customer_no`), map entity → DTO |
| Masters | `repository` + `entity` | JPA for `m_*` tables |
| Bars + ticks | `cache` | JDBC warehouse, Redis `cache_set_*`, quote bus, ingest |
| Auth | `security` | JWT, refresh cookie, filter chain |
| Cross-cut | `exception`, `constants`, `config`, `util` | Errors, codes, yml binding, helpers |

---

## What each package is for

| Package | Mentor one-liner |
|---------|------------------|
| `controller` | Thin REST. Datafeed 120–126, layouts 127–131, study templates 132–135, chart templates 136–139, login, `/curpairs`. |
| `service` / `impl` | Interfaces named like the work (`history`, `listLayouts`). Impl holds rules. `MockBarGenerator` = **boot seed only** (past candles). Live prices are **not** here. |
| `repository` | Spring Data JPA. Layouts/templates filter by `customer_no` from the JWT. Marks have **no** customer column (global demo seed). |
| `entity` | One class per `m_*` table. Never JSON. |
| `dto` | Wire JSON. `FxQuoteMessage` is the Redis/WS tick shape (`curpairCd` as **string**). |
| `security` | Local S-01 stand-in: 1h access JWT + 1d opaque refresh in Redis + HttpOnly cookie. Not Peach SSO. |
| `config` | CORS, BCrypt, OpenAPI tags, `AppProperties`, seed users `demo` / `demo2`. |
| `cache` | Doc **121** warehouse + Redis ZSETs **and** runtime SSOT: `TickIngestWorker` → `QuoteBus`. |
| `exception` | `CODE:30020` / `CODE:30404` / `E_UNAUTHORIZED` / `E_BAD_CREDENTIALS` / `E_SERVER`. |
| `constants` | Error codes, mark unix window, `PriceComponent` BID/ASK/MID. |
| `util` | TV resolution → period ms; demo seed BID/spread. |

---

## Design docs → first file to open

Same as the table above. Start at the **controller method**, then the **service method**. For 121 also open `cache/ChartCacheStore.java` (read) and `cache/TickIngestWorker.java` (write last bar).

---

## Cache package (doc 121 + live SSOT)

Do not confuse **boot seed** with **runtime prices**:

| Class | Role |
|-------|------|
| `CacheNamespace` | TV resolution → Peach table name + Redis key prefix (enum only — never a request string as a table). |
| `CachedChartBar` | One bar: `bid_*` and `ask_*` OHLC. MID is averaged at read time. `openFromTick` / `applyTick` used by ingest. |
| `ChartBarRepository` | `JdbcTemplate` INSERT/DELETE/upsert into `t_chart_*`. |
| `ChartCacheStore` | Redis ZSET hot cache. `GET /api/history` reads here first. |
| `ChartCacheWriter` | `@Order(100)` boot: `MockBarGenerator` fills warehouse + Redis. **No** scheduled `peachBarAt` refresh. |
| `DemoTickEngine` | Mock LP: Gaussian BID walk, ASK = BID + spread, MID = (BID+ASK)/2. |
| `QuoteBus` | `SET peach:quote:{cd}` + `PUBLISH peach:quotes`. |
| `TickIngestWorker` | `@Order(200)` + `@Scheduled` ~333ms. **Runtime SSOT:** tick → quote bus → upsert current open bar on every namespace. |

Python (`ws-python/`) only `SUBSCRIBE peach:quotes`. It does not generate prices. A real Peach feed would replace `DemoTickEngine.stepAll()` inside `TickIngestWorker` — Redis keys and WS stay.

---

## File tree

```text
com/task/chart/
├── ChartBackendApplication.java      Spring Boot entry (@EnableScheduling for ingest)
├── package-info.java                 this package
│
├── controller/                       HTTP only (no SQL, no JWT parse)
│   ├── AuthController.java           extra vs 120–139: login / refresh / logout
│   ├── ChartDataController.java      ALL datafeed 120–126 + health (see method table above)
│   ├── ChartLayoutController.java    127 POST, 128 PUT, 129 GET id, 130 GET list, 131 DELETE
│   ├── ChartTemplateController.java  136 GET list, 137 POST, 138 GET name, 139 DELETE
│   ├── CurrencyPairController.java   extra: GET /curpairs (JWT, m_ccypairs)
│   └── IndicatorTemplateController.java  132 GET list, 133 POST, 134 GET name, 135 DELETE
│
├── service/                          interfaces (verb methods)
│   ├── AuthService.java
│   ├── ChartDataService.java
│   ├── ChartLayoutService.java
│   ├── ChartTemplateService.java
│   ├── CurrencyPairService.java
│   ├── IndicatorTemplateService.java
│   ├── LocalizedMessageService.java
│   ├── MockBarGenerator.java         demo OHLC factory (boot seed only)
│   ├── SymbolCatalog.java            in-memory TV symbols from m_ccypairs
│   └── impl/
│       ├── AuthServiceImpl.java          BCrypt check, issue JWT + Redis refresh
│       ├── ChartDataServiceImpl.java     120 config, 121 history (Redis read, no stitch),
│       │                                 122 time, 123 resolve, 124 search, 125/126 marks
│       ├── ChartLayoutServiceImpl.java   127–131 tenant CRUD
│       ├── ChartTemplateServiceImpl.java 136–139 upsert/list/get/delete
│       ├── CurrencyPairServiceImpl.java  GET /curpairs from m_ccypairs.priority
│       ├── IndicatorTemplateServiceImpl.java  132–135
│       ├── LocalizedMessageServiceImpl.java   error message EN/JA
│       ├── MockBarGeneratorImpl.java     deterministic past OHLC for boot seed
│       └── SymbolCatalogImpl.java        in-memory TV symbols from m_ccypairs
│
├── repository/                       Spring Data JPA (masters)
│   ├── AppUserRepository.java
│   ├── CcypairRepository.java
│   ├── SeasonRepository.java
│   ├── TvChartLayoutRepository.java
│   ├── TvChartTemplateRepository.java
│   ├── TvIndicatorTemplateRepository.java
│   ├── TvMarkRepository.java
│   └── TvTimescaleMarkRepository.java
│
├── entity/                           JPA rows (never API JSON)
│   ├── AppUser.java                  m_app_user
│   ├── Ccypair.java                  m_ccypairs
│   ├── Season.java                   m_season
│   ├── TvChartLayout.java            m_tv_chart_layout
│   ├── TvChartTemplate.java          m_tv_chart_templates
│   ├── TvIndicatorTemplate.java      m_tv_indicator_template
│   ├── TvMark.java                   m_tv_mark
│   └── TvTimescaleMark.java          m_tv_timescale_mark
│
├── dto/
│   ├── request/                      JSON in
│   │   ├── LoginRequest.java
│   │   ├── RegisterChartLayoutRequest.java
│   │   ├── UpsertChartTemplateRequest.java
│   │   └── UpsertIndicatorTemplateRequest.java
│   └── response/                     JSON out
│       ├── BarDto.java
│       ├── ChartLayoutDto.java
│       ├── ChartLayoutIdResponse.java
│       ├── ChartLayoutListItemDto.java
│       ├── ChartTemplateDto.java
│       ├── ChartTemplateListItemDto.java
│       ├── CurrencyPairDto.java
│       ├── DatafeedConfigResponse.java
│       ├── ErrorResponse.java
│       ├── FxQuoteMessage.java         Redis + WS tick JSON
│       ├── HealthResponse.java
│       ├── HistoryResponse.java
│       ├── IndicatorTemplateDto.java
│       ├── IndicatorTemplateListItemDto.java
│       ├── LoginResponse.java
│       ├── RefreshResponse.java
│       ├── MarkDto.java
│       ├── SearchSymbolDto.java
│       ├── ServerTimeResponse.java
│       ├── SymbolInfoDto.java
│       ├── SystemDatetimeResponse.java
│       └── TimescaleMarkDto.java
│
├── security/                         JWT stand-in for S-01
│   ├── AuthCookieSupport.java        HttpOnly refresh cookie
│   ├── ChartPrincipal.java           username + customer_no
│   ├── CustomerContext.java          ThreadLocal tenant for this request
│   ├── JsonUnauthorizedEntryPoint.java   401 JSON
│   ├── JwtAuthenticationFilter.java  Bearer → SecurityContext + CustomerContext
│   ├── JwtService.java               HS256 access token create / parse
│   ├── RefreshTokenSession.java      username + customer_no in Redis
│   ├── RefreshTokenStore.java        opaque refresh ids in Redis
│   └── SecurityConfig.java           filter chain, public matchers
│
├── config/                           Spring beans that are not the JWT filter
│   ├── AppProperties.java            app.jwt / app.tradingview / CORS
│   ├── AppUserSeedRunner.java        demo / demo2 if missing
│   ├── OpenApiConfig.java            Swagger tags + Bearer scheme
│   ├── PasswordConfig.java           BCrypt encoder
│   └── WebConfig.java                CORS
│
├── cache/                            design doc 121 warehouse + Redis + ingest
│   ├── CacheNamespace.java           TV resolution → t_chart_* → cache_set_*
│   ├── CachedChartBar.java           bid_/ask_ OHLC row
│   ├── ChartBarRepository.java       JdbcTemplate (table name from enum only)
│   ├── ChartCacheStore.java          Redis ZSET hot cache
│   ├── ChartCacheWriter.java         boot seed only (MockBarGenerator)
│   ├── DemoTickEngine.java           mock Peach-feed stand-in (BID walk)
│   ├── QuoteBus.java                 SET peach:quote:* + PUBLISH peach:quotes
│   └── TickIngestWorker.java         live SSOT: ticks → quotes + open bars
│
├── exception/
│   ├── BadCredentialsAppException.java
│   ├── GlobalExceptionHandler.java   CODE:30020 / 30404 / E_* JSON
│   ├── ResourceNotFoundException.java
│   ├── ServerErrorException.java
│   ├── UnauthorizedAppException.java
│   └── ValidationException.java
│
├── constants/
│   ├── ErrorCodes.java
│   ├── MarkSeedWindow.java           125/126 seed from/to unix
│   └── PriceComponent.java           BID / ASK / MID
│
└── util/
    ├── DemoMarket.java               seed prices, spread, scale
    └── ResolutionMapper.java         TV resolution → period ms / Peach type
```

Tests live under `backend/src/test/java/com/task/chart/` (`controller/` = `SystemOverviewDesign120Test`–`139Test`, `cache/TickIngestWorkerTest`, `service/`, `support/`, `FlywayMigrationTest`).
