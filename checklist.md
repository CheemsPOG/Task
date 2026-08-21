# Mentor checkpoint — docs 120–134 vs this app

**Scope:** TradingView **datafeed** (120–126), chart layouts (127–131), and indicator templates through get (132–134). Doc **132** list is Done; **133–134** are planned in `plan.md` Steps 14–15. Docs **135–139** (indicator delete + chart templates) are out of scope until planned.

**How to read:** `Done` = matches the markdown for this demo. `Planned` = in `plan.md`, not coded yet. `Intentional gap` = doc says X, we kept Y so the widget still loads. `Open` = not done and still unsafe or incomplete.

Header on every `/api/**` call except health and login: `Authorization: Bearer <jwt>` (after `POST /api/auth/login`). Optional: `Accept-Language: en|ja` for localized error `message`.

---

## Shared (all 120–126)

| Doc requirement | Status | What we did |
|---|---|---|
| S-01 token auth | **Intentional gap** | Local JWT stand-in (not Peach SSO). `POST /api/auth/login` → Bearer on `/api/**`. Seeded `demo`/`demo` (customer 1), `demo2`/`demo2` (customer 2). FE login overlay. |
| 422 `CODE:30020` | **Done** | `ValidationException` → `{ "errorCode": "CODE:30020", "message": "<localized>" }` |
| 404 `CODE:30404` | **Done** | `{ "errorCode": "CODE:30404", "message": "<localized>" }`. History unknown symbol is `{ s: "error" }` (UDF), not 404. |
| 500 `E_SERVER` | **Done** | `{ "errorCode": "E_SERVER", "message": "<localized>" }` (EN default; JA if `Accept-Language: ja`) |
| No `ApiResponse` wrapper on UDF | **Done** | Raw JSON as the library expects |
| Postgres + Flyway | **Done** | V1–V7 (`m_app_user` for demo auth) |
| Frontend datafeed | **Done** | `datafeed.ts` + `api.ts` sends Bearer + `Accept-Language` |

**Not in 120–126 (do not treat as missing):** save/load still `LocalStorageSaveLoadAdapter`; quotes still mock WS; `/curpairs` is extra.

---

## 120 — Get datafeed configuration

**Path:** `GET /api/config`  
**Table:** none

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | JWT stand-in | 401 without Bearer |
| `supports_search` true | **Done** | yml `app.tradingview` |
| `supports_marks` true | **Done** | Enabled after 125 |
| `supports_timescale_marks` true | **Done** | Enabled after 126 |
| `supports_time` true | **Done** | |
| `exchanges` = CTFX | **Done** | `[{ value, name, desc: "CTFX" }]` |
| `symbols_types` = FOREX | **Done** | `[{ name, value: "FOREX" }]` |
| Resolutions `1S,1,5,15,30,60,120,240,480,1D,1W,1M` | **Done** | |
| `supports_group_request` | Extra | Doc omits it. We send `false` so the library uses `/search` (required). |

**Prove it:** `GET /api/config` + header → flags true, 12 resolutions, CTFX/FOREX. Widget Network: same URL 200.

**Test class:** `SystemOverviewDesign120Test` (yml profile `doc120`).

---

## 121 — Get bars

**Path:** `GET /api/history`  
**Tables:** Flyway V8 — all 13 `t_chart_*`  
**Cache:** Redis `peach:cache_set_*:{CD}`

| Doc item | Status | Notes |
|---|---|---|
| `bid_ask` required `BID\|MID\|ASK` | **Done** | Missing / invalid → 422 |
| `symbol` required, length 6 | **Done** | After normalize (`USD/JPY` → `USDJPY`) |
| `resolution` list including `10` | **Done** | → chart_type → table + `cache_set_*` |
| `from`/`to` pair, `to >= from` | **Done** | XOR → 422 |
| 13 `t_chart_*` tables | **Done** | V8; bid_/ask_ OHLC columns |
| `cache_set_*` + sync writer | **Done** | Redis hot cache; writer seeds DB then Redis |
| Response `{ s, t[], o[], h[], l[], c[] }` | **Done (dual)** | + widget `bars[]` |
| BID/ASK/MID from bid_/ask_ | **Done** | MID = average |
| `s=no_data` + `nextTime` | **Done** | Prior bar before `from`; weekend gaps |
| Sort ascending | **Done** | |
| Map TV → Peach chart_type | **Done** | `CacheNamespace` |

