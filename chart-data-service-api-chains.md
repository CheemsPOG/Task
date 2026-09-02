# Backend call chains — deep review

Code-level trees for the files that hide product/security decisions. Format: ASCII
call tree, then bullets on **why** (status codes, boot order, cross-process
contracts). Pair with `backend-review-order.md`. A leftover **file-level** list is
at the bottom.

**Already mapped (docs 120–139 services):** `ChartDataServiceImpl`,
`ChartLayoutServiceImpl`, `ChartTemplateServiceImpl`.

**Added below (cache, security, remaining services, handler):**
`TickIngestWorker`, `ChartCacheStore`, `CacheNamespace` + `ResolutionMapper`,
`QuoteBus` + `CachedChartBar`, `ChartBarRepository`, `ChartCacheWriter`,
`JwtAuthenticationFilter`, `RefreshTokenStore`, `AuthServiceImpl`,
`SymbolCatalogImpl`, `CurrencyPairServiceImpl`, `IndicatorTemplateServiceImpl`,
`GlobalExceptionHandler`.

---

# ChartDataServiceImpl — API Call Chains

## DOC 120 — CONFIG

```
config()
└── appProperties.getTradingView()
```

- **config()** — pure passthrough of `app.tradingview` flags into the UDF `onReady`
  payload. No repository/cache access, so this call can't 404/422/500 on its own.
  `supports_group_request` is hardcoded `false` in the return value (not read off
  `tradingView`) — this demo has no group-resolve endpoint, so flipping that flag
  would make the widget call something that doesn't exist.
- **appProperties.getTradingView()** — single source of truth for search/marks/
  resolutions/exchange/type flags. Only one exchange and one symbol type are ever
  returned, even though the DTO shape is a list — reflects a single-exchange demo,
  not a multi-exchange catalog.

---

## DOC 121 — HISTORY

```
history()
├── PriceComponent.from()
├── validateHistoryRequest()
│   ├── normalizeSymbolCd()
│   ├── ResolutionMapper.isHistoryResolution()
│   └── PriceComponent.fromBidAsk()
└── private history()
    ├── symbolCatalog.find()
    ├── CacheNamespace.fromTvResolution()
    ├── chartCacheStore.query()
    ├── chartCacheStore.nextTimeBefore()
    └── CachedChartBar.toBarDto()
```

- **history()** (public) — reads Redis only, never computes OHLC. Unknown CD ends up
  as **422**, not 404 — deliberately asymmetric with `/symbols`' 404 for the same
  kind of miss, locked in by `SystemOverviewDesign121Test`. Don't harmonize the two.
- **PriceComponent.from()** — only invoked when `bidAsk` is blank and `price` is
  present. This is a widget quirk (`price=mid`) treated as a second input path for
  the same BID/ASK/MID semantic; this app's own datafeed always sends `bid_ask`, so
  this branch exists purely for compatibility, not as a primary contract.
- **validateHistoryRequest()** — gate before any repository/cache work. Missing both
  `bid_ask` and `price` is a hard 422, never a silent default to MID.
  - **normalizeSymbolCd()** — letters-only strip (`FX:USD/JPY` → `FXUSDJPY`, length 8
    → 422). This is a *different* normalizer from the one `resolve()` uses
    (`normalizeCcypairCd()`, which strips `FX:` first) — swapping the two between
    endpoints is the easiest way to introduce a regression.
  - **ResolutionMapper.isHistoryResolution()** — its own allowlist, distinct from the
    marks allowlist; notably `resolution=10` **is** valid here but not for
    marks/timescale-marks.
  - **PriceComponent.fromBidAsk()** — also called again below in the public method
    after validation passes; validation here is just to fail fast with 422 on a bad
    enum value before any Redis call is made.
- **private history()** (overload) — the actual Redis read + BID/ASK/MID projection.
  - **symbolCatalog.find()** — null result → 422 here (contrast with `resolve()`,
    where an unknown-but-well-formed pair is 404). This is the concrete place the
    404-vs-422 asymmetry is enforced.
  - **CacheNamespace.fromTvResolution()** — maps resolution to the Redis cache
    namespace; null → 422. Table/key names always come from this enum, never from the
    request string.
  - **chartCacheStore.query()** — Redis ZSET read for the namespace/symbol/range.
    `countBack` trimming afterward only trims *within* whatever this call already
    returned — it's not a warehouse-wide "last N ever" query.
  - **`to` clamp** — `private history()` does `Math.min(queryTo, nowSec)` *before*
    the Redis call so the widget cannot request future bars. That clamp lives here,
    not inside `fromTvResolution()`.
  - **chartCacheStore.nextTimeBefore()** — only called when the query result is empty
    and `from` was supplied; lets the widget jump back to the previous stored bar
    instead of getting a dead end.
  - **CachedChartBar.toBarDto()** — final BID/ASK/MID projection per cached row into
    the response DTO; empty results skip this and return Peach's `no_data` (200), not
    a 404.

---

## DOC 122 — TIME

```
serverTimeSeconds()
└── Instant.now().getEpochSecond()
```

- **serverTimeSeconds()** — trivial, but the unit is the whole point: **unix
  seconds**, matching Peach's `t[]` bar timestamps, not millis. A caller that treats
  this as millis (e.g. multiplying by 1000 again) will desync the widget's clock.
- **Instant.now().getEpochSecond()** — no config or DB dependency; this endpoint
  can't meaningfully fail.

---

## DOC 123 — SYMBOL RESOLVE

```
resolve()
├── requireCcypairCd()
│   └── normalizeCcypairCd()
├── ccypairRepository.findByCcypairCdAndIsDeleted()
├── currentSession()
│   └── seasonRepository.findBySeasonCdInAnd...
└── toSymbolInfo()
    ├── displayTicker()
    └── priceScale()
```

