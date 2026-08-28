# Verification guide — design docs 120–139

Complete overview: **what the MD requires**, **what we implemented**, **how to test** (Postman URLs, expected JSON, DBeaver SQL, automated tests).

**Spec folder:** [`System_Overview_Design/`](System_Overview_Design/)  
**Code map:** [`structure.md`](structure.md)  
**Gap summary:** [`checklist.md`](checklist.md)

---

## Legend

| Status | Meaning |
|--------|---------|
| **Done** | Matches MD for this demo |
| **Extra** | Implemented for TradingView / demo; not in MD table |
| **Partial** | Implemented with intentional difference |
| **Open** | Not wired yet (usually frontend adapter) |
| **Stub** | Local JWT + BCrypt stand-in for Peach S-01 |

**Auth:** All `/api/**` except `GET /api/health`, `POST /api/auth/login`, `POST /api/auth/refresh`, and `POST /api/auth/logout` need `Authorization: Bearer <accessToken>`. The browser app also uses an HttpOnly refresh cookie (see §0.3).

---

## 0. Prerequisites (all docs)

### 0.1 Start services

```powershell
cd "d:\Personal Projects\New\Task"
docker compose up -d
cd backend
.\mvnw.cmd spring-boot:run
```

Wait for Flyway + `AppUserSeedRunner` (demo users).

| Service | Host | Port |
|---------|------|------|
| Spring Boot | `127.0.0.1` | `8080` |
| Postgres | `127.0.0.1` | `5432` |
| Redis | `127.0.0.1` | `6379` (doc 121 boot seed) |
| Vite (UI) | `127.0.0.1` | `5173` |

### 0.2 DBeaver connection

| Field | Value |
|-------|-------|
| Host | `127.0.0.1` |
| Port | `5432` |
| Database | `chart` |
| User | `chart` |
| Password | `chart` |

### 0.3 Login — get token (Postman)

| | |
|---|---|
| Method | `POST` |
| URL | `http://127.0.0.1:8080/api/auth/login` |
| Auth | **No Auth** |
| Header | `Content-Type: application/json` |
| Body | `{"username":"demo","password":"demo"}` |

**Expect `200`:**

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshExpiresIn": 86400
}
```

Copy **`accessToken`**. Use on every call below:

```
Authorization: Bearer eyJ...
```

**Refresh cookie:** Login also sets `Set-Cookie: chart_refresh_token=…; HttpOnly; Path=/; SameSite=Lax`. Postman stores it in the cookie jar automatically. The refresh token is **not** in the JSON body.

| User | Password | `customer_no` |
|------|----------|---------------|
| `demo` | `demo` | `1` |
| `demo2` | `demo2` | `2` |

**Sanity:** `GET http://127.0.0.1:8080/api/config` + Bearer → `200`. No Bearer → `401`.

### 0.3a Refresh and logout (Postman)

**Refresh** (no Bearer; cookie from login required):

| | |
|---|---|
| Method | `POST` |
| URL | `http://127.0.0.1:8080/api/auth/refresh` |
| Auth | **No Auth** (Postman sends cookies from jar) |

**Expect `200`:** new `accessToken`, `expiresIn: 3600`, rotated `Set-Cookie`.

**Logout:**

| | |
|---|---|
| Method | `POST` |
| URL | `http://127.0.0.1:8080/api/auth/logout` |

**Expect `200`** and cookie cleared (`Max-Age=0`). Subsequent refresh → `401` `E_UNAUTHORIZED`.

**Automated:** `AuthLoginTest` covers login cookie, refresh rotation, and logout revoke (requires Redis on `127.0.0.1:6379`).

### 0.4 Run all automated tests (120–139)

```powershell
cd backend
docker compose up -d redis
.\mvnw.cmd "-Dtest=SystemOverviewDesign120Test,SystemOverviewDesign121Test,SystemOverviewDesign122Test,SystemOverviewDesign123Test,SystemOverviewDesign124Test,SystemOverviewDesign125Test,SystemOverviewDesign126Test,SystemOverviewDesign127Test,SystemOverviewDesign128Test,SystemOverviewDesign129Test,SystemOverviewDesign130Test,SystemOverviewDesign131Test,SystemOverviewDesign132Test,SystemOverviewDesign133Test,SystemOverviewDesign134Test,SystemOverviewDesign135Test,SystemOverviewDesign136Test,SystemOverviewDesign137Test,SystemOverviewDesign138Test,SystemOverviewDesign139Test" test
```

**Expect:** BUILD SUCCESS.

### 0.5 Extra — `GET /curpairs` and WebSocket quotes (not in 120–139)

The chart maps live ticks to a pair name using this extra catalog, then the same `m_ccypairs` rows as docs **123** / **124**.

| | |
|---|---|
| **Path** | `GET /curpairs` (**JWT required**) |
| **Table** | `m_ccypairs` (`priority` → `curpairCd`, `ccypair_cd` → `curpairName`) |
| **WS** | `ws://127.0.0.1:5173/ws/fx-quotes` (Vite proxy → Python `:8081`; ticks from Java ingest on Redis) |
| **Test class** | `CurrencyPairControllerTest` |

**Postman:** `GET http://127.0.0.1:8080/curpairs` with `Authorization: Bearer <accessToken>`.

**Expect `401`** without Bearer.

**Expect `200`:**

```json
[
  { "curpairCd": 1, "curpairName": "USDJPY", "curpairDisplay": "USD/JPY" },
  { "curpairCd": 2, "curpairName": "EURJPY", "curpairDisplay": "EUR/JPY" },
  { "curpairCd": 3, "curpairName": "EURUSD", "curpairDisplay": "EUR/USD" },
  { "curpairCd": 4, "curpairName": "GBPUSD", "curpairDisplay": "GBP/USD" },
  { "curpairCd": 5, "curpairName": "AUDUSD", "curpairDisplay": "AUD/USD" }
]
```

WebSocket tick (`curpairCd` is a **string**; field name keeps the spec typo `rateMiliSecondUTC`; ~3/s):

```json
{
  "curpairCd": "1",
  "rateMiliSecondUTC": 1787195533139,
  "bid": 158.456,
  "ask": 158.458,
  "mid": 158.457,
  "high": 158.766,
  "low": 158.036
}
```

Chart UI: `quoteToolbar.ts` loads `/curpairs` with Bearer, ignores unknown WS codes, shows `USD/JPY  BID …  ASK …  MID …` for the active chart symbol.

Docs 123 / 124 are unchanged: `GET /api/symbols` and `GET /api/search` still use 6-char `ccypair_cd` and JWT.

---

## 120 — Get datafeed configuration