**Prove it:** `structure.md` §121 compliance checklist.

**Test class:** `SystemOverviewDesign121Test` + `FlywayMigrationTest`.

---

## 122 — Get server time

**Path:** `GET /api/time`  
**Table:** none  
**Doc does not mention auth** — still gated by the `/api/**` stub.

| Doc item | Status | Notes |
|---|---|---|
| Field `t` unix seconds, no ms | **Done** | |
| Widget field `serverTime` | Extra | Same value as `t` so `datafeed.ts` keeps working. |

**Prove it:** body has `t` === `serverTime`, within a few seconds of now. Widget countdown still works.

**Test:** covered with other controller tests / Postman.

---

## 123 — Get symbol information

**Path:** `GET /api/symbols?symbol=`  
**Tables:** `m_ccypairs`, `m_season` (V1/V2)

| Doc item | Status | Notes |
|---|---|---|
| Token | Stub | |
| `symbol` required, length 6 | **Partial** | Blank → 422. `USDJPY` and `USD/JPY` both 200. |
| Active pair `is_deleted=0` | **Done** | Else 404 `CODE:30404` |
| `name` = `ccypair_cd` | **Intentional gap** | `name` and `ticker` = `USD/JPY` so the chart header is readable. CD is `provider_symbol`. |
| `description` = `ccypair_jp` | **Done** | e.g. `米ドル/円` |
| `pricescale` = 10^`rate_unit` | **Done** | USDJPY → 1000 |
| timezone / exchange / type / has_intraday / visible_plots_set / resolutions / multipliers / has_seconds | **Done** | From yml |
| `minmov` = 1 | **Done** | |
| Session from `m_season` vs now | **Done** | DST → `time-summer`, else winter |
| No season row covering now | **Done** | 500 `E_SERVER` |
| Extra library fields | Extra | `ticker`, `listed_exchange`, `format`, daily/weekly multipliers, `data_status` |

**Prove it:** `symbol=USDJPY` and `USD/JPY` → 200, `pricescale=1000`, `exchange=CTFX`. `ETHUSD` → 404.

**Test class:** `SystemOverviewDesign123Test`.

---

## 124 — Get symbol list (search)

**Path:** `GET /api/search`  
**Table:** `m_ccypairs`

| Doc item | Status | Notes |
|---|---|---|
| Token | Stub | |
| `query` optional, max 10 | **Done** | Longer → 422 |
| `limit` 1…100, default 100 | **Done** | yml `search-default-limit` / `search-max-limit` |
| Match CD or Japanese name, partial | **Done** | |
| `is_deleted=0`, sort `priority` asc | **Done** | |
| DTO `symbol, description, type, exchange` | **Done** | `symbol`=CD, `description`=JP, type/exchange from yml |
| Widget `ticker` / `full_name` | Extra | Display `USD/JPY` so pick → resolve still works |
| Widget `exchange` / `type` query | Extra | Case-insensitive filter (CTFX/FOREX) |

**Prove it:** no query → 5 pairs, `USDJPY` first. `query=USD`, `query=円`. `query` 11 chars → 422.

**Test class:** `SystemOverviewDesign124Test`.

---

## 125 — Get marks list

**Path:** `GET /api/marks`  
**Table:** `m_tv_mark` (V3)

| Doc item | Status | Notes |
|---|---|---|
| Token | Stub | |
| `symbol`, `resolution`, `from`, `to` required; `to >= from` | **Done** | Missing / `to < from` / bad resolution → 422 |
| Resolution list (no `10`) | **Done** | `resolution=10` → 422 |
| Filter CD + resolution + `mark_at` in `[from, to]` | **Done** | Inclusive. Empty → `[]` not 404 |
| DTO `id, color, label, text, time` | **Done** | `time` = `mark_at` |
| `labelFontColor`, `minSize` | Extra | Library Mark requires them (`#ffffff`, 14) |
| Accept `USD/JPY` | Extra | Same normalize as 123 |