- **resolve()** — unknown active pair is **404**, the mirror-image asymmetry to
  `/history`'s 422 for an unknown CD; both are intentional and test-locked, not bugs
  to "fix" toward consistency.
- **requireCcypairCd()** — malformed ticker (wrong length after normalization) is 422
  *before* any DB lookup — a well-formed-but-nonexistent CD only becomes 404 later,
  after the repository call fails to find it.
  - **normalizeCcypairCd()** — strips `FX:` **first**, then `/`, so
    `FX:USD/JPY` → `USDJPY` (valid, length 6). This is the sibling of `history()`'s
    `normalizeSymbolCd()`; the same raw input normalizes differently under each,
    which is the single biggest source of confusion between these two endpoints.
- **ccypairRepository.findByCcypairCdAndIsDeleted()** — the actual existence check;
  `Optional` empty → `ResourceNotFoundException` (404).
- **currentSession()** — session string (summer/winter) driven by `m_season`, not by
  the widget's own clock.
  - **seasonRepository.findBySeasonCdInAnd...** — empty result is a **500**, not a
    client error: the seed data covers 2020–2099, so a miss here means the season
    seed/config itself is broken, not that the client sent something wrong.
- **toSymbolInfo()** — assembles the UDF `SymbolInfo` DTO; `minmov` is hardcoded `1`
  and the plot type is always `"price"` (volume plotting is unused in this datafeed).
  - **displayTicker()** — `USDJPY` → `USD/JPY` for widget display; non-6-char input
    passes through unchanged rather than erroring (defensive, since validation
    upstream should already guarantee length 6).
  - **priceScale()** — `10^rate_unit`, i.e. pip display precision — **not** the live
    quote's actual decimal precision, easy to conflate with something else.

---

## DOC 124 — SEARCH

```
search()
├── resolveSearchLimit()
├── matchesConfiguredFilter()
├── ccypairRepository.searchActive()
└── toSearchSymbol()
    └── displayTicker()
```

- **search()** — query longer than 10 chars is 422; everything else that "finds
  nothing" (wrong exchange/type, no DB matches) returns `200 []` instead of erroring,
  so the widget only ever treats a too-long query as a hard datafeed failure.
- **resolveSearchLimit()** — omitted `limit` falls back to the configured default;
  an out-of-range value ( `<1` or `>` configured max) is a 422, not silently clamped
  — an out-of-range limit is treated as a client bug worth surfacing, not a hint to
  reinterpret.
- **matchesConfiguredFilter()** — blank exchange/type means "no filter" (match all);
  a non-blank mismatch is *not* an error by itself — the caller (`search()`) converts
  a mismatch into an empty result list rather than a 422.
- **ccypairRepository.searchActive()** — matches against **both** the display ticker
  and the slash-stripped CD (`needleCd`) simultaneously, and an empty query is a valid
  "list everything active" request rather than an edge case to special-case away.
- **toSearchSymbol()** — builds each search hit; ticker is the raw 6-char CD, display
  is the slashed form, Japanese name comes straight from `ccypair_jp`.
  - **displayTicker()** — same helper reused from the resolve path; keeps display
    formatting consistent between `/symbols` and `/search` without duplicating logic.

---

## DOC 125 — MARKS

```
marks()
├── validateMarksRequest()
│   └── ResolutionMapper.isMarksResolution()
├── normalizeCcypairCd()
├── tvMarkRepository.findBy...
└── toMarkDto()
```

- **marks()** — global demo seed data, no `customer_no` tenancy. After validation, a
  well-formed pair with zero matching rows still returns `200 []` — this method never
  checks that the pair itself exists, so there's no 404 path here at all.
- **validateMarksRequest()** — 422 on blank symbol, bad resolution, or `to < from`.
  - **ResolutionMapper.isMarksResolution()** — its **own** allowlist, distinct from
    `isHistoryResolution()`. Concretely, `resolution=10` is valid for `/history` but
    **422** here — the single most likely place someone copies logic between
    marks and history and breaks one of them.
- **normalizeCcypairCd()** — same normalizer used by `resolve()` (strips `FX:` then
  `/`), reused here since marks lookups key off the same CD format.
- **tvMarkRepository.findBy...** — ordered by `markAt` ascending; no existence check
  on the pair itself, consistent with the "always 200, possibly empty" contract.
- **toMarkDto()** — label styling (`#ffffff`, size 14) is **hardcoded** in this
  mapper, not stored per-row in `m_tv_mark` — don't go looking for a font/color
  column that isn't in the schema.

---

## DOC 126 — TIMESCALE MARKS

```
timescaleMarks()
├── validateMarksRequest()
├── normalizeCcypairCd()
├── tvTimescaleMarkRepository.findBy...
└── toTimescaleMarkDto()
```

- **timescaleMarks()** — identical contract to `marks()`: global, no tenancy, 422 on
  bad fields, `200 []` if nothing matches (never 404 for an unknown-but-valid pair).
- **validateMarksRequest()** — the *exact same* shared method `marks()` uses. Any
  future change to marks validation (e.g. the resolution allowlist) silently changes
  behavior for both endpoints at once — if these two ever need to diverge, this
  shared method has to be split first.
- **normalizeCcypairCd()** — same CD normalizer as `resolve()`/`marks()`.
- **tvTimescaleMarkRepository.findBy...** — ordered by `timescaleMarkAt` ascending;
  same "no pair-existence check" behavior as marks.
- **toTimescaleMarkDto()** — wraps `tooltip` in a single-element `List.of(...)` even
  though the DB column is one plain string — purely to satisfy the widget's expected
  array shape. "Simplifying" this to a bare string would break widget deserialization.

---

# ChartLayoutServiceImpl — Docs 127–131

