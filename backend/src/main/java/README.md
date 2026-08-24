# Java source map — `com.task.chart`

Browse this file next to the packages. Layering is CTFX-style: **controller → service → repository/cache**. HTTP never returns JPA entities.

Root: `backend/src/main/java/com/task/chart/`

```text
com/task/chart/
├── ChartBackendApplication.java      Spring Boot entry (@EnableScheduling)
├── package-info.java                 this package
│
├── controller/                       HTTP only (no SQL, no JWT parse)
│   ├── AuthController.java           POST /api/auth/login
│   ├── ChartDataController.java      /api/health + datafeed 120–126
│   ├── ChartLayoutController.java    /api/layouts 127–131
│   ├── ChartTemplateController.java  /api/chart-templates 136–139
│   ├── CurrencyPairController.java   GET /curpairs (public)
│   └── IndicatorTemplateController.java  /api/indicator-templates 132–135
│
├── service/                          interfaces (verb methods)
│   ├── AuthService.java
│   ├── ChartDataService.java
│   ├── ChartLayoutService.java
│   ├── ChartTemplateService.java
│   ├── CurrencyPairService.java
│   ├── IndicatorTemplateService.java
│   ├── LocalizedMessageService.java
│   ├── MockBarGenerator.java         demo OHLC factory
│   ├── MockFxQuoteService.java       in-process quote walk (history stitch)
│   ├── SymbolCatalog.java            in-memory TV symbols
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── ChartDataServiceImpl.java
│       ├── ChartLayoutServiceImpl.java
│       ├── ChartTemplateServiceImpl.java
│       ├── CurrencyPairServiceImpl.java
│       ├── IndicatorTemplateServiceImpl.java
│       ├── LocalizedMessageServiceImpl.java
│       ├── MockBarGeneratorImpl.java
│       ├── MockFxQuoteServiceImpl.java
│       └── SymbolCatalogImpl.java
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
│       ├── FxQuoteMessage.java
│       ├── HealthResponse.java
│       ├── HistoryResponse.java
│       ├── IndicatorTemplateDto.java
│       ├── IndicatorTemplateListItemDto.java
│       ├── LoginResponse.java
│       ├── MarkDto.java
│       ├── SearchSymbolDto.java
│       ├── ServerTimeResponse.java
│       ├── SymbolInfoDto.java
│       ├── SystemDatetimeResponse.java
│       └── TimescaleMarkDto.java
│
├── security/                         JWT stand-in for S-01
│   ├── ChartPrincipal.java           username + customer_no
│   ├── CustomerContext.java          ThreadLocal tenant for this request
│   ├── JsonUnauthorizedEntryPoint.java   401 JSON
│   ├── JwtAuthenticationFilter.java  Bearer → SecurityContext + CustomerContext
│   ├── JwtService.java               HS256 create / parse
│   └── SecurityConfig.java           filter chain, public matchers
│
├── config/                           Spring beans that are not the JWT filter
│   ├── AppProperties.java            app.jwt / app.tradingview / CORS
│   ├── AppUserSeedRunner.java        demo / demo2 if missing
│   ├── OpenApiConfig.java            Swagger tags + Bearer scheme
│   ├── PasswordConfig.java           BCrypt encoder
│   └── WebConfig.java                CORS
│
├── cache/                            design doc 121 warehouse + Redis
│   ├── CacheNamespace.java           TV resolution → t_chart_* → cache_set_*
│   ├── CachedChartBar.java           bid_/ask_ OHLC row
│   ├── ChartBarRepository.java       JdbcTemplate (table name from enum only)
│   ├── ChartCacheStore.java          Redis ZSET hot cache
│   └── ChartCacheWriter.java         boot seed + scheduled open-bar refresh
│
├── exception/
│   ├── BadCredentialsAppException.java
│   ├── GlobalExceptionHandler.java   CODE:30020 / 30404 / E_* JSON
│   ├── ResourceNotFoundException.java
│   ├── ServerErrorException.java
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

Tests live under `backend/src/test/java/com/task/chart/` (`controller/`, `service/`, `support/`, `FlywayMigrationTest`).

Onboarding narrative (how to run, JWT, Flyway V1–V9): repo root [`structure.md`](../../../structure.md).