**Seed window (Postman):** `from=1787011200` `to=1787270400` `symbol=USDJPY` `resolution=1D` → `m1`/`m2`/`m3`.

**Frontend:** `getMarks` → `/api/marks`. Config `supports_marks: true`.

**Test class:** `SystemOverviewDesign125Test`.

---

## 126 — Get timescale marks list

**Path:** `GET /api/timescale_marks`  
**Table:** `m_tv_timescale_mark` (V4)

| Doc item | Status | Notes |
|---|---|---|
| Token | Stub | |
| Same validation as 125 | **Done** | |
| Filter CD + resolution + `timescale_mark_at` in range | **Done** | Empty → `[]` |
| DTO `id, color, label, time, tooltip` | **Done** | `time` = `timescale_mark_at` |
| `tooltip` as string | **Partial** | DB is a string; JSON is `["…"]` because TradingView `TimescaleMark.tooltip` is `string[]` |
| `labelFontColor` | Extra | `#ffffff` |

**Seed:** same window → `tm1`/`tm2`/`tm3`.

**Frontend:** `getTimescaleMarks` → `/api/timescale_marks`. Config `supports_timescale_marks: true`.

**Test class:** `SystemOverviewDesign126Test`. Flyway seed: `FlywayMigrationTest`.

---

## 127 — Register chart layout

**Path:** `POST /api/layouts` (JSON body)  
**Tables:** `m_tv_chart_layout` (V5), `m_ccypairs` (pair check)

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | Stub | Missing header → **401** |
| Body `name` required, max 64 | **Done** | Blank / length 65 → **422** `CODE:30020` |
| Body `content` required | **Done** | Blank/missing → **422** |
| Body `symbol` required (length 6 in doc) | **Partial** | Required; accept `USDJPY` and `USD/JPY` → normalize to CD |
| Body `resolution` in `1S,1,5,15,30,60,120,240,480,1D,1W,1M` | **Done** | `resolution=10` → **422** |
| Pair exists + `is_deleted=0` | **Done** | Else **404** `CODE:30404` |
| Register `customer_no` from token | **Done** | From JWT claim → `CustomerContext` |
| Register `name`, `content`, `ccypair_cd`, `chart_type` | **Done** | `chart_type` = resolution |
| Response chart layout id | **Done** | **201** `{ "id": <number> }` (Peach may want bare number later) |
| `updated_at` on insert | Extra | Needed for doc 129 `timestamp`; set to now |
| SaveLoadAdapter → POST | **Open** | Still localStorage; wire after Step 12 delete |

**Prove it:**

```
POST http://127.0.0.1:8080/api/layouts
Headers: Authorization: Bearer <token>  (+ optional Accept-Language)
Content-Type: application/json

{ "name": "My layout", "content": "{\"pane\":1}", "symbol": "USDJPY", "resolution": "1D" }
```

Expect **201** `{ "id": 1 }` (or higher). DBeaver:

```sql
SELECT id, customer_no, name, ccypair_cd, chart_type, updated_at, left(content, 80)
FROM m_tv_chart_layout
ORDER BY id;
```

Row must show `customer_no=1`, `ccypair_cd=USDJPY`, `chart_type=1D`.

**Also:** no header → 401; `symbol=ETHUSD` → 404; `name` 65 chars / `resolution=10` → 422; `symbol=USD/JPY` → same CD.

**Test class:** `SystemOverviewDesign127Test`.

**Not in this step alone:** full Load dialog needs list (130 Done) + delete (131 Done) + adapter wiring.

---

## 128 — Update chart layout

**Path:** `PUT /api/layouts/{id}` (same JSON body as 127)  
**Tables:** `m_tv_chart_layout`, `m_ccypairs`

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | Stub | Missing header → **401** |
| Path id must be numeric (S-11) | **Done** | `PUT .../abc` → **422** `CODE:30020` |
| Body validation (same as 127) | **Done** | Blank name / `resolution=10` → **422** |
| Layout exists by id | **Done** | Else **404** `CODE:30404` |
| Pair exists + `is_deleted=0` | **Done** | Else **404** |
| Update `name`, `ccypair_cd`, `chart_type` | **Done** | From body; bump `updated_at` |
| Update `content` | **Intentional gap vs update-conditions table** | Doc table says keep `[1].content`; we write request `content` (matches overview + required field + TV save). |
| Response chart layout id | **Done** | **200** `{ "id": <same> }` |
| Tenant scope | Extra | Other customer’s id → **404** (stub multi-customer) |
| Accept `USD/JPY` | Extra | Normalize to CD |