Table `m_tv_chart_layout`, scoped by JWT `customer_no`. Two cross-cutting rules apply
to every endpoint below and are called out once here instead of repeated per-doc:

- **Tenant isolation is 404-not-403.** Another tenant's layout id looks exactly like a
  missing id — the API never confirms an id exists before denying it, so no endpoint
  should be "fixed" to return 403 instead.
- **Missing `CustomerContext` is 500-not-401.** The security filter is expected to
  have already set it; an unset context means the filter itself is broken, not that
  the caller is unauthenticated.

Template services (chart templates 136–139, indicator templates 132–135) are
explicitly told to copy this 404-vs-403 / 500-vs-401 pattern rather than filtering
tenant from `SecurityContextHolder` directly.

---

## DOC 127 — REGISTER (create layout)

```
register()
├── null request check
├── validateUpsertBody()
│   └── ResolutionMapper.isMarksResolution()
├── normalizeCcypairCd()
├── requireActivePair()
│   └── ccypairRepository.findByCcypairCdAndIsDeleted()
├── CustomerContext.requireCustomerNo()
├── new TvChartLayout(...)
└── tvChartLayoutRepository.save()
```

- **register()** — inserts a new row scoped to the JWT tenant. An unknown or
  soft-deleted symbol is **404** (catalog miss), not 422 — the deliberate contrast
  with `/history`, which 422s the same kind of unknown-CD miss.
- **null request check** — a missing body is 422 before any field-level validation
  even runs.
- **validateUpsertBody()** — blank/oversize name (>64 chars), blank content, blank
  symbol, or bad resolution all 422 with no custom message per field.
  - **ResolutionMapper.isMarksResolution()** — layout resolution reuses the *marks*
    allowlist (no `10`), **not** the history list — same trap as docs 125/126, now a
    third place that trips on the `10` case if someone copies from `/history` logic.
- **normalizeCcypairCd()** — identical to `ChartDataServiceImpl`'s version (strips
  `FX:` then `/`). Explicitly **not** the history letter-only strip
  (`normalizeSymbolCd`) — mixing the two across services is the same class of bug
  called out in the data-service doc.
- **requireActivePair()** — existence/soft-delete check against the catalog.
  - **ccypairRepository.findByCcypairCdAndIsDeleted()** — empty result → 404, mirrors
    `resolve()`'s catalog-miss behavior in `ChartDataServiceImpl`, intentionally
    diverging from `/history`'s 422 for the same underlying case.
- **CustomerContext.requireCustomerNo()** — pulled only *after* body/catalog
  validation passes, so a bad request never even needs a resolved tenant. Throws 500
  if unset, per the class-level contract — never 401.
- **new TvChartLayout(...) / save()** — persists with `customerNo`, trimmed name,
  raw content, normalized CD, resolution, and `Instant.now()` as the timestamp; the
  response only ever returns the new id (`ChartLayoutIdResponse`), never the row.

---

## DOC 128 — UPDATE (full replace)

```
update()
├── null request check
├── validateUpsertBody()
├── normalizeCcypairCd()
├── requireActivePair()
├── requireOwnedLayout()
│   ├── parseLayoutId()
│   ├── CustomerContext.requireCustomerNo()
│   └── tvChartLayoutRepository.findById()
├── layout.applyUpdate()
└── tvChartLayoutRepository.save()
```

- **update()** — this is a **full replace**, not a partial PATCH: content, name,
  symbol, and resolution are all overwritten unconditionally. There is no merge
  logic, so a caller that omits a field on purpose will still wipe it.
- **null request check / validateUpsertBody() / normalizeCcypairCd() /
  requireActivePair()** — identical validation pipeline to `register()`, run
  **before** ownership is even checked — a malformed body 422s regardless of whether
  the target layout exists or belongs to someone else.
- **requireOwnedLayout()** — the shared ownership gate (see helper detail under
  DOC 129, since `get()`/`update()`/`delete()` all call the same method).
  - Ordering matters here: validation happens first, ownership second, so a bad
    body on someone *else's* id still 422s rather than 404s — worth knowing when
    writing tests that probe both failure modes.
- **layout.applyUpdate()** — mutates the already-loaded, already-owned entity in
  place with the new name/content/CD/resolution and a fresh `Instant.now()`.
- **tvChartLayoutRepository.save()** — persists the mutated entity; response is again
  just the id, same shape as `register()`.

---

## DOC 129 — GET (fetch one)

```
get()
└── requireOwnedLayout()
    ├── parseLayoutId()
    │   # non-numeric id → 422, not 404
    ├── CustomerContext.requireCustomerNo()
    └── tvChartLayoutRepository.findById()
        # wrong tenant or missing row → 404 (never 403)
```

- **get()** — the simplest of the five; its entire job is proving ownership and
  shaping the DTO. Same 404-not-403 contract as `update()`/`delete()` so a caller
  can't distinguish "doesn't exist" from "exists but isn't yours" by probing ids.
- **requireOwnedLayout()** — shared by `update()`, `get()`, and `delete()`; this is
  the one place the tenant-isolation rule is actually enforced, so any change here
  ripples across all three mutating/reading-by-id endpoints at once.
  - **parseLayoutId()** — a non-numeric path segment is **422** (malformed input is
    validation, not "resource missing"), evaluated *before* any DB call — distinct
    from the 404 that follows for a well-formed but wrong/foreign id.
  - **CustomerContext.requireCustomerNo()** — resolved before the `findById()` call
    so the comparison below always has a tenant to compare against; missing context
    is the 500 case from the class-level contract.
  - **tvChartLayoutRepository.findById()** — this repository method is
    **unscoped** (no tenant filter at the query level); the tenant check happens
    manually in application code right after (`layout.getCustomerNo() != customerNo`
    → 404). Anyone adding a new query against this repository needs to remember this
    isn't automatically tenant-safe.