**MD:** [`System_Overview_Design_120_Get_Datafeed_Configuration_Data_(TV).md`](System_Overview_Design/System_Overview_Design_120_Get_Datafeed_Configuration_Data_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/config` |
| **Tables** | none (yml `app.tradingview.*`) |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign120Test` |
| **Key code** | `ChartDataController.config` → `ChartDataServiceImpl.config` → `DatafeedConfigResponse` |

### What MD requires vs app

| MD field | Status | App value |
|----------|--------|-----------|
| `supports_search` | Done | `true` |
| `supports_marks` | Done | `true` |
| `supports_timescale_marks` | Done | `true` |
| `supports_time` | Done | `true` |
| `exchanges` | Done | `[{ "value":"CTFX", "name":"CTFX", "desc":"CTFX" }]` |
| `symbols_types` | Done | `[{ "name":"FOREX", "value":"FOREX" }]` |
| `supported_resolutions` | Done | 12: `1S,1,5,15,30,60,120,240,480,1D,1W,1M` |
| Token (S-01) | Stub | JWT required on `/api/**` |
| `supports_group_request` | **Extra** | `false` — forces widget to use `/search` |

### Postman

```
GET http://127.0.0.1:8080/api/config
Authorization: Bearer <token>
```

**Expect `200`:** all four `supports_*` flags `true`; `supported_resolutions` length **12**; `exchanges[0].value` = `CTFX`; `symbols_types[0].value` = `FOREX`.

No Bearer → **401**.

### DBeaver

Not applicable (no table).

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign120Test" test
```

### UI

Login → chart loads → Network shows `/api/config` **200** on widget `onReady`.

---

## 121 — Get bars

**MD:** [`System_Overview_Design_121_Get_Bars_(TV).md`](System_Overview_Design/System_Overview_Design_121_Get_Bars_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/history` |
| **Tables** | V8: `t_chart_1` … `t_chart_month` (13) |
| **Cache** | Redis `peach:cache_set_*:{CD}` |
| **Implemented?** | **Yes** |
| **Test classes** | `SystemOverviewDesign121Test`, `FlywayMigrationTest` |
| **Key code** | `ChartDataController.history` → `ChartDataServiceImpl.history`; `ChartCacheWriter`, `ChartBarRepository`, `ChartCacheStore`, `MockBarGenerator` |

### What MD requires vs app

| MD rule | Status | Notes |
|---------|--------|-------|
| `bid_ask` required (`BID`/`MID`/`ASK`) | Done | else 422 |
| `symbol` length 6 after normalize | Done | |
| `resolution` incl. `10` | Done | maps to chart_type / table |
| `from`/`to` paired, `to >= from` | Done | |
| 13 warehouse tables + cache | Done | V8 + Redis |
| Response `{ s, t[], o[], h[], l[], c[] }` | Done | **Extra** `bars[]` for widget |
| BID/ASK/MID from bid_/ask_ columns | Done | |
| `no_data` + `nextTime` | Done | |
| Bar data source | **Mock** | `MockBarGenerator` seeds DB+Redis, not live Peach |

### Postman

```powershell
$to = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$from = $to - (20 * 86400)
```

```
GET http://127.0.0.1:8080/api/history?symbol=USDJPY&resolution=1D&from=$from&to=$to&bid_ask=MID
Authorization: Bearer <token>
```

**Expect `200`:** `"s":"ok"`; `bars` array non-empty; `t`/`o`/`h`/`l`/`c` arrays same length; times ascending.

Missing `bid_ask` → **422**. Unknown symbol → `{ "s":"error" }` (UDF, not 404).

### DBeaver

```sql
SELECT COUNT(*) AS bar_count
FROM t_chart_day
WHERE curpair_cd = 'USDJPY';
```

**Expect:** `bar_count > 0` after app boot (cache writer seed).

```sql
SELECT curpair_cd, chart_datetime, bid_close, ask_close, volume
FROM t_chart_day
WHERE curpair_cd = 'USDJPY'
ORDER BY chart_datetime DESC
LIMIT 5;
```

### Redis (optional)

```powershell
docker compose exec redis redis-cli ZCARD peach:cache_set_day:USDJPY
```

**Expect:** count > 0.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign121Test,FlywayMigrationTest" test
```

---

## 122 — Get server time

**MD:** [`System_Overview_Design_122_Get_Server_Time_(TV).md`](System_Overview_Design/System_Overview_Design_122_Get_Server_Time_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/time` |
| **Tables** | none |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign122Test` |
| **Key code** | `ChartDataController.time` → `ServerTimeResponse` |

### What MD requires vs app

| MD field | Status | Notes |
|----------|--------|-------|
| `t` = unix seconds, no ms | Done | |
| `serverTime` | **Extra** | same value; FE reads this |
| Auth in MD | not specified | JWT required |

### Postman

```
GET http://127.0.0.1:8080/api/time
Authorization: Bearer <token>
```

**Expect `200`:**

```json
{ "t": 172..., "serverTime": 172... }
```

`t` === `serverTime`; both within a few seconds of now; both < `10000000000` (seconds not ms).

### DBeaver

Not applicable.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign122Test" test
```

---

## 123 — Get symbol information

**MD:** [`System_Overview_Design_123_Get_Symbol_Information_(TV).md`](System_Overview_Design/System_Overview_Design_123_Get_Symbol_Information_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/symbols?symbol=` |
| **Tables** | V1 `m_ccypairs`, V2 `m_season` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign123Test` |
| **Key code** | `ChartDataController.symbols` → `ChartDataServiceImpl.resolve` → `SymbolInfoDto` |

### What MD requires vs app

| MD rule | Status | Notes |
|---------|--------|-------|
| `symbol` required, length 6 after normalize | Done | blank / wrong length → 422 |
| Active pair `is_deleted=0` | Done | else 404 |
| `name` = `ccypair_cd` | Done | |
| `description` = `ccypair_jp` | Done | |
| `pricescale` = 10^`rate_unit` | Done | USDJPY → 1000 |
| Config fields (timezone, exchange, type, session, …) | Done | from yml + season |
| `ticker`, library extras | **Extra** | slash display for widget |

### Postman

```
GET http://127.0.0.1:8080/api/symbols?symbol=USDJPY
Authorization: Bearer <token>
```

**Expect `200`:**

| Field | Value |
|-------|-------|
| `name` | `USDJPY` |
| `ticker` | `USD/JPY` |
| `description` | `米ドル/円` |
| `pricescale` | `1000` |
| `exchange` | `CTFX` |
| `type` | `FOREX` |

Also: `symbol=USD/JPY` → **200** same `name`. `symbol=ETH` → **422**. `symbol=ETHUSD` → **404**.

### DBeaver

```sql
SELECT ccypair_cd, ccypair_jp, rate_unit, is_deleted
FROM m_ccypairs
WHERE ccypair_cd = 'USDJPY';
```

**Expect:** one row, `rate_unit=3`, `is_deleted=0`, `ccypair_jp=米ドル/円`.

```sql
SELECT season_cd, start_at, end_at
FROM m_season
WHERE start_at <= NOW() AND end_at >= NOW();
```

**Expect:** at least one row (default seed: `season_cd=2` standard time).

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign123Test" test
```

---

## 124 — Get symbol list (search)

**MD:** [`System_Overview_Design_124_Get_Symbol_List_(TV).md`](System_Overview_Design/System_Overview_Design_124_Get_Symbol_List_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/search` |
| **Table** | V1 `m_ccypairs` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign124Test` |
| **Key code** | `ChartDataController.search` → `CcypairRepository.searchActive` → `SearchSymbolDto` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| `query` optional, max 10 → 422 | Done |
| `limit` default 100, range 1–100 → 422 | Done |
| Match CD or JP partial, `is_deleted=0`, sort `priority` ASC | Done |
| DTO `symbol`, `description`, `type`, `exchange` | Done |
| `ticker`, `full_name` | **Extra** |
| `exchange`/`type` query filters | **Extra** |

### Postman

**All pairs:**

```
GET http://127.0.0.1:8080/api/search
Authorization: Bearer <token>
```

**Expect `200`:** array length **5**, order: `USDJPY`, `EURJPY`, `EURUSD`, `GBPUSD`, `AUDUSD`.

**Filter:**

```
GET http://127.0.0.1:8080/api/search?query=USD
GET http://127.0.0.1:8080/api/search?query=%E5%86%86
GET http://127.0.0.1:8080/api/search?query=USD/JPY
```

**Errors:**

```
GET http://127.0.0.1:8080/api/search?query=ABCDEFGHIJK   → 422 CODE:30020
GET http://127.0.0.1:8080/api/search?limit=0             → 422
GET http://127.0.0.1:8080/api/search                     → 401 (no Bearer)
```

### DBeaver

```sql
SELECT ccypair_cd, ccypair_jp, is_deleted, priority
FROM m_ccypairs
WHERE is_deleted = 0
ORDER BY priority ASC;
```

**Expect:** 5 rows matching Postman order above.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign124Test" test
```

---

## 125 — Get marks list

**MD:** [`System_Overview_Design_125_Get_Marks_List_(TV).md`](System_Overview_Design/System_Overview_Design_125_Get_Marks_List_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/marks` |
| **Table** | V3 `m_tv_mark` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign125Test` |
| **Key code** | `ChartDataController.marks` → `MarkDto` |

**Fixed seed window:** `from=1787011200`, `to=1787270400` (2026-08-18 – 2026-08-21 UTC).

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| `symbol`, `resolution`, `from`, `to` required | Done |
| `resolution` in marks list (no `10`) | Done |
| `to >= from` | Done |
| Filter CD + resolution + time range | Done |
| DTO `id`, `time`, `color`, `label`, `text` | Done |
| `labelFontColor`, `minSize` | **Extra** |
| `symbol` length 6 → 422 | **Partial** (blank only; bad CD → `[]`) |

### Postman

```
GET http://127.0.0.1:8080/api/marks?symbol=USDJPY&resolution=1D&from=1787011200&to=1787270400
Authorization: Bearer <token>
```

**Expect `200`**, array length **3**:

| id | time | color | label | text |
|----|------|-------|-------|------|
| m1 | 1787011200 | green | B | Buy signal |
| m2 | 1787097600 | red | S | Sell signal |
| m3 | 1787184000 | green | B | Buy signal follow-up |

**Edge cases:**

```
from=1&to=2                              → 200 []
resolution=60 (same window)              → 200 []
resolution=10                            → 422
to < from                                → 422
missing symbol                           → 422
```

### DBeaver

```sql
SELECT id, ccypair_cd, resolution, mark_at, color, label, mark_text
FROM m_tv_mark
WHERE ccypair_cd = 'USDJPY' AND resolution = '1D'
  AND mark_at BETWEEN 1787011200 AND 1787270400
ORDER BY mark_at ASC;
```

**Expect:** 3 rows (`m1`, `m2`, `m3`).

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign125Test" test
```

### UI

Chart **USD/JPY 1D**, zoom to **2026-08-18 – 21** → mark pins at **top** of chart.

---

## 126 — Get timescale marks list

**MD:** [`System_Overview_Design_126_Get_Timescale_Marks_List_(TV).md`](System_Overview_Design/System_Overview_Design_126_Get_Timescale_Marks_List_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/timescale_marks` |
| **Table** | V4 `m_tv_timescale_mark` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign126Test` |
| **Key code** | `ChartDataController.timescaleMarks` → `TimescaleMarkDto` |

Same query params and validation as **125**. Same seed window.

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Same validation as 125 | Done |
| DTO `id`, `color`, `label`, `time`, `tooltip` | Done |
| `tooltip` as string in MD | **Partial** — JSON returns `["…"]` (TV type) |
| `labelFontColor` | **Extra** |

### Postman

```
GET http://127.0.0.1:8080/api/timescale_marks?symbol=USDJPY&resolution=1D&from=1787011200&to=1787270400
Authorization: Bearer <token>
```

**Expect `200`**, array length **3**:

| id | time | color | label | tooltip |
|----|------|-------|-------|---------|
| tm1 | 1787011200 | rgba(255, 99, 71, 0.2) | B | ["Buy event"] |
| tm2 | 1787097600 | rgba(70, 130, 180, 0.3) | S | ["Sell event"] |
| tm3 | 1787184000 | green | N | ["News note"] |

### DBeaver

```sql
SELECT id, ccypair_cd, resolution, timescale_mark_at, color, label, tooltip
FROM m_tv_timescale_mark
WHERE ccypair_cd = 'USDJPY' AND resolution = '1D'
  AND timescale_mark_at BETWEEN 1787011200 AND 1787270400
ORDER BY timescale_mark_at ASC;
```

**Expect:** 3 rows (`tm1`, `tm2`, `tm3`).

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign126Test" test
```

### UI

Same date range as 125 → labels on **time axis**.

---

## 127 — Register chart layout

**MD:** [`System_Overview_Design_127_Register_Chart_Layout_(TV).md`](System_Overview_Design/System_Overview_Design_127_Register_Chart_Layout_(TV).md)

| | |
|---|---|
| **Path** | `POST /api/layouts` |
| **Tables** | V5 `m_tv_chart_layout`, V1 `m_ccypairs` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign127Test` |
| **Key code** | `ChartLayoutController.register` → `ChartLayoutServiceImpl.register` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Body: `name` (max 64), `content`, `symbol`, `resolution` | Done |
| Active pair check | Done → 404 |
| Register `customer_no`, `name`, `content`, `ccypair_cd`, `chart_type` | Done |
| Return layout id | Done → **201** `{ "id": n }` |
| SaveLoadAdapter | **Done** — `ServerSaveLoadAdapter` → REST / Postgres |
| `symbol` strict length 6 | **Partial** — slash accepted |

### Postman

```
POST http://127.0.0.1:8080/api/layouts
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "name": "My layout",
  "content": "{\"pane\":1}",
  "symbol": "USDJPY",
  "resolution": "1D"
}
```

**Expect `201`:** `{ "id": 1 }` (note the id).

**Errors:** no Bearer → 401; `ETHUSD` → 404; `resolution=10` → 422; name 65 chars → 422.

### DBeaver (after POST)

```sql
SELECT id, customer_no, name, content, ccypair_cd, chart_type, updated_at
FROM m_tv_chart_layout
WHERE id = 1;
```

**Expect:**

| Column | Value |
|--------|-------|
| customer_no | 1 |
| name | My layout |
| content | {"pane":1} |
| ccypair_cd | USDJPY |
| chart_type | 1D |

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign127Test" test
```

---

## 128 — Update chart layout

**MD:** [`System_Overview_Design_128_Update_Chart_Layout_(TV).md`](System_Overview_Design/System_Overview_Design_128_Update_Chart_Layout_(TV).md)

| | |
|---|---|
| **Path** | `PUT /api/layouts/{id}` |
| **Tables** | V5 `m_tv_chart_layout`, V1 `m_ccypairs` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign128Test` |
| **Key code** | `ChartLayoutController.update` → `ChartLayoutServiceImpl.update` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Path id numeric (S-11) | Done → 422 |
| Body validation (same as 127) | Done |
| Layout exists | Done → 404 |
| Update `name`, `ccypair_cd`, `chart_type` | Done |
| Update `content` per update-conditions | **Partial** — MD says keep old content; **app overwrites** (TV save) |
| Return layout id | Done → **200** `{ "id": same }` |
| Tenant: other customer | **Extra** → 404 |

### Postman

Use `id` from doc 127 (example `1`):

```
PUT http://127.0.0.1:8080/api/layouts/1
Authorization: Bearer <token>
Content-Type: application/json
```

Body:

```json
{
  "name": "Renamed",
  "content": "{\"pane\":2}",
  "symbol": "EURUSD",
  "resolution": "60"
}
```

**Expect `200`:** `{ "id": 1 }`.

**Errors:** `PUT .../abc` → 422; `999999` → 404; login as `demo2` on demo's layout → 404.

### DBeaver (after PUT)

```sql
SELECT id, customer_no, name, content, ccypair_cd, chart_type,
       updated_at,
       EXTRACT(EPOCH FROM updated_at)::bigint AS timestamp_unix
FROM m_tv_chart_layout
WHERE id = 1;
```

**Expect:** `name=Renamed`, `content={"pane":2}`, `ccypair_cd=EURUSD`, `chart_type=60`, `updated_at` newer than before PUT. `timestamp_unix` ≈ API `timestamp` on GET (doc 129).

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign128Test" test
```

---

## 129 — Get chart layout

**MD:** [`System_Overview_Design_129_Get_Chart_Layout_(TV).md`](System_Overview_Design/System_Overview_Design_129_Get_Chart_Layout_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/layouts/{id}` |
| **Table** | V5 `m_tv_chart_layout` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign129Test` |
| **Key code** | `ChartLayoutController.get` → `ChartLayoutDto` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Token | Stub (JWT) |
| Path id numeric | Done → 422 |
| Layout exists by id | Done → 404 |
| DTO `id`, `name`, `timestamp`, `content` | Done |
| No `symbol` / `resolution` / `customer_no` on GET | Done |
| Tenant: other customer | **Extra** → 404 |
| SaveLoadAdapter `getChartContent` | **Done** |

### Postman — full round-trip (127 → 128 → 129)

**Step A — register (127):**

```
POST http://127.0.0.1:8080/api/layouts
Authorization: Bearer <token>
Content-Type: application/json

{"name":"RoundTrip","content":"{\"pane\":1}","symbol":"USDJPY","resolution":"1D"}
```

Note `id` from **201** response (e.g. `5`).

**Step B — get after register:**

```
GET http://127.0.0.1:8080/api/layouts/5
Authorization: Bearer <token>
```

**Expect `200`:**

```json
{
  "id": 5,
  "name": "RoundTrip",
  "timestamp": 172...,
  "content": "{\"pane\":1}"
}
```

Must **not** include `symbol`, `resolution`, `customer_no`.

**Step C — update (128):**

```
PUT http://127.0.0.1:8080/api/layouts/5
Authorization: Bearer <token>
Content-Type: application/json

{"name":"RoundTripUpdated","content":"{\"pane\":99}","symbol":"EURUSD","resolution":"60"}
```

**Step D — get after update:**

```
GET http://127.0.0.1:8080/api/layouts/5
Authorization: Bearer <token>
```

**Expect `200`:** `name=RoundTripUpdated`, `content={"pane":99}`, `timestamp` **≥** step B timestamp.

**Errors:**

```
GET http://127.0.0.1:8080/api/layouts/abc     → 422 CODE:30020
GET http://127.0.0.1:8080/api/layouts/999999  → 404 CODE:30404
GET .../5  with demo2 token (demo owns row)   → 404
GET .../5  without Bearer                       → 401
```

### DBeaver

After step D:

```sql
SELECT id, name, content,
       EXTRACT(EPOCH FROM updated_at)::bigint AS timestamp_unix
FROM m_tv_chart_layout
WHERE id = 5;
```

**Expect:** API `timestamp` ≈ `timestamp_unix` (within 1–2 seconds). `name` and `content` match PUT body.

Compare list vs get — list has **no** `content` (doc 130):

```sql
SELECT id, name, ccypair_cd AS symbol, chart_type AS resolution,
       EXTRACT(EPOCH FROM updated_at)::bigint AS timestamp
FROM m_tv_chart_layout
WHERE customer_no = 1 AND id = 5;
```

GET response `timestamp` should match SQL `timestamp`.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign129Test" test
```

---

## 130 — Get chart layout list

**MD:** [`System_Overview_Design_130_Get_Chart_Layout_List_(TV).md`](System_Overview_Design/System_Overview_Design_130_Get_Chart_Layout_List_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/layouts` (no `{id}`) |
| **Table** | V5 `m_tv_chart_layout` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign130Test` |
| **Key code** | `ChartLayoutController.list` → `ChartLayoutListItemDto` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Filter by token `customer_no` | Done |
| Sort `updated_at` DESC | Done |
| DTO `id`, `name`, `resolution`, `symbol`, `timestamp` | Done |
| No `content` on list | Done |
| Empty → `200 []` | Done |
| SaveLoadAdapter `getAllCharts` | **Done** |

### Postman — setup then list

**Create two layouts as `demo`:**

```
POST http://127.0.0.1:8080/api/layouts
{"name":"Layout A","content":"{\"a\":1}","symbol":"USDJPY","resolution":"1D"}

POST http://127.0.0.1:8080/api/layouts
{"name":"Layout B","content":"{\"b\":2}","symbol":"EURUSD","resolution":"60"}
```

Wait 1 second, then **update A** (moves it to front):

```
PUT http://127.0.0.1:8080/api/layouts/{idOfA}
{"name":"Layout A updated","content":"{\"a\":9}","symbol":"GBPUSD","resolution":"15"}
```

**List:**

```
GET http://127.0.0.1:8080/api/layouts
Authorization: Bearer <token>
```

**Expect `200`:** JSON array; **first item** = updated Layout A (`GBPUSD`, `15`); each item has:

```json
{
  "id": 1,
  "name": "...",
  "resolution": "1D",
  "symbol": "USDJPY",
  "timestamp": 172...
}
```

Must **not** have `content`.

**Tenant test:** login as `demo2`, list → must **not** contain `demo`'s layout names.

**Empty test:** use a user with no layouts (or fresh DB) → **200** `[]`.

**Errors:** no Bearer → **401**.

### DBeaver

```sql
SELECT id, customer_no, name, ccypair_cd AS symbol, chart_type AS resolution,
       EXTRACT(EPOCH FROM updated_at)::bigint AS timestamp
FROM m_tv_chart_layout
WHERE customer_no = 1
ORDER BY updated_at DESC;
```

**Expect:** same order and field values as Postman list (newest first). No `content` column in API — only in DB.

Verify tenant isolation:

```sql
SELECT id, customer_no, name FROM m_tv_chart_layout ORDER BY customer_no, id;
```

Customer `2` rows must not appear when listing as `demo` (customer `1`).

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign130Test" test
```

---

## 131 — Delete chart layout

**MD:** [`System_Overview_Design_131_Delete_Chart_Layout_(TV).md`](System_Overview_Design/System_Overview_Design_131_Delete_Chart_Layout_(TV).md)

| | |
|---|---|
| **Path** | `DELETE /api/layouts/{id}` |
| **Table** | V5 `m_tv_chart_layout` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign131Test` |
| **Key code** | `ChartLayoutController.delete` → `SystemDatetimeResponse` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Path id numeric | Done → 422 |
| Layout exists | Done → 404 |
| Hard delete row | Done |
| Response system datetime | Done → `{ "t": unixSeconds }` |
| Tenant: other customer | Done → 404, row kept |
| SaveLoadAdapter `removeChart` | **Done** |
| Bare number vs `{t}` | **Extra wrapper** |

### Postman — full delete flow

**Step A — create layout to delete:**

```
POST http://127.0.0.1:8080/api/layouts
Authorization: Bearer <token>
Content-Type: application/json

{"name":"ToDelete","content":"{\"pane\":1}","symbol":"USDJPY","resolution":"1D"}
```

Note `id` (e.g. `7`).

**Step B — delete:**

```
DELETE http://127.0.0.1:8080/api/layouts/7
Authorization: Bearer <token>
```

**Expect `200`:**

```json
{ "t": 172... }
```

`t` within a few seconds of now.

**Step C — confirm gone:**

```
GET http://127.0.0.1:8080/api/layouts/7
Authorization: Bearer <token>
```

**Expect `404`** `CODE:30404`.

```
GET http://127.0.0.1:8080/api/layouts
Authorization: Bearer <token>
```

**Expect:** array does **not** contain `id: 7`.

**Errors:**

```
DELETE .../abc        → 422
DELETE .../999999     → 404
DELETE .../7 as demo2 → 404 (row remains for demo)
DELETE .../7 no Bearer → 401
```

### DBeaver

Before delete:

```sql
SELECT COUNT(*) FROM m_tv_chart_layout WHERE id = 7;
```

**Expect:** `1`.

After delete:

```sql
SELECT COUNT(*) FROM m_tv_chart_layout WHERE id = 7;
```

**Expect:** `0`.

Full table check:

```sql
SELECT id, customer_no, name FROM m_tv_chart_layout ORDER BY id;
```

Deleted id must be absent.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign131Test" test
```

---

## 132 — Get indicator template list

**MD:** [`System_Overview_Design_132_Get_Indicator_Template_List_(TV).md`](System_Overview_Design/System_Overview_Design_132_Get_Indicator_Template_List_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/indicator-templates` |
| **Table** | V6 `m_tv_indicator_template` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign132Test` |
| **Key code** | `IndicatorTemplateController.list` → `IndicatorTemplateServiceImpl` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Filter by token `customer_no` | Done |
| DTO `name` only | Done |
| Empty → `200 []` | Done |
| Sort order in MD | not specified — app uses **name ASC** |
| SaveLoadAdapter `getAllStudyTemplates` | **Done** |

### Postman — seed via 133 or DBeaver

Prefer **POST /api/indicator-templates** (doc 133). Flyway V6 starts empty; DBeaver INSERT also works:

```sql
INSERT INTO m_tv_indicator_template (customer_no, name, content, updated_at) VALUES
  (1, 'Alpha', '{"study":1}', NOW()),
  (1, 'Zulu', '{"study":2}', NOW()),
  (2, 'Other', '{"study":3}', NOW());
```

**List as demo (customer 1):**

```
GET http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
```

**Expect `200`:**

```json
[
  { "name": "Alpha" },
  { "name": "Zulu" }
]
```

Sorted A→Z. **No** `content`, **no** `customer_no`. Must **not** include `Other`.

**List as demo2:**

Login as `demo2` / `demo2`, same GET → only `[{ "name": "Other" }]`.

**Empty customer:**

Fresh DB with no rows for customer 99 → **200** `[]`.

**Errors:** no Bearer → **401**.

### DBeaver — verify source

```sql
SELECT id, customer_no, name, content, updated_at
FROM m_tv_indicator_template
WHERE customer_no = 1
ORDER BY name ASC;
```

**Expect:** rows for `Alpha`, `Zulu` only; `content` present in DB but **not** returned by API.

Tenant check:

```sql
SELECT customer_no, name FROM m_tv_indicator_template ORDER BY customer_no, name;
```

### Automated

Test seeds its own rows in `@Transactional` tests:

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign132Test" test
```

---

## 133 — Register / update indicator template

**MD:** [`System_Overview_Design_133_Register_Update_Indicator_Template_(TV).md`](System_Overview_Design/System_Overview_Design_133_Register_Update_Indicator_Template_(TV).md)

| | |
|---|---|
| **Path** | `POST /api/indicator-templates` |
| **Table** | V6 `m_tv_indicator_template` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign133Test` |
| **Key code** | `IndicatorTemplateController.upsert` → `IndicatorTemplateServiceImpl.upsert` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Token | Stub (JWT) |
| Body `name` required, max 64 | Done → 422 `CODE:30020` |
| Body `content` required | Done → 422 `CODE:30020` |
| Lookup by token `customer_no` + `name` | Done |
| Found → update `content` only | Done (`name` / `customer_no` unchanged) |
| Miss → register `customer_no`, `name`, `content` | Done |
| Return update datetime of the row | Done → **200** `{ "t": unix }` from `updated_at` |
| HTTP 201 on first insert | **Extra** — app always **200** (upsert) |
| SaveLoadAdapter `saveStudyTemplate` | **Done** |

Unique key: `(customer_no, name)`. Same name for `demo2` is a **different** row.

### Postman — register then update (same name)

**Step A — first POST (register):**

```
POST http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My RSI","content":"{\"studies\":[]}"}
```

**Expect `200`:**

```json
{ "t": 172... }
```

`t` within a few seconds of now. Note this `t` (call it `t1`).

**Step B — second POST (update content only):**

Wait 1 second, then:

```
POST http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My RSI","content":"{\"studies\":[1]}"}
```

**Expect `200`:** `{ "t": t2 }` with `t2` ≥ `t1`. Still **one** name in the list (doc 132).

**Step C — tenant (optional):** login as `demo2` / `demo2`, POST the same body `{"name":"My RSI",...}` → **200** and a **second** DB row (`customer_no = 2`).

**Errors:**

```
POST http://127.0.0.1:8080/api/indicator-templates
  (no Bearer)                                              → 401

POST http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
Content-Type: application/json
{"name":"","content":"{}"}                                 → 422 CODE:30020

POST ...  {"name":"<65 A characters>","content":"{}"}      → 422 CODE:30020

POST ...  {"name":"RSI","content":""}                      → 422 CODE:30020
```

### DBeaver (after step B)

```sql
SELECT id, customer_no, name, content,
       EXTRACT(EPOCH FROM updated_at)::bigint AS t
FROM m_tv_indicator_template
WHERE customer_no = 1 AND name = 'My RSI';
```

**Expect:**

| Column | Value |
|--------|-------|
| customer_no | 1 |
| name | My RSI |
| content | {"studies":[1]} |
| t | ≈ API `t` from step B |

Row **count** for that customer + name = **1** (not 2). `id` same as after step A.

Tenant check after step C:

```sql
SELECT customer_no, name, content
FROM m_tv_indicator_template
WHERE name = 'My RSI'
ORDER BY customer_no;
```

**Expect:** two rows (`1` and `2`) if you posted as both users.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign133Test" test
```

---

## 134 — Get indicator template

**MD:** [`System_Overview_Design_134_Get_Indicator_Template_(TV).md`](System_Overview_Design/System_Overview_Design_134_Get_Indicator_Template_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/indicator-templates/{name}` |
| **Table** | V6 `m_tv_indicator_template` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign134Test` |
| **Key code** | `IndicatorTemplateController.get` → `IndicatorTemplateDto` |

URL-encode spaces: `My RSI` → `My%20RSI`.

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Token | Stub (JWT) |
| Path `name` required, max 64 | Done → 422 `CODE:30020` |
| Match token `customer_no` + `name` | Done → else 404 `CODE:30404` |
| DTO `name`, `content` | Done |
| No extra fields (`customer_no`, timestamp) | Done |
| Other customer | **Extra** → 404 |
| SaveLoadAdapter `getStudyTemplateContent` | **Done** |

### Postman — full round-trip (133 → 134 → 133 → 134)

**Step A — register (133):**

```
POST http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My RSI","content":"{\"studies\":[]}"}
```

**Step B — get after register:**

```
GET http://127.0.0.1:8080/api/indicator-templates/My%20RSI
Authorization: Bearer <token>
```

**Expect `200`:**

```json
{
  "name": "My RSI",
  "content": "{\"studies\":[]}"
}
```

Must **not** include `customer_no`, `id`, or `timestamp`.

**Step C — update content (133):**

```
POST http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My RSI","content":"{\"studies\":[1]}"}
```

**Step D — get after update:**

```
GET http://127.0.0.1:8080/api/indicator-templates/My%20RSI
Authorization: Bearer <token>
```

**Expect `200`:** `name=My RSI`, `content={"studies":[1]}`.

**Errors:**

```
GET http://127.0.0.1:8080/api/indicator-templates/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
  (65 × A)                                                         → 422 CODE:30020

GET http://127.0.0.1:8080/api/indicator-templates/Missing
Authorization: Bearer <token>                                      → 404 CODE:30404

GET http://127.0.0.1:8080/api/indicator-templates/My%20RSI
Authorization: Bearer <demo2 token>  (demo owns the row)           → 404 CODE:30404

GET http://127.0.0.1:8080/api/indicator-templates/My%20RSI
  (no Bearer)                                                      → 401
```

Compare list vs get — list (doc 132) has **no** `content`:

```
GET http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
```

**Expect:** `{ "name": "My RSI" }` in the array, no `content`. GET-by-name is the only call that returns `content`.

### DBeaver

After step D:

```sql
SELECT name, content
FROM m_tv_indicator_template
WHERE customer_no = 1 AND name = 'My RSI';
```

**Expect:** API `name` and `content` match this row.

```sql
SELECT customer_no, name, content
FROM m_tv_indicator_template
WHERE name = 'My RSI'
ORDER BY customer_no;
```

Customer `2` must not appear in GET when using the `demo` token.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign134Test" test
```

---

## 135 — Delete indicator template

**MD:** [`System_Overview_Design_135_Delete_Indicator_Template_(TV).md`](System_Overview_Design/System_Overview_Design_135_Delete_Indicator_Template_(TV).md)

| | |
|---|---|
| **Path** | `DELETE /api/indicator-templates/{name}` |
| **Table** | V6 `m_tv_indicator_template` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign135Test` |
| **Key code** | `IndicatorTemplateController.delete` → `SystemDatetimeResponse` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Token | Stub (JWT) |
| Path `name` required, max 64 | Done → 422 `CODE:30020` |
| Template exists for this customer | Done → 404 `CODE:30404` |
| Hard delete row | Done |
| Return system datetime | Done → `{ "t": unix }` (**now**, not row `updated_at`) |
| Other customer | Done → 404, row **kept** |
| SaveLoadAdapter `removeStudyTemplate` | **Done** |
| Bare number vs `{t}` | **Extra wrapper** (same as 131) |

### Postman — full delete flow

**Step A — create template to delete (133):**

```
POST http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"ToDelete","content":"{\"pane\":1}"}
```

**Expect `200`:** `{ "t": … }`.

**Step B — delete:**

```
DELETE http://127.0.0.1:8080/api/indicator-templates/ToDelete
Authorization: Bearer <token>
```

**Expect `200`:**

```json
{ "t": 172... }
```

`t` within a few seconds of now.

**Step C — confirm gone (134 + 132):**

```
GET http://127.0.0.1:8080/api/indicator-templates/ToDelete
Authorization: Bearer <token>
```

**Expect `404`** `CODE:30404`.

```
GET http://127.0.0.1:8080/api/indicator-templates
Authorization: Bearer <token>
```

**Expect:** array does **not** contain `{ "name": "ToDelete" }`.

**Errors:**

```
DELETE http://127.0.0.1:8080/api/indicator-templates/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
  (65 × A)                                                         → 422 CODE:30020

DELETE http://127.0.0.1:8080/api/indicator-templates/Missing
Authorization: Bearer <token>                                      → 404 CODE:30404

DELETE http://127.0.0.1:8080/api/indicator-templates/ToDelete
Authorization: Bearer <demo2 token>  (demo owns the row)           → 404 (row remains for demo)

DELETE http://127.0.0.1:8080/api/indicator-templates/ToDelete
  (no Bearer)                                                      → 401
```

### DBeaver

Before delete:

```sql
SELECT COUNT(*) FROM m_tv_indicator_template
WHERE customer_no = 1 AND name = 'ToDelete';
```

**Expect:** `1`.

After delete:

```sql
SELECT COUNT(*) FROM m_tv_indicator_template
WHERE customer_no = 1 AND name = 'ToDelete';
```

**Expect:** `0`.

Full table check:

```sql
SELECT id, customer_no, name FROM m_tv_indicator_template ORDER BY customer_no, name;
```

Deleted name for customer `1` must be absent. If step “other customer” ran, `demo`’s row is still there until `demo` deletes it.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign135Test" test
```

---

## 136 — Get chart template list

**MD:** [`System_Overview_Design_136_Get_Chart_Template_List_(TV).md`](System_Overview_Design/System_Overview_Design_136_Get_Chart_Template_List_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/chart-templates` |
| **Table** | V9 `m_tv_chart_templates` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign136Test` |
| **Key code** | `ChartTemplateController.list` → `ChartTemplateServiceImpl` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Token | Stub (JWT) |
| Filter by token `customer_no` | Done |
| DTO `name` only | Done |
| Empty → `200 []` | Done |
| Sort order in MD | not specified — app uses **name ASC** |
| SaveLoadAdapter chart templates | **Done** |

### Postman — seed via 137 or DBeaver

Prefer **POST /api/chart-templates** (doc 137). Flyway V9 starts empty; DBeaver INSERT also works:

```sql
INSERT INTO m_tv_chart_templates (customer_no, name, content, updated_at) VALUES
  (1, 'Alpha', '{"theme":"a"}', NOW()),
  (1, 'Zulu', '{"theme":"z"}', NOW()),
  (2, 'Other', '{"theme":"o"}', NOW());
```

**List as demo (customer 1):**

```
GET http://127.0.0.1:8080/api/chart-templates
Authorization: Bearer <token>
```

**Expect `200`:**

```json
[
  { "name": "Alpha" },
  { "name": "Zulu" }
]
```

Sorted A→Z. **No** `content`, **no** `customer_no`. Must **not** include `Other`.

**List as demo2:**

Login as `demo2` / `demo2`, same GET → only `[{ "name": "Other" }]`.

**Empty customer:**

Fresh DB with no rows for customer 99 → **200** `[]`.

**Errors:** no Bearer → **401**.

### DBeaver — verify source

```sql
SELECT id, customer_no, name, content, updated_at
FROM m_tv_chart_templates
WHERE customer_no = 1
ORDER BY name ASC;
```

**Expect:** rows for `Alpha`, `Zulu` only; `content` present in DB but **not** returned by API.

Tenant check:

```sql
SELECT customer_no, name FROM m_tv_chart_templates ORDER BY customer_no, name;
```

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign136Test" test
```

---

## 137 — Register / update chart template

**MD:** [`System_Overview_Design_137_Register_Update_Chart_Template_(TV).md`](System_Overview_Design/System_Overview_Design_137_Register_Update_Chart_Template_(TV).md)

| | |
|---|---|
| **Path** | `POST /api/chart-templates` |
| **Table** | V9 `m_tv_chart_templates` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign137Test` |
| **Key code** | `ChartTemplateController.upsert` → `TvChartTemplate.applyContent` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Token | Stub (JWT) |
| Body `name` required, max 64 | Done → 422 `CODE:30020` |
| Body `content` required | Done → 422 `CODE:30020` |
| Match `(customer_no, name)` | Done |
| Insert if missing | Done: `customer_no`, `name`, `content` |
| Update if found | Done: **content only** (name / customer stay) |
| Return update datetime of [1] | Done → `{ "t": unix }` from row `updated_at` |
| Unique per customer | Done — `demo2` may reuse the same name |
| SaveLoadAdapter | **Done** |
| Bare number vs `{t}` | **Extra wrapper** (same as 133) |

### Postman — register then update

**Step A — first POST (insert):**

```
POST http://127.0.0.1:8080/api/chart-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My Dark","content":"{\"theme\":\"dark\"}"}
```

**Expect `200`:**

```json
{ "t": 172... }
```

`t` within a few seconds of now.

**Step B — second POST same name (update content only):**

```
POST http://127.0.0.1:8080/api/chart-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My Dark","content":"{\"theme\":\"light\"}"}
```

**Expect `200`:** `{ "t": … }` (same or later unix). List still has one `My Dark` row, no `content` in the list DTO.

**Errors:**

```
POST … {"name":"  ","content":"{}"}                         → 422 CODE:30020
POST … {"name":"<65 × A>","content":"{}"}                   → 422 CODE:30020
POST … {"name":"Dark","content":""}                         → 422 CODE:30020
POST … (no Bearer)                                          → 401
```

### DBeaver

After step A:

```sql
SELECT id, customer_no, name, content, updated_at
FROM m_tv_chart_templates
WHERE customer_no = 1 AND name = 'My Dark';
```

**Expect:** one row, `content` = `{"theme":"dark"}`.

After step B: same `id`, same `name`, `content` = `{"theme":"light"}`, `updated_at` later.

Same name, other customer:

```sql
SELECT customer_no, name, content
FROM m_tv_chart_templates
WHERE name = 'My Dark'
ORDER BY customer_no;
```

Customer `2` can have its own `My Dark` after logging in as `demo2` and POSTing.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign137Test" test
```

---

## 138 — Get chart template

**MD:** [`System_Overview_Design_138_Get_Chart_Template_(TV).md`](System_Overview_Design/System_Overview_Design_138_Get_Chart_Template_(TV).md)

| | |
|---|---|
| **Path** | `GET /api/chart-templates/{name}` |
| **Table** | V9 `m_tv_chart_templates` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign138Test` |
| **Key code** | `ChartTemplateController.get` → `ChartTemplateDto` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Token | Stub (JWT) |
| Path `name` required, max 64 | Done → 422 `CODE:30020` |
| Match customer + name | Done |
| Missing row | Done → 404 `CODE:30404` |
| DTO `name` + `content` | Done |
| Other customer | Done → 404 (same as 134) |
| SaveLoadAdapter | **Done** |

Spaces in names must be URL-encoded (`My%20Dark`).

### Postman — full get flow

**Step A — create (137):**

```
POST http://127.0.0.1:8080/api/chart-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My Dark","content":"{\"theme\":\"dark\"}"}
```

**Step B — get:**

```
GET http://127.0.0.1:8080/api/chart-templates/My%20Dark
Authorization: Bearer <token>
```

**Expect `200`:**

```json
{
  "name": "My Dark",
  "content": "{\"theme\":\"dark\"}"
}
```

Must **not** include `customer_no`, `id`, or `timestamp`.

**Step C — update content (137):**

```
POST http://127.0.0.1:8080/api/chart-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"My Dark","content":"{\"theme\":\"light\"}"}
```

**Step D — get after update:**

```
GET http://127.0.0.1:8080/api/chart-templates/My%20Dark
Authorization: Bearer <token>
```

**Expect `200`:** `name=My Dark`, `content={"theme":"light"}`.

**Errors:**

```
GET http://127.0.0.1:8080/api/chart-templates/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
  (65 × A)                                                         → 422 CODE:30020

GET http://127.0.0.1:8080/api/chart-templates/Missing
Authorization: Bearer <token>                                      → 404 CODE:30404

GET http://127.0.0.1:8080/api/chart-templates/My%20Dark
Authorization: Bearer <demo2 token>  (demo owns the row)           → 404 CODE:30404

GET http://127.0.0.1:8080/api/chart-templates/My%20Dark
  (no Bearer)                                                      → 401
```

Compare list vs get — list (doc 136) has **no** `content`:

```
GET http://127.0.0.1:8080/api/chart-templates
Authorization: Bearer <token>
```

**Expect:** `{ "name": "My Dark" }` in the array, no `content`. GET-by-name is the only call that returns `content`.

### DBeaver

After step D:

```sql
SELECT name, content
FROM m_tv_chart_templates
WHERE customer_no = 1 AND name = 'My Dark';
```

**Expect:** API `name` and `content` match this row.

```sql
SELECT customer_no, name, content
FROM m_tv_chart_templates
WHERE name = 'My Dark'
ORDER BY customer_no;
```

Customer `2` must not appear in GET when using the `demo` token.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign138Test" test
```

---

## 139 — Delete chart template

**MD:** [`System_Overview_Design_139_Delete_Chart_Template_(TV).md`](System_Overview_Design/System_Overview_Design_139_Delete_Chart_Template_(TV).md)

| | |
|---|---|
| **Path** | `DELETE /api/chart-templates/{name}` |
| **Table** | V9 `m_tv_chart_templates` |
| **Implemented?** | **Yes** |
| **Test class** | `SystemOverviewDesign139Test` |
| **Key code** | `ChartTemplateController.delete` → `SystemDatetimeResponse` |

### What MD requires vs app

| MD rule | Status |
|---------|--------|
| Token | Stub (JWT) |
| Path `name` required, max 64 | Done → 422 `CODE:30020` |
| Template exists for this customer | Done → 404 `CODE:30404` |
| Hard delete row | Done |
| Return system datetime | Done → `{ "t": unix }` (**now**, not row `updated_at`) |
| Other customer | Done → 404, row **kept** |
| SaveLoadAdapter | **Done** |
| Bare number vs `{t}` | **Extra wrapper** (same as 135) |

### Postman — full delete flow

**Step A — create template to delete (137):**

```
POST http://127.0.0.1:8080/api/chart-templates
Authorization: Bearer <token>
Content-Type: application/json

{"name":"ToDelete","content":"{\"pane\":1}"}
```

**Expect `200`:** `{ "t": … }`.

**Step B — delete:**

```
DELETE http://127.0.0.1:8080/api/chart-templates/ToDelete
Authorization: Bearer <token>
```

**Expect `200`:**

```json
{ "t": 172... }
```

`t` within a few seconds of now.

**Step C — confirm gone (138 + 136):**

```
GET http://127.0.0.1:8080/api/chart-templates/ToDelete
Authorization: Bearer <token>
```

**Expect `404`** `CODE:30404`.

```
GET http://127.0.0.1:8080/api/chart-templates
Authorization: Bearer <token>
```

**Expect:** array does **not** contain `{ "name": "ToDelete" }`.

**Errors:**

```
DELETE http://127.0.0.1:8080/api/chart-templates/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
  (65 × A)                                                         → 422 CODE:30020

DELETE http://127.0.0.1:8080/api/chart-templates/Missing
Authorization: Bearer <token>                                      → 404 CODE:30404

DELETE http://127.0.0.1:8080/api/chart-templates/ToDelete
Authorization: Bearer <demo2 token>  (demo owns the row)           → 404 (row remains for demo)

DELETE http://127.0.0.1:8080/api/chart-templates/ToDelete
  (no Bearer)                                                      → 401
```

### DBeaver

Before delete:

```sql
SELECT COUNT(*) FROM m_tv_chart_templates
WHERE customer_no = 1 AND name = 'ToDelete';
```

**Expect:** `1`.

After delete:

```sql
SELECT COUNT(*) FROM m_tv_chart_templates
WHERE customer_no = 1 AND name = 'ToDelete';
```

**Expect:** `0`.

Full table check:

```sql
SELECT id, customer_no, name FROM m_tv_chart_templates ORDER BY customer_no, name;
```

Deleted name for customer `1` must be absent. If step “other customer” ran, `demo`’s row is still there until `demo` deletes it.

### Automated

```powershell
.\mvnw.cmd "-Dtest=SystemOverviewDesign139Test" test
```

---

## Summary table — implementation status 120–139

| Doc | API | Implemented | Test class | Main gaps |
|-----|-----|-------------|------------|-----------|
| 120 | `GET /api/config` | Yes | 120Test | Extra `supports_group_request`; JWT stub |
| 121 | `GET /api/history` | Yes | 121Test, Flyway | Mock bars; extra `bars[]` |
| 122 | `GET /api/time` | Yes | 122Test | Extra `serverTime` |
| 123 | `GET /api/symbols` | Yes | 123Test | Extra library fields; JWT stub |
| 124 | `GET /api/search` | Yes | 124Test | Extra ticker/filters |
| 125 | `GET /api/marks` | Yes | 125Test | Extra mark fonts; symbol len partial |
| 126 | `GET /api/timescale_marks` | Yes | 126Test | tooltip array; symbol len partial |
| 127 | `POST /api/layouts` | Yes | 127Test | Widget Save → POST; 201 + `{id}` |
| 128 | `PUT /api/layouts/{id}` | Yes | 128Test | content overwrite vs MD |
| 129 | `GET /api/layouts/{id}` | Yes | 129Test | Tenant filter extra; adapter open |
| 130 | `GET /api/layouts` | Yes | 130Test | Sort name N/A; adapter open |
| 131 | `DELETE /api/layouts/{id}` | Yes | 131Test | `{t}` wrapper; adapter open |
| 132 | `GET /api/indicator-templates` | Yes | 132Test | Sort name ASC; adapter open |
| 133 | `POST /api/indicator-templates` | Yes | 133Test | `{t}` wrapper; adapter open |
| 134 | `GET /api/indicator-templates/{name}` | Yes | 134Test | Tenant 404 extra; adapter open |
| 135 | `DELETE /api/indicator-templates/{name}` | Yes | 135Test | `{t}` wrapper; adapter open |
| 136 | `GET /api/chart-templates` | Yes | 136Test | Sort name ASC; adapter open |
| 137 | `POST /api/chart-templates` | Yes | 137Test | `{t}` wrapper; adapter open |
| 138 | `GET /api/chart-templates/{name}` | Yes | 138Test | Tenant 404 extra; adapter open |
| 139 | `DELETE /api/chart-templates/{name}` | Yes | 139Test | `{t}` wrapper; adapter open |

**Frontend Save/Load:** [`frontend/src/save-load-adapter.ts`](frontend/src/save-load-adapter.ts) `ServerSaveLoadAdapter` calls `/api/layouts`, `/api/indicator-templates`, and `/api/chart-templates`. Widget header Save/Load is wired. Drawing-tool templates have no Peach table (stubs).

---

## Quick URL reference

| Doc | Method | URL |
|-----|--------|-----|
| Login | POST | `http://127.0.0.1:8080/api/auth/login` |
| Extra | GET | `http://127.0.0.1:8080/curpairs` |
| 120 | GET | `http://127.0.0.1:8080/api/config` |
| 121 | GET | `http://127.0.0.1:8080/api/history?symbol=USDJPY&resolution=1D&from=<sec>&to=<sec>&bid_ask=MID` |
| 122 | GET | `http://127.0.0.1:8080/api/time` |
| 123 | GET | `http://127.0.0.1:8080/api/symbols?symbol=USDJPY` |
| 124 | GET | `http://127.0.0.1:8080/api/search` |
| 125 | GET | `http://127.0.0.1:8080/api/marks?symbol=USDJPY&resolution=1D&from=1787011200&to=1787270400` |
| 126 | GET | `http://127.0.0.1:8080/api/timescale_marks?symbol=USDJPY&resolution=1D&from=1787011200&to=1787270400` |
| 127 | POST | `http://127.0.0.1:8080/api/layouts` |
| 128 | PUT | `http://127.0.0.1:8080/api/layouts/{id}` |
| 129 | GET | `http://127.0.0.1:8080/api/layouts/{id}` |
| 130 | GET | `http://127.0.0.1:8080/api/layouts` |
| 131 | DELETE | `http://127.0.0.1:8080/api/layouts/{id}` |
| 132 | GET | `http://127.0.0.1:8080/api/indicator-templates` |
| 133 | POST | `http://127.0.0.1:8080/api/indicator-templates` |
| 134 | GET | `http://127.0.0.1:8080/api/indicator-templates/{name}` |
| 135 | DELETE | `http://127.0.0.1:8080/api/indicator-templates/{name}` |
| 136 | GET | `http://127.0.0.1:8080/api/chart-templates` |
| 137 | POST | `http://127.0.0.1:8080/api/chart-templates` |
| 138 | GET | `http://127.0.0.1:8080/api/chart-templates/{name}` |
| 139 | DELETE | `http://127.0.0.1:8080/api/chart-templates/{name}` |

All except login and health: `Authorization: Bearer <accessToken>` (includes `GET /curpairs`).