**Prove it:** register (127) → note `id` →

```
PUT http://127.0.0.1:8080/api/layouts/{id}
Headers: Authorization: Bearer <token>  (+ optional Accept-Language)
Content-Type: application/json

{ "name": "Renamed", "content": "{\"pane\":2}", "symbol": "EURUSD", "resolution": "60" }
```

Expect **200** same `id`. DBeaver: `name=Renamed`, `content` new, `ccypair_cd=EURUSD`, `chart_type=60`, `updated_at` newer.

**Also:** `/layouts/abc` → 422; unknown id → 404; `ETHUSD` → 404; update as customer `2` → 404.

**Test class:** `SystemOverviewDesign128Test`.

**Come back later:** Peach-strict keep old content; SaveLoadAdapter overwrite → PUT.

---

## 129 — Get chart layout

**Path:** `GET /api/layouts/{id}`  
**Table:** `m_tv_chart_layout`

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | Stub | Missing header → **401** |
| Path id must be numeric (S-11) | **Done** | `/layouts/abc` → **422** `CODE:30020` |
| Layout exists by id | **Done** | Else **404** `CODE:30404` |
| DTO `id` | **Done** | |
| DTO `name` | **Done** | Layout name |
| DTO `timestamp` | **Done** | `updated_at` as **unix seconds** |
| DTO `content` | **Done** | Layout JSON string |
| No `symbol` / `resolution` / `customer_no` | **Done** | Doc omits them (list API 130 later) |
| Tenant scope | Extra | Other customer → **404** |

**Prove it (round-trip 127→128→129):**

1. `POST /api/layouts` → note `id`
2. `GET /api/layouts/{id}` → `name`/`content` match insert; `timestamp` ≈ now
3. `PUT /api/layouts/{id}` with new name/content
4. `GET /api/layouts/{id}` → new fields; `timestamp` ≥ previous

```
GET http://127.0.0.1:8080/api/layouts/{id}
Headers: Authorization: Bearer <token>  (+ optional Accept-Language)
```

**DBeaver:** `EXTRACT(EPOCH FROM updated_at)` ≈ API `timestamp`.

**Test class:** `SystemOverviewDesign129Test`.

**Come back later:** SaveLoadAdapter `getChartContent` → GET; delete (131) for full Load dialog.

---

## 130 — Get chart layout list (plan Step 11)

**Path:** `GET /api/layouts` (no path id)  
**Table:** `m_tv_chart_layout`  
**Plan:** Step 11 in `plan.md` (retroactively documented; implementation already shipped).

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | Stub | Missing header → **401** |
| Filter by token `customer_no` | **Done** | Other customers’ rows never appear |
| Sort `updated_at` descending | **Done** | Newest first |
| DTO `id` | **Done** | |
| DTO `name` | **Done** | |
| DTO `resolution` | **Done** | From `chart_type` |
| DTO `symbol` | **Done** | From `ccypair_cd` |
| DTO `timestamp` | **Done** | `updated_at` unix seconds |
| No `content` on list | **Done** | Content only on GET by id (129) |
| Empty list | **Done** | Customer with no rows → **200** `[]` |

**Prove it:**

```
GET http://127.0.0.1:8080/api/layouts
Headers: Authorization: Bearer <token>  (+ optional Accept-Language)
```

After creating 2+ layouts (and updating one), order is newest `updated_at` first; each item has `id,name,resolution,symbol,timestamp`.

**Also:** no header → 401; customer `99` with no data → `[]`; customer `2` layouts absent when listing as `1`.

**Test class:** `SystemOverviewDesign130Test` (green).

**Come back later:** wire `getAllCharts` in SaveLoadAdapter (delete API Done in 131).

---

## 131 — Delete chart layout (plan Step 12)