- **DTO shaping** — `get()` returns the full `content` blob; contrast with `list()`
  below, which omits it.

---

## DOC 130 — LIST

```
list()
├── CustomerContext.requireCustomerNo()
├── tvChartLayoutRepository.findByCustomerNoOrderByUpdatedAtDesc()
└── toListItem()
```

- **list()** — the only endpoint in this group that doesn't touch `requireOwnedLayout()`
  at all, since there's no id to own-check — the query itself is scoped by tenant.
- **CustomerContext.requireCustomerNo()** — same 500-not-401 contract as everywhere
  else in this service if the filter failed to set it.
- **tvChartLayoutRepository.findByCustomerNoOrderByUpdatedAtDesc()** — unlike
  `findById()` in `requireOwnedLayout()`, this query *is* tenant-scoped at the DB
  level, sorted newest-updated-first.
- **toListItem()** — deliberately a lighter DTO than `get()`'s: **omits the content
  blob** (bandwidth), and its `resolution` field is actually the DB's `chart_type`
  column verbatim (e.g. `1D`, `60`) — **not** any human-friendly form like `DAY` or
  `60M`. Don't assume the list and get endpoints return resolution in the same shape
  as some normalized enum; they're passing through raw stored values.

---

## DOC 131 — DELETE

```
delete()
├── requireOwnedLayout()
├── tvChartLayoutRepository.delete()
└── new SystemDatetimeResponse(Instant.now().getEpochSecond())
```

- **delete()** — deletion only happens *after* the same ownership gate as `get()`/
  `update()`, so attempting to delete a foreign or nonexistent id is 404, never a
  silent no-op or 403.
- **requireOwnedLayout()** — same shared helper as docs 128/129; no separate
  existence check is needed since ownership resolution already guarantees the row is
  real and belongs to the caller.
