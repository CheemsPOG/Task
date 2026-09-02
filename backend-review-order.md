 # CTFX chart backend — full package/file review order

Package rank = business-criticality and bug-surface, not alphabetical or dependency order. Within each package, files are ranked the same way: the one most likely to hide a real bug or teach you the most about the system sits on top.

---

## 1. `cache` — highest priority

This is the package the other docs keep warning you not to skip: it's a background scheduled job, not a request/response flow, so it's the easiest part of the app to misunderstand by only reading controllers. It owns both reads (history) and the only writer of live OHLC.

| Rank | File | Review insight |
|---|---|---|
| 1 | **`TickIngestWorker`** | The single most important class in the backend. `@Scheduled` every `app.chart-cache.tick-ms` (333ms in real config, `3600000` + scheduling off in tests). Each tick: upserts the current open bar (DB + Redis `cache_set_*`) → `SET peach:forming:*` + `PUBLISH peach:bars` → `SET peach:quote:*` + `PUBLISH peach:quotes`. This is the **only** place OHLC is computed — if you ever find yourself "fixing" candle math anywhere else (especially in Python), you're in the wrong file. Check: is a failed pair inside the loop isolated so it doesn't abort the whole tick, or does one bad pair kill the whole cycle? Not `@Transactional` — each JDBC upsert is its own statement, on purpose. `@Order(200)`, must run *after* `ChartCacheWriter`'s `@Order(100)` boot seed. |
| 2 | **`ChartCacheStore`** | The read path for `/api/history`. Cache-aside pattern: ZRANGE Redis first, warm from `t_chart_*` warehouse only on miss. Critical fact: the "last bar" it returns is whatever `TickIngestWorker` last wrote — there is **no** `stitchCurrentBar` logic reconciling ingest output with a separately-computed live candle. If history and the live socket ever disagree, this is one of the two places to check (the other is `TickIngestWorker`). Does not SUBSCRIBE to anything; it's a point-in-time read. |
| 3 | **`CacheNamespace`** | Enum mapping TradingView resolution string ↔ Peach `chart_type` ↔ table name ↔ Redis key prefix. The trap that bites every newcomer: TV `"1"` = 1 **minute**, not 1 month (`t_chart_60`, `cache_set_1m`). Javadoc explicitly says never pass a request string directly as a table name — table names must always resolve through this enum, never be interpolated from user input (SQL-injection-shaped risk even though it's JDBC not raw SQL). If you ever add a resolution, this is the single source of truth to update alongside `ResolutionMapper`. |
| 4 | **`CachedChartBar`** | Row model holding both bid *and* ask OHLC columns per bar. `toBarDto(BID/ASK/MID)` is the projection point — this is literally the same projection logic Python's `widget_bar()` duplicates, so if header/candle values look inconsistent between REST and WebSocket, diff this method against `ws-python/market.py`'s equivalent. |
| 5 | **`QuoteBus`** | Publishes `FxQuoteMessage` / `FormingBarMessage` to Redis. Channel/key names here are a **cross-process contract** — Python's `market.py` hardcodes the same channel names (`peach:quotes`, `peach:bars`) with no shared constant, no schema check. Renaming a channel here silently breaks Python with no compile-time signal. Treat any change to this file as a two-repo change even though it looks like a one-line Java edit. |
| 6 | **`ChartBarRepository`** | JDBC (not JPA) into the 13 `t_chart_*` warehouse tables — deliberately not JPA-mapped because 13 near-identical entity classes would be pure noise. `replacePair` does DELETE-then-INSERT (not upsert) — confirms Postgres and H2 test parity, but also means a partial failure mid-replace could leave a pair's warehouse rows empty until next boot. Table name always comes from `CacheNamespace`, never a raw string. |
| 7 | **`ChartCacheWriter`** | `ApplicationRunner @Order(100)` — boot-only seed. For every namespace × every catalog pair it generates a short mock series (via `MockBarGeneratorImpl`), wipes, and reinserts warehouse rows, then mirrors into Redis ZSETs. Runs once per boot; **there is no scheduled refresh** — after boot, `TickIngestWorker` owns the last bar exclusively. If someone reports "stale history after running for days," this file is not the suspect; check ingest instead. |
| 8 | **`DemoTickEngine`** | Fake market physics (random walk), explicitly called out in both docs as "not FX product rules — replace it later, don't build features inside it." Lowest review priority in this package precisely because it's meant to be thrown away when a real liquidity-provider feed replaces it. Skim only to confirm it doesn't leak business logic (e.g., don't let validation rules accidentally live here). |

---

## 2. `service` / `service.impl` — business rules

Controllers are intentionally thin; this is where almost every 422/404 decision and every product rule lives. This is the package you read to answer "what does this API actually enforce."

| Rank | File | Review insight |
|---|---|---|
| 1 | **`ChartDataServiceImpl`** | Docs 120–126 in one class: config, history, symbols, search, marks, timescale marks. Highest-value single file to review. Look for: `validateHistoryRequest` (symbol must resolve to 6 letters after stripping slashes, resolution must be in `HISTORY_RESOLUTIONS`, `from`/`to` must both be present or both absent); the `price=mid` → `bid_ask=MID` widget-quirk fallback (a hidden second input path for the same semantic value — easy to miss in a diff); and the deliberate inconsistency that unknown symbol on `/history` is **422** but unknown symbol on `/symbols` is **404** (history is stricter than resolve — don't "fix" this without reading `SystemOverviewDesign121Test` first, it's tested behavior, not a bug). |
| 2 | **`ChartLayoutServiceImpl`** | Tenant-scoped CRUD reference implementation — read this fully once, then use it as the template every other tenant-scoped service should match. The core invariant: another customer's layout returns **404**, not 403, so the API never confirms an id exists for a tenant that doesn't own it. `CustomerContext.requireCustomerNo()` missing is treated as a **500**, not a redirect-to-login — because if the filter did its job, context should always be set; a missing context here is an internal bug, not a client error. `@Transactional` on write methods, `readOnly` on reads. |
| 3 | **`AuthServiceImpl`** | Login issues the access JWT; refresh rotates the opaque UUID; logout revokes it. The one fact worth memorizing: the refresh value is **never** put in a JSON response body — only in the HttpOnly cookie and Redis. If you ever see a refresh token in a JSON payload during review, that's a security regression. `login` is `@Transactional(readOnly=true)` — it only reads `m_app_user`; the Redis refresh-store write happens outside JPA, so a Redis failure after a successful DB read isn't rolled back (there's nothing to roll back). |
| 4 | **`SymbolCatalogImpl`** | Backs doc 123 symbol resolution — accepts both `USD/JPY` and `USDJPY`. Cross-check this against `ChartDataServiceImpl.history`'s stricter symbol validation; these two code paths intentionally diverge (422 vs 404) and it's easy to "harmonize" them by accident during a refactor. |
| 5 | **`CurrencyPairServiceImpl`** | Backs `GET /curpairs` — note this endpoint is **not** under `/api` but still requires a Bearer JWT. Returns `priority` as `curpairCd`, which is also the id format Python's WebSocket messages use (as a string, not a number — another cross-process type mismatch worth checking on any change). |
| 6 | **`MockBarGeneratorImpl`** | Boot-time history generator only (`peachBarAt`). After boot, `TickIngestWorker` owns the last bar — this class never runs again until next restart. Deterministic by design (same seed → same past), which is why tests can assert exact values against it. |
| 7 | **`ChartTemplateServiceImpl`** | Same CRUD shape as indicator templates and layouts (upsert on `customer_no + name`, update touches `content` only). Docs 136–139. Read this fully — pick one of the two "template" services to go deep on. |
| 8 | **`IndicatorTemplateServiceImpl`** | Structurally identical to `ChartTemplateServiceImpl` (docs 132–135). Skim only if you already read the chart-template one fully — the two exist mainly to satisfy two different design-doc numbers, not two different behaviors. |

---

## 3. `security` — who is calling, and which tenant

Small package, disproportionate blast radius if broken (auth bypass or cross-tenant data leak).

| Rank | File | Review insight |
|---|---|---|
| 1 | **`JwtAuthenticationFilter`** | The single highest-risk file in the whole backend from a review standpoint. Parses Bearer JWT, sets `SecurityContextHolder` + `CustomerContext` on success; on a bad token it leaves the request anonymous rather than writing 401 itself (401 is decided later by the authorization matcher). **The critical invariant:** context must be cleared in a `finally` block. If that's ever removed or bypassed by an early return, a pooled Tomcat thread can carry the previous request's `customer_no` into the next unrelated request — a real cross-tenant data leak, not a theoretical one. Any PR touching this file deserves extra scrutiny on control flow around early returns/exceptions. |
| 2 | **`CustomerContext`** | Request-scoped `ThreadLocal` tenant id — explicitly **not** an HTTP session. Everything that enforces tenant isolation (`ChartLayoutServiceImpl` and its template siblings) calls `requireCustomerNo()` on this, not on `SecurityContextHolder` directly. If a new tenant-scoped table gets added and the developer filters by `SecurityContextHolder` principal instead of this class, that's a code-review catch, not a compile error. |
| 3 | **`JwtService`** | Issues/parses the HS256 access JWT. Claims: `sub` = username, `customer_no` = long, 1h expiry. Secret comes from `app.jwt.secret` in `application.yml` — flagged everywhere as demo-only; rotating it logs out every active session with no graceful migration path. |
| 4 | **`AuthCookieSupport`** | Sets/clears the HttpOnly `chart_refresh_token` cookie (`Path=/`, `SameSite=Lax`). Pure cookie mechanics — check this if refresh ever silently fails in a browser but works in curl (cookie attributes are usually the culprit, e.g. domain/path mismatch through the Vite proxy). |
| 5 | **`RefreshTokenStore`** | Redis-backed opaque UUID store, key `peach:auth:refresh:{uuid}`, 24h TTL. Rotation on every refresh call (old UUID invalidated, new one issued) — this is what makes `structure.md §6.1`'s "refresh after logout must fail" test meaningful; confirm rotation actually happens rather than reusing the same key. |
| 6 | **`SecurityConfig`** | Defines the filter chain: CSRF off (stateless JWT API, no forms), `SessionCreationPolicy.STATELESS`, and the public-vs-protected path matcher (`/api/health`, `/api/auth/**`, Swagger, OPTIONS are public; everything else under `/api/**` plus `/curpairs` requires auth). Good file to read *first* in this package for context even though the real bug surface is in the filter above it. |
| 7 | **`JsonUnauthorizedEntryPoint`** | Handles the filter-chain 401 (missing/invalid Bearer on a protected path) — deliberately **separate** from `GlobalExceptionHandler`. Two 401 code paths exist on purpose: this one for "never got past the gate," `UnauthorizedAppException` (thrown by `AuthServiceImpl`) for "got past the gate, refresh cookie was bad." Don't try to collapse these into one handler without understanding why they're split. |
| 8 | **`ChartPrincipal`** | Thin holder (username + customerNo) placed into `SecurityContextHolder`. Low review priority — it's a data carrier, not logic. |

---

## 4. `controller` — HTTP mapping only

Should be boring by design; the docs' own house rule is "HTTP only, no SQL." Review priority here is really about catching violations of that rule.

| Rank | File | Review insight |
|---|---|---|
| 1 | **`ChartDataController`** | Widest surface area — docs 120–126 all land here (`/config`, `/history`, `/time`, `/symbols`, `/search`, `/marks`, `/timescale_marks`) plus the public `/api/health`. Confirm it does query-param binding only and delegates everything to `ChartDataServiceImpl` — any inline validation or Redis/JDBC call here is a violation of the layering rule and worth flagging. |
| 2 | **`AuthController`** | Thin wrapper over `AuthServiceImpl` for login/refresh/logout. Worth confirming `/refresh` and `/logout` really are marked public (no `@PreAuthorize`/Bearer requirement) since they rely on the cookie, not the header. |
| 3 | **`ChartLayoutController`** | Full CRUD verb set (127–131). Check each verb maps to the right expected status (POST→201 with `{id}`, DELETE→`{t: now}`, etc.) — these exact shapes are what `SystemOverviewDesign127Test`–`131Test` assert. |
| 4 | **`CurrencyPairController`** | Single endpoint (`GET /curpairs`), notable mainly for living **outside** `/api` while still requiring Bearer — an easy thing for a new controller to get wrong if someone copies this pattern without understanding why it's special-cased in `SecurityConfig`. |
| 5 | **`IndicatorTemplateController`** | Same verb shape as chart templates (132–135). Read one of the two template controllers fully. |
| 6 | **`ChartTemplateController`** | Structurally identical to the indicator-template controller (136–139) — MD table name is plural (`m_tv_chart_templates`), REST noun is `/api/chart-templates`. Skim if you already reviewed the indicator one. |

---

## 5. `exception` — the error contract

Small but load-bearing: this is the only place that decides what JSON shape the frontend and Swagger consumers actually see on failure.

| Rank | File | Review insight |
|---|---|---|
| 1 | **`GlobalExceptionHandler`** | `@RestControllerAdvice` mapping typed exceptions → `{errorCode, message}` + HTTP status. Controllers never `try/catch` business exceptions — if you see one that does, that's a design violation worth flagging in review. Also catches framework-level failures (`MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`) and normalizes them all to 422 `CODE:30020`. Note it does **not** handle the filter-chain 401 (`JsonUnauthorizedEntryPoint` does that separately, before `DispatcherServlet` even routes to a controller). |
| 2 | **`ValidationException` / `ResourceNotFoundException` / `BadCredentialsAppException` / `UnauthorizedAppException` / `ServerErrorException`** | The five typed exceptions services throw. Worth memorizing the status/errorCode/thrower table: `ValidationException`→422/`CODE:30020` (services, most common), `ResourceNotFoundException`→404/`CODE:30404`, `BadCredentialsAppException`→401/`E_BAD_CREDENTIALS` (wrong password only), `UnauthorizedAppException`→401/`E_UNAUTHORIZED` (bad/missing refresh cookie), `ServerErrorException`→500/`E_SERVER` (missing `CustomerContext`, missing `m_season` row — i.e. things that should never happen if the rest of the app is correct). `ValidationException` carries **no message** by design — the handler always sends the same localized text, so you can't currently attach a specific reason ("resolution 10 invalid for marks") without changing this design. |
| 3 | **`LocalizedMessageService`** | Resolves `message` from `messages.properties` / `messages_ja.properties` via `Accept-Language`. Check any new error path actually has both English and Japanese keys — an easy thing to forget when adding a new validation rule. |
| 4 | **`ErrorResponse`** | Simple DTO (`errorCode`, `message`). Low review priority — shape, not logic. |

---

## 6. `dto.request` / `dto.response` — the JSON contract

Nothing here computes anything, but changing a field name here breaks the widget with no compiler warning on the frontend side (it's TypeScript, a separate build).

| Rank | File | Review insight |
|---|---|---|
| 1 | **`HistoryResponse`** | Dual-shaped on purpose: Peach columnar arrays (`s, t[], o[], h[], l[], c[]`, unix **seconds**) *and* widget `bars[]` (unix **milliseconds**). Any review of a history-related PR should double check which time unit a given field uses — this is the single most common off-by-1000 bug class in this codebase. |
| 2 | **`LoginResponse`** | `{accessToken, tokenType: "Bearer", expiresIn: 3600, refreshExpiresIn: 86400}` — confirm the refresh **value** never appears here (only its expiry, as a number). |
| 3 | **`FxQuoteMessage` / `FormingBarMessage`** | These are the wire contract with Python — `QuoteBus` publishes them, `market.py` on the Python side independently defines matching field names with no shared schema. Renaming a field here is effectively a breaking change to a different repo/language with no build-time signal. |
| 4 | **`ChartLayoutDto`** | Read alongside `ChartLayoutServiceImpl` — confirms `chart_type` (DB column, historical Peach name) is exposed as `resolution` in JSON (`1D`/`60`, not `DAY`/`60M`). |
| 5 | **Request DTOs** (`UpsertChartTemplateRequest`, `LoginRequest`, etc.) | `LoginRequest` is notably a plain record **without** `@NotBlank` — blank username/password is validated manually in `AuthServiceImpl`, not via Jakarta Validation. Worth knowing so you don't go looking for bean-validation annotations that aren't there. |

---

## 7. `repository` — JPA access for masters

Thin by design; Spring generates SQL from method names, so there's rarely custom logic to review, just correctness of the generated query against intent.

| Rank | File | Review insight |
|---|---|---|
| 1 | **`TvChartLayoutRepository`** | `findByCustomerNo…` queries back the tenant-scoping invariant from `ChartLayoutServiceImpl`. Confirm every query method that returns layout data is scoped by `customer_no` — an unscoped finder added here is a data-leak risk regardless of what the service layer does. |
| 2 | **`CcypairRepository`** | `searchActive` — active-pair filtering (`is_deleted = 0`) backs docs 123/124/127. Check soft-delete filtering is applied consistently rather than reimplemented ad hoc elsewhere. |
| 3 | **`AppUserRepository`** | Backs login lookup by username. Simple, low risk, but confirm password comparison happens via BCrypt in the service layer, not here. |
| 4 | **`SeasonRepository`** | Backs the session-string lookup for doc 123. Note the demo only seeds one winter row covering 2020–2099 — if that ever expires or a caller queries outside that window, expect a **500**, not a graceful fallback. |
| 5 | **`TvIndicatorTemplateRepository` / `TvChartTemplateRepository`** | Same upsert-by-`(customer_no, name)` shape. Review one, skim the other. |
| 6 | **`TvMarkRepository` / `TvTimescaleMarkRepository`** | Global (no `customer_no` — marks are shared demo seeds, not tenant data), filtered by a fixed seed window (`MarkSeedWindow`). Lowest risk in this package since there's no tenant isolation to get wrong. |

---

## 8. `entity` — JPA rows

Pure data classes. Review priority here is almost entirely about confirming these are **never** returned directly as API JSON (that's what the `dto` package exists to prevent) and that they match the Flyway schema exactly.

| Rank | File | Review insight |
|---|---|---|
| 1 | **`TvChartLayout`** | Most-touched entity in practice (layouts are the feature users interact with most). Confirm column mapping matches V5 exactly, especially the `chart_type` ↔ `resolution` naming mismatch noted above. |
| 2 | **`Ccypair`** | Root reference data almost everything else joins against *logically* (there are no real foreign keys in this schema — relationships are enforced in Java, not SQL). `ACTIVE = 0` constant worth knowing. |
| 3 | **`AppUser`** | Demo-only, not part of design docs 120–139. BCrypt hash lives here — confirm it's never serialized outward. |
| 4 | **`Season`, `TvMark`, `TvTimescaleMark`, `TvIndicatorTemplate`, `TvChartTemplate`** | Straightforward column mirrors of their respective Flyway migrations. Skim as a batch — no entity in this tier has unusual logic. |

---

## 9. `util` — small, high-leverage

Tiny package, but the one file in it is a single point of truth other packages depend on.

| Rank | File | Review insight |
|---|---|---|
| 1 | **`ResolutionMapper`** | `periodMillis` / `toPeachChartType` / `isHistoryResolution` / `isMarksResolution`. This is the file that keeps history, marks, layouts, and Redis keys from drifting apart on resolution strings. Any new resolution must be added here **and** in `CacheNamespace` **and** in the Flyway `t_chart_*` tables — a change in only one place is the classic bug this package exists to prevent. |
| 2 | **`DemoMarket`** | Minor demo helper; low review priority. |

---

## 10. `constants` — magic strings, centralized

| Rank | File | Review insight |
|---|---|---|
| 1 | **`ErrorCodes`** | `CODE:30020` (validation), `CODE:30404` (not found), etc. — confirm nothing outside this file hardcodes these strings. |
| 2 | **`PriceComponent`** | `fromBidAsk("BID"/"ASK"/"MID")` parsing, throws `IllegalArgumentException` on bad input (caught and rewrapped as `ValidationException` upstream). |
| 3 | **`MarkSeedWindow`** | Fixed unix-second window (`1787011200`–`1787270400`) the demo mark seed data falls into. Purely a demo-data constant, lowest risk. |

---

## 11. `config` — plumbing, rarely business logic

| Rank | File | Review insight |
|---|---|---|
| 1 | **`AppProperties`** | Binds `app.*` from `application.yml` (JWT secret/expirations, chart-cache tick interval, TradingView flags). Read this alongside `application.yml` itself — almost every "why does the demo behave this way" question traces back here. |
| 2 | **`AppUserSeedRunner`** | Seeds `demo`/`demo2` on boot if missing. Demo-only, not part of 120–139. |
| 3 | **`WebConfig`** | CORS origins (`localhost`/`127.0.0.1` on 5173/3000). Check this if a direct-to-Java request (bypassing Vite) fails with a CORS error that curl doesn't reproduce. |
| 4 | **`PasswordConfig`** | Just the BCrypt bean. Framework plumbing, essentially zero logic to review. |

---

## 12. `com.task.chart` (root) — lowest priority

| Rank | File | Review insight |
|---|---|---|
| 1 | **`ChartBackendApplication`** | `@SpringBootApplication` + `@EnableConfigurationProperties(AppProperties.class)` + `@EnableScheduling` — that last annotation is the only reason `TickIngestWorker`'s `@Scheduled` method ever fires. One-line file, one-line insight: if scheduling ever silently stops working app-wide, this is the first thing to check. Nothing else to review here; the OS starts it, nothing in-app depends on it. |

---

## How to use this while reviewing

For each file, ask the three questions from the earlier senior-review guidance before you consider it "reviewed":
1. Does its class javadoc have a **NOT list** (what it deliberately doesn't do)? If missing on a file that clearly needs one (e.g. anything in `cache` or `security`), that's a documentation gap worth raising.
2. Is any non-obvious HTTP status code actually a **security or product decision** rather than a bug? (See the 404-vs-403, 422-vs-404 tables above.)
3. Does every `//` comment explain **why**, not **what**? If a comment just restates the method name, it's noise; if it explains an invariant (boot order, thread-local lifecycle, cross-process contract), it's doing its job.