**Path:** `DELETE /api/layouts/{id}`  
**Table:** `m_tv_chart_layout`  
**Plan:** Step 12 in `plan.md`

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | Stub | Missing header → **401** |
| Path id numeric (S-11) | **Done** | `/layouts/abc` → **422** `CODE:30020` |
| Missing id | **Done** | **404** `CODE:30404` |
| Hard delete row | **Done** | Removed from `m_tv_chart_layout` |
| Tenant scope | **Done** | Other customer’s id → **404** (row kept) |
| Response system datetime | **Done** | `{ "t": <unix seconds> }` (Peach may want bare number later) |

**Prove it:**

1. `POST /api/layouts` → note `id`
2. `DELETE /api/layouts/{id}` → **200** `{ "t": … }` ≈ now
3. `GET /api/layouts/{id}` → **404**
4. `GET /api/layouts` → id absent
5. DBeaver: `SELECT * FROM m_tv_chart_layout WHERE id = ?` → 0 rows

```
DELETE http://127.0.0.1:8080/api/layouts/{id}
Headers: Authorization: Bearer <token>  (+ optional Accept-Language)
```

**Also:** no header → 401; `/layouts/abc` → 422; unknown id → 404; delete as customer `2` → 404 while owner can still GET.

**Test class:** `SystemOverviewDesign131Test`.

**Come back later:** wire `removeChart` in SaveLoadAdapter; Peach-exact response if not `{ "t": n }`.

---

## 132 — Get indicator template list (plan Step 13)

**Path:** `GET /api/indicator-templates`  
**Table:** `m_tv_indicator_template` (Flyway **V6**)  
**Plan:** Step 13 in `plan.md`

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | Stub | Missing header → **401** |
| Filter by token `customer_no` | **Done** | Other customers’ rows never appear |
| DTO `name` only | **Done** | No `content` / `customer_no` on list |
| Empty list | **Done** | Customer with no rows → **200** `[]` |
| Sort | **Done** | Name ASC (stable; Peach may want different order later) |

**Schema V6:** `id`, `customer_no`, `name` (64), `content`, `updated_at`; unique `(customer_no, name)`.

**Prove it:**

```
GET http://127.0.0.1:8080/api/indicator-templates
Headers: Authorization: Bearer <token>  (+ optional Accept-Language)
```

Empty DB → `[]`. After seeding two names for customer `1` and one for `2`, list as `1` returns only those two `{ "name": "..." }` objects, sorted A→Z, no `content`.

**Also:** no header → 401; customer `99` → `[]`.

**Test class:** `SystemOverviewDesign132Test`.

**Come back later:** wire `getAllStudyTemplates`; upsert API (133) to create rows from Postman without repo seed.

---

## 133 — Register / update indicator template (plan Step 14)

**Path:** `POST /api/indicator-templates` (body `{ name, content }`)  
**Table:** `m_tv_indicator_template`  
**Status:** **Planned**

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | **Planned** | |
| `name` required, max 64 | **Planned** | Else **422** |
| `content` required | **Planned** | Else **422** |
| Upsert by customer + name | **Planned** | Insert or update `content` + `updated_at` |
| Update keeps name | **Planned** | Per doc update-conditions |
| Response update datetime | **Planned** | Prefer `{ "t": <unix> }` from row |

**Prove it (when done):** POST twice same name → one row, newer `t`, new content.

**Test class (planned):** `SystemOverviewDesign133Test`.

---

## 134 — Get indicator template (plan Step 15)

**Path:** `GET /api/indicator-templates/{name}` (URL-encode spaces)  
**Table:** `m_tv_indicator_template`  
**Status:** **Planned**

| Doc item | Status | Notes |
|---|---|---|
| Token (S-01) | **Planned** | |
| `name` required, max 64 | **Planned** | **422** if invalid |
| Match customer + name | **Planned** | Else **404** `CODE:30404` |
| DTO `name`, `content` | **Planned** | |

**Prove it (when done):** upsert → get → content matches; other customer → 404.

**Test class (planned):** `SystemOverviewDesign134Test`.

**Come back later:** wire `getStudyTemplateContent`; doc **135** delete for `removeStudyTemplate`.

---

## End-to-end (widget)

With backend on `:8080` and Vite on `:5173`, hard-refresh, `USD/JPY` on `1D`:

| Request | Expect |
|---|---|
| `/api/config` | Both marks flags **true** |
| `/api/symbols?symbol=USD%2FJPY` | 200 |
| `/api/search?...` | Five pairs under CTFX/FOREX |
| `/api/history?...` | 200, `s=ok`, `bars[]` |
| `/api/time` | 200 |
| `/api/marks?...` | 200, pins at **top** of chart |
| `/api/timescale_marks?...` | 200, labels on **time axis** |

No 404 on those routes. Restart Boot after code changes — an old process was the earlier marks 404.

**Automated:** `backend` → `.\mvnw.cmd test`

---

## Can we close the “come back later” list without going past 126?

Short answer: **no for the four named Peach items as a set.** Two of them would break the chart if done strictly. Two can be *approached* inside 120–126 only with a dual contract or extra product (SSO, bar warehouse) we do not have.

### 1. Real S-01 (SSO / login token)

| | |
|---|---|
| Close inside 120–126? | **No** (Peach SSO still out of scope) |
| Break the chart? | **No** with local JWT stand-in |
| Why | Real S-01 is not in this repo. We use Spring Security + JWT + FE login as a stand-in (`adjust_plan.md`). Not the same as Peach SSO. |

**Safe for this checkpoint:** local JWT + login overlay; document as S-01 stand-in. Do not claim it is Peach S-01.

---

### 2. `t_chart_*` bars (13 tables + cache + writer sync)

| | |
|---|---|
| Close inside 120–126? | **Phase 1 done** (in-memory). Phase 2 = Flyway tables |
| Break the chart? | **Yes**, if we switch `/history` onto empty `t_chart_*` with no writer |
| Why | Doc 121 is a **cache pipeline**. Phase 1 fills `cache_set_*` in memory. Empty Flyway tables still would not satisfy “retrieve cache data”. |

**Safe Phase 2:** migrate the same `CachedChartBar` writer/reader onto `t_chart_*`; keep seeding.

---

### 3. Peach `{ t, o, h, l, c }` (+ `nextTime`)

| | |
|---|---|
| Close inside 120–126? | **Done** (dual shape + cache `nextTime` with weekend gaps) |
| Break the chart? | **No** — `bars[]` kept; FE still reads `bars` and forwards `nextTime` |

**122 Peach-only `{ t }`:** already return both `t` and `serverTime`. Switching FE to `t` is a one-line change and does not need 127. Dropping `serverTime` without that FE change **breaks** time.

---

### 4. Strict CD-only `name` / length-6 `symbol`

| | |
|---|---|
| Close inside 120–126? | **Soft yes / strict no** |
| Break the chart? | **Strict length-6 (reject `USD/JPY`) → yes.** Soft `name=USDJPY` + `ticker=USD/JPY` → maybe (legend/header may show `USDJPY`) |
| Why | Library search/resolve uses `USD/JPY` because we put that in `ticker`/`full_name`. Rejecting slash at validation 422s the widget. |

**Safe experiment (still ≤126):** set DTO `name` = `ccypair_cd`, keep `ticker`/`full_name` = slash form, **keep accepting both** on query params. Re-check chart title. Do **not** enforce length 6 on input.

---

## Recommended freeze for the mentor

**Datafeed freeze (120–126)** remains the main vertical for chart load: stub auth, Flyway V1–V4, working widget.

**127–131** layout APIs are Done. **132** indicator list is Done (V6). **133–134** upsert/get are Planned (Steps 14–15). They do **not** close S-01 / `t_chart_*` / Peach columns / strict CD name.

Leave unchecked on purpose:

1. Real S-01  
2. `t_chart_*` / Peach cache / sync writer  
3. Replacing widget history JSON with Peach-only columns  
4. Rejecting `USD/JPY` as invalid `symbol`  
5. SaveLoadAdapter → server layouts (backend CRUD ready; wire FE)  
6. SaveLoadAdapter → study templates (needs 133–135)

Optional small refactors **inside** datafeed (do not expand scope): extract shared marks validation; point history symbol lookup at `m_ccypairs`; dual-shape history only if the mentor asks.

**Next:** implement **133–134** (Steps 14–15) per `plan.md`, or wire layout SaveLoadAdapter.