- **tvChartLayoutRepository.delete()** — hard delete, not a soft-delete/flag flip
  (contrast with `Ccypair`'s `isDeleted` pattern used elsewhere in the catalog).
- **SystemDatetimeResponse** — the response body isn't an id or confirmation flag at
  all, just server-now seconds (`t`) — same epoch-seconds convention as
  `serverTimeSeconds()` in `ChartDataServiceImpl` (doc 122), reused here rather than
  returning anything about the deleted row itself.

---

## Cross-service gotchas (ChartDataServiceImpl ↔ ChartLayoutServiceImpl)

| Concern | `ChartDataServiceImpl` | `ChartLayoutServiceImpl` |
|---|---|---|
| Unknown/soft-deleted CD | `/history` → **422**; `/symbols` (resolve) → **404** | `register`/`update` → **404** (matches `resolve()`, not `/history`) |
| CD normalization used | `normalizeSymbolCd()` for history (letters-only); `normalizeCcypairCd()` for resolve/marks | `normalizeCcypairCd()` only (same helper as data-service's resolve/marks path) |
| Resolution allowlist | `HISTORY_RESOLUTIONS` (has `10`) for history; `MARKS_RESOLUTIONS` (no `10`) for marks | `MARKS_RESOLUTIONS` (no `10`) for layout resolution — same trap as marks |
| Unknown/foreign id | N/A (no owned-resource concept) | **404**, never 403 — id existence is never confirmed to the caller |
| Missing tenant context | N/A | **500**, never 401 — filter is expected to have set it |

---

# ChartTemplateServiceImpl — Docs 136–139

Table `m_tv_chart_templates` (plural in the spec), scoped by JWT `customer_no`. Same two invariants as
`ChartLayoutServiceImpl`, copied deliberately rather than reimplemented against
`SecurityContextHolder`:

- **Another tenant's name is 404, not 403** — the lookup query is scoped by
  `customer_no` from the start, so a miss can never distinguish "unknown name" from
  "belongs to someone else."
- **Missing `CustomerContext` is 500, not 401** — a filter bug, not an auth failure.

The key structural difference from layouts: templates are keyed by **`(customer_no,
name)`** instead of a numeric id, and `upsert()` folds create + update into one
operation instead of splitting them across `register()`/`update()`.

---

## DOC 136 — LIST

```
list()
├── CustomerContext.requireCustomerNo()
├── tvChartTemplateRepository.findByCustomerNoOrderByNameAsc()
└── toListItem()
```

- **list()** — this tenant's templates only, sorted alphabetically by name (unlike
  the layout list, which sorts by most-recently-updated).
- **CustomerContext.requireCustomerNo()** — same 500-not-401 contract as everywhere
  else; the filter is expected to have already populated this.
- **tvChartTemplateRepository.findByCustomerNoOrderByNameAsc()** — DB-level tenant
  scoping, same pattern as `ChartLayoutServiceImpl.list()`.
- **toListItem()** — deliberately a thin wrapper carrying **only the name**, nothing
  else. The widget is expected to call `get()` separately for content — don't add
  content here "for convenience," it would duplicate what `get()` is for and bloat
  the list payload.

---

## DOC 137 — UPSERT (create-or-update)

```
upsert()
├── null request check
├── requireTemplateName()
├── content null/blank check
├── CustomerContext.requireCustomerNo()
├── tvChartTemplateRepository.findByCustomerNoAndName()
│   ├── present → TvChartTemplate.applyContent() → save()
│   └── absent  → new TvChartTemplate(...) → save()
└── SystemDatetimeResponse
```

- **upsert()** — the one endpoint in this group that isn't split into separate
  create/update methods like layouts are (`register()` vs `update()`). One call
  handles both, keyed on whether `(customer_no, name)` already exists.
- **null request check / requireTemplateName() / content check** — all 422s, run
  before any tenant/DB work, same ordering discipline as `ChartLayoutServiceImpl`'s
  upsert validation.
- **CustomerContext.requireCustomerNo()** — resolved only after body validation
  passes, so a malformed request never needs a tenant lookup to fail.
- **tvChartTemplateRepository.findByCustomerNoAndName()** — this single query is the
  fork point for create vs. update:
  - **present → `applyContent()` → `save()`** — same name for this tenant means
    **replace content only**, not insert a second row. There is no way to have two
    templates with the same name under one tenant — the unique key enforces this at
    the query level, not via a separate constraint-violation catch.
  - **absent → `new TvChartTemplate(...)` → `save()`** — first-time name for this
    tenant inserts fresh.
  - Note: **other tenants can reuse the same name freely** — the key includes
    `customer_no`, so name collisions across tenants are expected and never conflict.
- **SystemDatetimeResponse** — response is just server-now seconds, the same
  lightweight shape used by layout `delete()` — no id or name echoed back, so a
  caller relying on the response to know *which* branch (insert vs. update) was
  taken will need to track that themselves.

---

## DOC 138 — GET (fetch one by name)

```
get()
└── requireOwnedTemplate()
    ├── requireTemplateName()
    ├── CustomerContext.requireCustomerNo()
    └── tvChartTemplateRepository.findByCustomerNoAndName()
```

- **get()** — mirrors `ChartLayoutServiceImpl.get()`'s shape, but keyed by `name`
  instead of a numeric id, and with no separate "ownership after lookup" check —
  ownership is baked directly into the query itself.
- **requireOwnedTemplate()** — shared by `get()` and `delete()` (see doc 139).
  - **requireTemplateName()** — blank or over 64 chars is 422, identical validation
    to the one used in `upsert()` — reused rather than duplicated, so any future
    change to name rules (e.g. max length) automatically applies to both paths.
  - **CustomerContext.requireCustomerNo()** — 500-not-401 if unset, as elsewhere.
  - **tvChartTemplateRepository.findByCustomerNoAndName()** — because this query is
    already scoped to `customer_no + name`, an empty result is **inherently**
    ambiguous between "no such name" and "exists for another tenant" — and that
    ambiguity is the point, not a gap. The Javadoc is explicit: **do not add a
    global-by-name finder**, since that would let application code distinguish the
    two cases and accidentally leak existence across tenants.
- **ChartTemplateDto** — returns name + content; contrast with `toListItem()`, which
  strips content entirely.

---

## DOC 139 — DELETE

```
delete()
├── requireOwnedTemplate()
├── tvChartTemplateRepository.delete()
└── new SystemDatetimeResponse(Instant.now().getEpochSecond())
```

- **delete()** — same 404-as-missing contract as `get()`; a foreign or unknown name
  is indistinguishable from the caller's perspective.
- **requireOwnedTemplate()** — identical helper reused from `get()`; no separate
  existence check needed afterward since a successful lookup already proves the row
  is real and owned by the caller.
- **tvChartTemplateRepository.delete()** — hard delete, same as layout delete; no
  soft-delete flag involved for templates either.
- **SystemDatetimeResponse** — server-now seconds only, same shape/convention as
  layout `delete()` (doc 131) and `serverTimeSeconds()` (doc 122) — nothing about the
  deleted template is echoed back.

---

## Cross-service gotchas (ChartLayoutServiceImpl ↔ ChartTemplateServiceImpl)

| Concern | `ChartLayoutServiceImpl` (127–131) | `ChartTemplateServiceImpl` (136–139) |
|---|---|---|
| Primary key exposed to client | Numeric `id` (path param) | `name` string, unique per `(customer_no, name)` |
| Create vs. update | Split: `register()` (insert-only) / `update()` (full replace) | Merged: single `upsert()` branches on lookup result |
| Duplicate create attempt | New id every time (no natural uniqueness on name) | Same name **updates existing row**, never creates a duplicate |
| Ownership check shape | `findById()` (unscoped) + manual `customerNo` comparison | `findByCustomerNoAndName()` (scoped at the query itself) |
| List payload | Omits `content`, includes `chartType`/`ccypairCd`/`updatedAt` | Omits `content`, name only |
| Tenant/id error codes | 404-not-403, 500-not-401 | Same: 404-not-403, 500-not-401 (explicitly copied pattern) |

---

# TickIngestWorker — live OHLC (not an HTTP API)

`@Order(200)` scheduled worker. **Only** writer of forming candles. Python relays
`peach:quotes` / `peach:bars`; it does not compute OHLC. `@EnableScheduling` on
`ChartBackendApplication` is why `tick()` fires.

Not `@Transactional`. No per-pair try/catch — one JDBC/Redis failure aborts the
rest of that tick on purpose (outage should stop ingest, not silently skip pairs).

---

## BOOT SNAPSHOT (`ApplicationRunner.run`)

```
run()
├── demoTickEngine.loadFromWarehouse()
├── publishQuoteSnapshot()
│   └── quoteBus.publish()                  # peach:quote:* SET + PUBLISH peach:quotes
└── publishFormingSnapshot()
    └── for each pair × CacheNamespace
        ├── chartCacheStore.query()         # last Redis bar
        ├── formingBars.put(...)
        └── quoteBus.publishForming()       # peach:forming:* SET + PUBLISH peach:bars
```

- **run()** — so a WS client that connects *before* the first 333ms tick still sees
  a header price and a forming candle. Does not step the mock LP.
- **demoTickEngine.loadFromWarehouse()** — aligns mock LP with seeded 1S closes.
  Replace this bean for a real Peach feed; keep the rest of the worker.
- **publishQuoteSnapshot() / publishFormingSnapshot()** — republish stored state
  only. Forming snapshot keys `formingBars` by `namespace + USDJPY` (warehouse CD),
  but `publishForming` takes **numeric** `curpairCd` as a string (Python tick id).

---

## SCHEDULED TICK (`tick`)

```
tick()                                      # @Scheduled app.chart-cache.tick-ms (333)
├── chartCacheWriter.isSeedComplete()       # false → return (writer is @Order 100)
├── demoTickEngine.stepAll()
└── for each FxQuoteMessage
    ├── upsertOpenBars()
    │   ├── currencyPairService.find(priority)
    │   └── for each CacheNamespace
    │       └── upsertOpenBar()
    │           ├── openFromTick() | applyTick()
    │           ├── formingBars.put()
    │           ├── chartBarRepository.upsert()
    │           ├── chartCacheStore.put()
    │           └── quoteBus.publishForming()
    └── quoteBus.publish()                  # header tick AFTER persist
```

- **isSeedComplete()** — ingest must not tick during boot replace. If scheduling
  "silently" never publishes, check this flag and `@EnableScheduling` first.
- **stepAll()** — mock LP only. Product rules (validation, tenant, 422) must not
  land here.
- **upsertOpenBars()** — unknown numeric `curpairCd` → skip that quote (no 422;
  this is not HTTP). Then every Peach resolution for that pair.
- **upsertOpenBar()** — new period → `openFromTick`; same period → `applyTick`.
  Persist warehouse + Redis **then** publish forming, so `/api/history` last bar
  matches the socket candle. Same forming row is what `ChartCacheStore` will return.
- **quoteBus.publish()** after persist — header tick is the same pair, later in
  the loop. One failure in upsert stops remaining pairs this cycle.

---

# ChartCacheStore — `/api/history` read path

Not a SUBSCRIBE. No `stitchCurrentBar`. Last bar = whatever ingest last `put`.

```
query(namespace, USDJPY, from, to)
├── warmFromWarehouseIfEmpty()
│   ├── ZCARD peach:{cacheName}:USDJPY
│   └── if empty → chartBarRepository.query() → replacePairUnlocked()
└── ZRANGEBYSCORE (unix seconds inclusive)

put(namespace, bar)                         # ingest live last bar
└── ZREM score + ZADD same key

nextTimeBefore(...)
├── warmFromWarehouseIfEmpty()
├── ZREVRANGEBYSCORE ... LIMIT 1
└── else chartBarRepository.nextTimeBefore()

replacePair(...)                            # boot seed only (ChartCacheWriter)
└── DELETE key + ZADD all members
```

- **query()** — cache-aside. Cold Redis key warms from `t_chart_*` once, then
  serves ZRANGE. Python does **not** read these ZSETs (it SUBSCRIBEs `peach:bars`).
- **from > to** → empty list, not 422 (service already validated the HTTP range).
- **put()** — score = unix **seconds**. Member = JSON `CachedChartBar` (bid *and*
  ask columns). History projects one side later in `toBarDto`.
- **Key shape** — `peach:{cacheName}:{USDJPY}` e.g. `peach:cache_set_1m:USDJPY`.
  TV `"1"` is `cache_set_1m` (one **minute**), not month.

---

# CacheNamespace + ResolutionMapper — resolution SSOT

```
fromTvResolution("1" | "1D" | ...)
└── ResolutionMapper.toPeachChartType()
    └── match CacheNamespace.chartType     # null → 422 on history

isHistoryResolution()                      # includes "10"
isMarksResolution()                        # no "10" — also used by layouts
periodMillis() / tableName() / cacheName()
```

- **TV `"1"` = 1 minute** (`t_chart_60`, Peach `1M`, `cache_set_1m`), not 1 month.
  `1M` on the widget is the month bar. This is the newcomer trap.
- New resolution must land in **three** places: this enum, `ResolutionMapper`
  lists, and Flyway `t_chart_*`. One place only = classic drift bug.
- Table names **always** from the enum. Never interpolate a request string into
  JDBC (injection-shaped even though it is not raw user SQL today).

---

# QuoteBus + CachedChartBar — WS / REST projection

```
quoteBus.publish(quote)
├── SET peach:quote:{numericCd}
└── PUBLISH peach:quotes                    # Python market.py hardcodes this name

quoteBus.publishForming(numericCd, ns, bar)
├── SET peach:forming:{tvResolution}:{USDJPY}
└── PUBLISH peach:bars                      # Python hardcodes this name too

CachedChartBar.toBarDto(BID|ASK|MID)
├── time = chartDatetimeSec * 1000          # BarDto millis; HistoryResponse t[] is seconds
└── MID = (bid + ask) / 2 at read time      # same idea as Python widget_bar()
```

- Channel/key **renames are a silent two-process break** — no shared schema with
  Python. Treat any edit here as a Java + `ws-python/market.py` change.
- `numericCd` on the bus is catalog `priority` as a **string**; warehouse rows
  key by `USDJPY`. Do not swap those in a refactor.
- REST vs WS mismatch → diff `toBarDto` against `widget_bar()`. Null side in
  `toBarDto` defaults MID but HTTP history never passes null (422 if missing
  `bid_ask`).

---

# ChartBarRepository — JDBC warehouse

Not JPA. Table name from `CacheNamespace` only.

```
upsert(namespace, bar)                      # ingest
├── DELETE ... WHERE curpair_cd AND chart_datetime
└── INSERT

replacePair(namespace, USDJPY, bars)        # boot seed
├── DELETE ... WHERE curpair_cd
└── INSERT each bar                         # mid-crash can empty that pair until next boot

query(...) / nextTimeBefore(...)            # cache warm / Peach no_data nextTime
```

- DELETE-then-INSERT (not SQL MERGE) so H2 tests match Postgres.
- Controllers never call this. History goes Redis first.

---

# ChartCacheWriter — boot seed only (`@Order 100`)

No scheduled refresh. Stale history after days of uptime is **ingest**, not this.

```
run()
└── seedAll()
    └── for each CacheNamespace × catalog symbol
        ├── mockBarGenerator.peachBarAt()   # both bid and ask OHLC
        ├── chartBarRepository.replacePair()
        └── chartCacheStore.replacePair()
    seedComplete = true                     # TickIngestWorker may tick
```

- `peachBarAt` writes **both** sides into one warehouse row. History then
  projects BID/ASK/MID. After this returns, `MockBarGeneratorImpl` is idle until
  next process start.

---

# JwtAuthenticationFilter — tenant ThreadLocal

Highest-risk control flow in the backend. Does **not** write 401.

```
doFilterInternal()
├── try
│   ├── if Authorization: Bearer
│   │   └── authenticateBearer()
│   │       ├── jwtService.parseToken()     # JwtException → leave anonymous
│   │       └── setAuthentication()
│   │           ├── SecurityContextHolder   # ChartPrincipal
│   │           └── CustomerContext.set(customerNo)
│   └── filterChain.doFilter()              # SecurityConfig matcher → 401 if needed
└── finally
    ├── CustomerContext.clear()             # MUST — Tomcat thread reuse leak
    └── SecurityContextHolder.clearContext()
```

- Bad/missing token: request stays anonymous; `JsonUnauthorizedEntryPoint` 401s
  **protected** paths. This filter never writes the body.
- Skipping `finally` (or returning before it) leaks the previous request's
  `customer_no` onto the next worker — real cross-tenant leak, not theoretical.
- Tenant APIs must use `CustomerContext.requireCustomerNo()`, not
  `SecurityContextHolder` alone.

---

# RefreshTokenStore — opaque UUID rotation

Key `peach:auth:refresh:{uuid}`, TTL from `app.jwt.refresh-expiration-ms`.
Not a JWT, not the bar cache keys.

```
issue(username, customerNo)
└── SET uuid → "username|customerNo" + TTL

rotate(oldUuid)
├── find(oldUuid)                           # empty → empty (caller 401)
├── revoke(oldUuid)                         # old dies FIRST
└── issue(...)                              # new uuid

revoke(uuid)
└── DEL
```

- Steal-resistant: old UUID is gone before the new cookie is issued. Logout then
  refresh must fail (`structure.md` §6.1). Reusing the same Redis key would
  break that test.

---

# AuthServiceImpl — login / refresh / logout

Refresh UUID is **never** a JSON field — only Redis + HttpOnly cookie.
`login` is `@Transactional(readOnly=true)` (JPA read of `m_app_user` only; Redis
write has nothing to roll back).

---

## LOGIN

```
login()
├── authenticate()
│   ├── blank user/pass → 422               # LoginRequest has no @NotBlank
│   ├── findByUsername
│   └── unknown / disabled / wrong hash
│       └── BadCredentialsAppException      # same 401, no user-enum
├── jwtService.createToken()                # 1h HS256, claim customer_no
├── refreshTokenStore.issue()
├── authCookieSupport.setRefreshCookie()
└── buildLoginResponse()                    # accessToken + expiries; no refresh value
```

- 401 `E_BAD_CREDENTIALS` is **login only**. Do not reuse for a bad refresh cookie.

---

## REFRESH

```
refresh()
├── authCookieSupport.readRefreshToken()    # missing → UnauthorizedAppException
├── refreshTokenStore.rotate()              # unknown → same 401
├── refreshTokenStore.find(newUuid)
├── jwtService.createToken()
├── setRefreshCookie(newUuid)
└── buildRefreshResponse()                  # still no refresh UUID in body
```

- 401 `E_UNAUTHORIZED` = got past the filter, cookie was bad. Distinct from
  filter-chain 401 (`JsonUnauthorizedEntryPoint`) and from login 401.

---

## LOGOUT

```
logout()
├── readRefreshToken().ifPresent(revoke)    # missing cookie still 200
└── clearRefreshCookie()
```

- Idempotent. After this, `refresh()` must fail.

---

# SymbolCatalogImpl — history lookup (returns null)

Built **once** at startup from `CurrencyPairService.list()`. Pair catalog edits
need a restart.

```
find(raw)
└── matches()                               # USD/JPY, USDJPY, numeric priority, fx:USD/JPY
    └── null on miss                        # does NOT throw
```

- Caller chooses the status: `ChartDataServiceImpl` history → **422**, resolve
  uses the repository not this catalog for 404. Do not throw 404 from `find()`.
- Matcher would accept `FX:USD/JPY`, but history **validation** letter-strips to
  `FXUSDJPY` (length 8 → 422) **before** `find` runs. This method does not make
  `FX:` legal on `/history`.

---

# CurrencyPairServiceImpl — `GET /curpairs`

Outside `/api`, still Bearer (`SecurityConfig`). Not doc 123 `/api/symbols`.

```
list()
└── findByIsDeletedOrderByPriorityAsc(ACTIVE=0)
    └── toDto()
        # curpairCd = priority (Python tick id, JSON number here / string on WS)
        # curpairName = ccypair_cd (USDJPY)
        # curpairDisplay = USD/JPY
```

- Swapping `curpairCd` and `curpairName` breaks the WS keying. Soft-deleted pairs
  stay out of ingest and the widget catalog.
- `find(priority)` returns **null** on miss — ingest skips; HTTP callers decide.

---

# IndicatorTemplateServiceImpl — Docs 132–135

Table `m_tv_indicator_template`. **Same trees as** `ChartTemplateServiceImpl`
(136–139): `list` / `upsert` / `get` / `delete`, unique `(customer_no, name)`,
content-only update, 404-not-403, 500-not-401. Two design-doc numbers, not two
behaviors.

```
list()   → findByCustomerNoOrderByNameAsc → name-only items
upsert() → findByCustomerNoAndName → applyContent | insert → SystemDatetimeResponse
get()    → requireOwnedTemplate → name + content
delete() → requireOwnedTemplate → hard delete → SystemDatetimeResponse
```

Do not add a global-by-name finder. After reading chart templates, skim this
file for table/DTO names only.

---

# GlobalExceptionHandler — JSON error contract

Controllers do not catch. Filter-chain 401 never reaches this class.

```
ValidationException                         → 422 CODE:30020   (same localized text always)
MethodArgumentNotValid / unreadable / mismatch
                                            → 422 CODE:30020   (MSG_BAD_REQUEST)
ResourceNotFoundException                   → 404 CODE:30404
BadCredentialsAppException                  → 401 E_BAD_CREDENTIALS   (login)
UnauthorizedAppException                    → 401 E_UNAUTHORIZED      (bad refresh cookie)
ServerErrorException                        → 500 E_SERVER            (missing CustomerContext / m_season)
```

- `ValidationException` carries **no** custom message. You cannot attach
  "resolution 10 invalid for marks" without changing this design.
- Two 401 paths on purpose: this handler (after DispatcherServlet) vs
  `JsonUnauthorizedEntryPoint` (never got past the gate). Do not collapse them.

---

# File-level leftovers — preview only

These still matter on a PR, but class Javadoc / NOT list / field names are enough.
Recheck against `backend-review-order.md` (same package rank). Do **not** walk
method bodies unless a diff touches them.

## `cache`

| File | What to confirm at file level |
|---|---|
| `DemoTickEngine` | Throwaway mock LP. No product/validation rules leaked in. |

## `service.impl`

| File | What to confirm at file level |
|---|---|
| `MockBarGeneratorImpl` | Boot seed only (`peachBarAt`). After boot, ingest owns the last bar. |
| `LocalizedMessageServiceImpl` | New error keys need **both** `messages.properties` and `messages_ja.properties`. |

## `security`

| File | What to confirm at file level |
|---|---|
| `CustomerContext` | ThreadLocal, not a session. Tenant APIs use this, not `SecurityContextHolder`. |
| `JwtService` | HS256 access JWT; `app.jwt.secret` demo-only; rotate = everyone logged out. |
| `AuthCookieSupport` | HttpOnly `chart_refresh_token`, `Path=/`, `SameSite=Lax`. Browser-vs-curl refresh. |
| `SecurityConfig` | CSRF off, stateless. Public: health, `/api/auth/**`, Swagger, OPTIONS. `/curpairs` is protected. |
| `JsonUnauthorizedEntryPoint` | Filter-chain 401 only. Do not merge with `UnauthorizedAppException`. |
| `ChartPrincipal` | Data carrier (username + customerNo). No logic. |

## `controller` — HTTP only, no SQL

| File | What to confirm at file level |
|---|---|
| `ChartDataController` | Docs 120–126 + `/api/health`. Query-param binding only. |
| `AuthController` | `/refresh` and `/logout` public (cookie, not Bearer). |
| `ChartLayoutController` | POST 201 `{id}`; DELETE `{t: now}`; other tenant 404 is the **service**. |
| `CurrencyPairController` | `GET /curpairs` **outside** `/api`, still JWT. |
| `IndicatorTemplateController` / `ChartTemplateController` | Same verb shape; read one, skim the other. |

## `exception` / `dto`

| File | What to confirm at file level |
|---|---|
| `ValidationException` and siblings | Status table in Javadoc; no custom 422 message. |
| `ErrorResponse` | `{errorCode, message}` shape only. |
| `HistoryResponse` | Peach `t[]` unix **seconds**; `BarDto.time` **millis**. Off-by-1000. |
| `LoginResponse` | Refresh **value** never a field (expiry number only). |
| `FxQuoteMessage` / `FormingBarMessage` | Python wire contract — rename = silent break. |
| `ChartLayoutDto` | DB `chart_type` → JSON `resolution` (`1D`/`60`). |
| `LoginRequest` | No `@NotBlank`; blanks validated in `AuthServiceImpl`. |

## `repository` / `entity`

| File | What to confirm at file level |
|---|---|
| `TvChartLayoutRepository` | No unscoped **content** finder. `findById` is unscoped; service checks tenant. |
| `CcypairRepository` | Soft-delete `is_deleted = 0` on search/active. |
| `AppUserRepository` | Lookup only; BCrypt is in `AuthServiceImpl`. |
| `SeasonRepository` | Demo window 2020–2099; miss → **500**. |
| `TvChartTemplateRepository` / `TvIndicatorTemplateRepository` | Scoped `(customer_no, name)`. Review one. |
| `TvMarkRepository` / `TvTimescaleMarkRepository` | Global, no tenant. |
| `TvChartLayout` | `chart_type` column vs JSON `resolution`. Never return entity as HTTP. |
| `Ccypair` | `ACTIVE = 0`; no SQL FKs. |
| `AppUser` | Hash never serialized. |
| `Season`, `TvMark`, `TvTimescaleMark`, templates | Column mirrors of Flyway. |

## `util` / `constants` / `config` / root

| File | What to confirm at file level |
|---|---|
| `DemoMarket` | Seed/spread helpers for mock bars only. |
| `ErrorCodes` | Nothing else hardcodes `CODE:30020` / `CODE:30404`. |
| `PriceComponent` | `fromBidAsk` strict (422); `from` lenient MID — only the unused `price=` path. |
| `MarkSeedWindow` | Demo mark unix-second window. |
| `AppProperties` + `application.yml` | JWT, tick-ms, TradingView flags. |
| `AppUserSeedRunner` | Seeds `demo` / `demo2`. Not docs 120–139. |
| `WebConfig` | CORS 5173/3000. Direct-to-Java vs Vite. |
| `PasswordConfig` | BCrypt bean. |
| `ChartBackendApplication` | `@EnableScheduling` — if ingest never fires, start here. |

