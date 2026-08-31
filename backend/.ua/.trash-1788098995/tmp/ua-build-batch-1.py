#!/usr/bin/env python3
"""Build batch-1 knowledge graph fragments from file-analyzer semantic analysis."""
import json
import math
from pathlib import Path

UA_DIR = Path(r"D:\Personal Projects\New\Task\backend\.ua")

nodes = []
edges = []


def add_node(**kwargs):
    nodes.append(kwargs)


def add_edge(source, target, etype, weight):
    edges.append({
        "source": source,
        "target": target,
        "type": etype,
        "direction": "forward",
        "weight": weight,
    })


# ---------------------------------------------------------------------------
# File nodes
# ---------------------------------------------------------------------------
add_node(
    id="file:src/main/java/com/task/chart/cache/CacheNamespace.java",
    type="file",
    name="CacheNamespace.java",
    filePath="src/main/java/com/task/chart/cache/CacheNamespace.java",
    summary="Enum of Redis/warehouse cache namespaces keyed by Peach chart type, mapping TradingView resolutions to table names, cache names, and bar periods.",
    tags=["cache", "enum", "resolution", "data-model"],
    complexity="moderate",
    languageNotes="Java enum with a static factory that scans values to resolve a TradingView resolution string.",
)
add_node(
    id="file:src/main/java/com/task/chart/cache/ChartCacheStore.java",
    type="file",
    name="ChartCacheStore.java",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    summary="Redis sorted-set cache for OHLC bars, with per-namespace locks, JSON encode/decode, and lazy warm-from-warehouse when a pair's key is empty.",
    tags=["cache", "redis", "service", "serialization"],
    complexity="complex",
    languageNotes="Uses Spring Redis ZSET operations scored by bar datetime, with striped Object locks by namespace ordinal.",
)
add_node(
    id="file:src/main/java/com/task/chart/controller/ChartDataController.java",
    type="file",
    name="ChartDataController.java",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    summary="REST controller exposing the TradingView UDF datafeed: health, config, server time, symbol search/resolve, marks, timescale marks, and history bars.",
    tags=["api-handler", "controller", "datafeed", "entry-point"],
    complexity="complex",
)
add_node(
    id="file:src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java",
    type="file",
    name="DatafeedConfigResponse.java",
    filePath="src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java",
    summary="Response DTO for the UDF /config endpoint, including supported exchanges, symbol types, resolutions, and feature flags.",
    tags=["dto", "serialization", "type-definition", "datafeed"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/dto/response/HealthResponse.java",
    type="file",
    name="HealthResponse.java",
    filePath="src/main/java/com/task/chart/dto/response/HealthResponse.java",
    summary="Minimal health-check response carrying a status string for the datafeed liveness endpoint.",
    tags=["dto", "serialization", "type-definition", "health"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/dto/response/HistoryResponse.java",
    type="file",
    name="HistoryResponse.java",
    filePath="src/main/java/com/task/chart/dto/response/HistoryResponse.java",
    summary="UDF history response that converts OHLC bars into columnar t/o/h/l/c arrays, with factories for successful and empty (no-data / nextTime) results.",
    tags=["dto", "serialization", "factory", "datafeed"],
    complexity="moderate",
)
add_node(
    id="file:src/main/java/com/task/chart/dto/response/MarkDto.java",
    type="file",
    name="MarkDto.java",
    filePath="src/main/java/com/task/chart/dto/response/MarkDto.java",
    summary="DTO for a TradingView chart mark: id, timestamp, color, text, and label used by the /marks endpoint.",
    tags=["dto", "serialization", "type-definition", "datafeed"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/dto/response/SearchSymbolDto.java",
    type="file",
    name="SearchSymbolDto.java",
    filePath="src/main/java/com/task/chart/dto/response/SearchSymbolDto.java",
    summary="DTO for one UDF symbol-search hit, including ticker, description, exchange, and type.",
    tags=["dto", "serialization", "type-definition", "datafeed"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/dto/response/ServerTimeResponse.java",
    type="file",
    name="ServerTimeResponse.java",
    filePath="src/main/java/com/task/chart/dto/response/ServerTimeResponse.java",
    summary="Response wrapping Unix epoch seconds for the UDF /time endpoint.",
    tags=["dto", "serialization", "type-definition", "datafeed"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/dto/response/SymbolInfoDto.java",
    type="file",
    name="SymbolInfoDto.java",
    filePath="src/main/java/com/task/chart/dto/response/SymbolInfoDto.java",
    summary="Resolved symbol metadata for TradingView: ticker, session, timezone, price scale, and supported resolutions.",
    tags=["dto", "serialization", "type-definition", "datafeed"],
    complexity="moderate",
)
add_node(
    id="file:src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java",
    type="file",
    name="TimescaleMarkDto.java",
    filePath="src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java",
    summary="DTO for a TradingView timescale mark: id, time, color, label, and tooltip list for the /timescale_marks endpoint.",
    tags=["dto", "serialization", "type-definition", "datafeed"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/entity/Season.java",
    type="file",
    name="Season.java",
    filePath="src/main/java/com/task/chart/entity/Season.java",
    summary="JPA entity for daylight-saving versus standard-time seasons, with start/end instants used to pick the FX trading session string.",
    tags=["data-model", "entity", "jpa", "session"],
    complexity="moderate",
)
add_node(
    id="file:src/main/java/com/task/chart/entity/TvMark.java",
    type="file",
    name="TvMark.java",
    filePath="src/main/java/com/task/chart/entity/TvMark.java",
    summary="JPA entity for persisted TradingView chart marks keyed by currency pair, resolution, and timestamp.",
    tags=["data-model", "entity", "jpa", "marks"],
    complexity="moderate",
)
add_node(
    id="file:src/main/java/com/task/chart/entity/TvTimescaleMark.java",
    type="file",
    name="TvTimescaleMark.java",
    filePath="src/main/java/com/task/chart/entity/TvTimescaleMark.java",
    summary="JPA entity for persisted TradingView timescale marks keyed by currency pair, resolution, and timestamp.",
    tags=["data-model", "entity", "jpa", "marks"],
    complexity="moderate",
)
add_node(
    id="file:src/main/java/com/task/chart/repository/SeasonRepository.java",
    type="file",
    name="SeasonRepository.java",
    filePath="src/main/java/com/task/chart/repository/SeasonRepository.java",
    summary="Spring Data repository that finds the current season overlapping a given instant among daylight-saving and standard codes.",
    tags=["repository", "jpa", "data-access", "session"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/repository/TvMarkRepository.java",
    type="file",
    name="TvMarkRepository.java",
    filePath="src/main/java/com/task/chart/repository/TvMarkRepository.java",
    summary="Spring Data repository querying chart marks for a pair and resolution within a time window, ordered by mark time.",
    tags=["repository", "jpa", "data-access", "marks"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java",
    type="file",
    name="TvTimescaleMarkRepository.java",
    filePath="src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java",
    summary="Spring Data repository querying timescale marks for a pair and resolution within a time window, ordered by mark time.",
    tags=["repository", "jpa", "data-access", "marks"],
    complexity="simple",
)
add_node(
    id="file:src/main/java/com/task/chart/service/ChartDataService.java",
    type="file",
    name="ChartDataService.java",
    filePath="src/main/java/com/task/chart/service/ChartDataService.java",
    summary="Service interface for the TradingView datafeed: config, server time, symbol search/resolve, marks, timescale marks, and OHLC history.",
    tags=["service", "interface", "datafeed", "api-handler"],
    complexity="moderate",
)
add_node(
    id="file:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    type="file",
    name="ChartDataServiceImpl.java",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    summary="Implements the UDF datafeed: validates requests, resolves FX pairs and sessions, loads marks from the database, and serves history bars from the Redis cache.",
    tags=["service", "datafeed", "validation", "cache"],
    complexity="complex",
)
add_node(
    id="file:src/main/java/com/task/chart/util/ResolutionMapper.java",
    type="file",
    name="ResolutionMapper.java",
    filePath="src/main/java/com/task/chart/util/ResolutionMapper.java",
    summary="Maps TradingView resolution strings to Peach chart types and period milliseconds, and validates history versus marks resolution sets.",
    tags=["utility", "resolution", "mapping", "validation"],
    complexity="moderate",
)

# ---------------------------------------------------------------------------
# Class nodes
# ---------------------------------------------------------------------------
add_node(
    id="class:src/main/java/com/task/chart/cache/CacheNamespace.java:CacheNamespace",
    type="class",
    name="CacheNamespace",
    filePath="src/main/java/com/task/chart/cache/CacheNamespace.java",
    lineRange=[34, 130],
    summary="Enum of cache namespaces (chart type, warehouse table, Redis name, TV resolution) with a factory that matches a TradingView resolution.",
    tags=["enum", "cache", "factory", "data-model"],
    complexity="moderate",
)
add_node(
    id="class:src/main/java/com/task/chart/cache/ChartCacheStore.java:ChartCacheStore",
    type="class",
    name="ChartCacheStore",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[46, 289],
    summary="Spring component that stores and queries CachedChartBar JSON in Redis ZSETs, warming from ChartBarRepository when a key is empty.",
    tags=["cache", "redis", "service", "singleton"],
    complexity="complex",
)
add_node(
    id="class:src/main/java/com/task/chart/controller/ChartDataController.java:ChartDataController",
    type="class",
    name="ChartDataController",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[52, 251],
    summary="Spring MVC controller that delegates UDF datafeed HTTP endpoints to ChartDataService.",
    tags=["api-handler", "controller", "datafeed"],
    complexity="complex",
)
add_node(
    id="class:src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java:DatafeedConfigResponse",
    type="class",
    name="DatafeedConfigResponse",
    filePath="src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java",
    lineRange=[33, 50],
    summary="Record-style response for datafeed configuration: exchanges, symbol types, resolutions, and capability flags.",
    tags=["dto", "type-definition", "serialization"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/dto/response/HealthResponse.java:HealthResponse",
    type="class",
    name="HealthResponse",
    filePath="src/main/java/com/task/chart/dto/response/HealthResponse.java",
    lineRange=[30, 31],
    summary="Tiny response record exposing a health status field.",
    tags=["dto", "type-definition", "health"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/dto/response/HistoryResponse.java:HistoryResponse",
    type="class",
    name="HistoryResponse",
    filePath="src/main/java/com/task/chart/dto/response/HistoryResponse.java",
    lineRange=[39, 118],
    summary="Columnar UDF history payload with static factories for bar lists and empty/no-data responses including optional nextTime.",
    tags=["dto", "factory", "serialization", "datafeed"],
    complexity="moderate",
)
add_node(
    id="class:src/main/java/com/task/chart/dto/response/MarkDto.java:MarkDto",
    type="class",
    name="MarkDto",
    filePath="src/main/java/com/task/chart/dto/response/MarkDto.java",
    lineRange=[32, 40],
    summary="Chart-mark DTO with id, time, color, text, and label fields.",
    tags=["dto", "type-definition", "marks"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/dto/response/SearchSymbolDto.java:SearchSymbolDto",
    type="class",
    name="SearchSymbolDto",
    filePath="src/main/java/com/task/chart/dto/response/SearchSymbolDto.java",
    lineRange=[32, 39],
    summary="Symbol-search hit DTO with ticker, description, type, and exchange.",
    tags=["dto", "type-definition", "search"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/dto/response/ServerTimeResponse.java:ServerTimeResponse",
    type="class",
    name="ServerTimeResponse",
    filePath="src/main/java/com/task/chart/dto/response/ServerTimeResponse.java",
    lineRange=[33, 44],
    summary="Wrapper for Unix server time in seconds returned by /time.",
    tags=["dto", "type-definition", "time"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/dto/response/SymbolInfoDto.java:SymbolInfoDto",
    type="class",
    name="SymbolInfoDto",
    filePath="src/main/java/com/task/chart/dto/response/SymbolInfoDto.java",
    lineRange=[34, 59],
    summary="Full UDF symbol-info payload including session, timezone, minmov, pricescale, and resolution lists.",
    tags=["dto", "type-definition", "symbol"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java:TimescaleMarkDto",
    type="class",
    name="TimescaleMarkDto",
    filePath="src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java",
    lineRange=[34, 41],
    summary="Timescale-mark DTO with id, time, color, label, and tooltip strings.",
    tags=["dto", "type-definition", "marks"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/entity/Season.java:Season",
    type="class",
    name="Season",
    filePath="src/main/java/com/task/chart/entity/Season.java",
    lineRange=[39, 88],
    summary="Maps the season table with daylight-saving/standard codes and inclusive start/end instants.",
    tags=["entity", "jpa", "data-model", "session"],
    complexity="moderate",
)
add_node(
    id="class:src/main/java/com/task/chart/entity/TvMark.java:TvMark",
    type="class",
    name="TvMark",
    filePath="src/main/java/com/task/chart/entity/TvMark.java",
    lineRange=[36, 92],
    summary="Persisted chart mark with pair code, resolution, timestamp, color, label, and text.",
    tags=["entity", "jpa", "data-model", "marks"],
    complexity="moderate",
)
add_node(
    id="class:src/main/java/com/task/chart/entity/TvTimescaleMark.java:TvTimescaleMark",
    type="class",
    name="TvTimescaleMark",
    filePath="src/main/java/com/task/chart/entity/TvTimescaleMark.java",
    lineRange=[36, 92],
    summary="Persisted timescale mark with pair code, resolution, timestamp, color, label, and tooltip.",
    tags=["entity", "jpa", "data-model", "marks"],
    complexity="moderate",
)
add_node(
    id="class:src/main/java/com/task/chart/repository/SeasonRepository.java:SeasonRepository",
    type="class",
    name="SeasonRepository",
    filePath="src/main/java/com/task/chart/repository/SeasonRepository.java",
    lineRange=[36, 50],
    summary="JpaRepository for Season with a derived query for seasons covering a given instant among selected codes.",
    tags=["repository", "jpa", "interface"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/repository/TvMarkRepository.java:TvMarkRepository",
    type="class",
    name="TvMarkRepository",
    filePath="src/main/java/com/task/chart/repository/TvMarkRepository.java",
    lineRange=[34, 50],
    summary="JpaRepository for TvMark with a range query by pair, resolution, and mark time.",
    tags=["repository", "jpa", "interface"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java:TvTimescaleMarkRepository",
    type="class",
    name="TvTimescaleMarkRepository",
    filePath="src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java",
    lineRange=[34, 52],
    summary="JpaRepository for TvTimescaleMark with a range query by pair, resolution, and timescale mark time.",
    tags=["repository", "jpa", "interface"],
    complexity="simple",
)
add_node(
    id="class:src/main/java/com/task/chart/service/ChartDataService.java:ChartDataService",
    type="class",
    name="ChartDataService",
    filePath="src/main/java/com/task/chart/service/ChartDataService.java",
    lineRange=[42, 119],
    summary="Contract for UDF datafeed operations consumed by ChartDataController.",
    tags=["service", "interface", "datafeed"],
    complexity="moderate",
)
add_node(
    id="class:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:ChartDataServiceImpl",
    type="class",
    name="ChartDataServiceImpl",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[71, 478],
    summary="Production ChartDataService: catalog/cache/history path plus DB-backed marks, seasons, and symbol resolve/search.",
    tags=["service", "datafeed", "validation", "cache"],
    complexity="complex",
)
add_node(
    id="class:src/main/java/com/task/chart/util/ResolutionMapper.java:ResolutionMapper",
    type="class",
    name="ResolutionMapper",
    filePath="src/main/java/com/task/chart/util/ResolutionMapper.java",
    lineRange=[36, 142],
    summary="Static utility holding allowed resolutions, period lookup, and Peach chart-type mapping from TV strings.",
    tags=["utility", "mapping", "validation"],
    complexity="moderate",
    languageNotes="Utility class with a private constructor and static lookup tables for period milliseconds.",
)

# ---------------------------------------------------------------------------
# Function nodes (10+ lines and/or exported non-trivial methods)
# ---------------------------------------------------------------------------
add_node(
    id="function:src/main/java/com/task/chart/cache/CacheNamespace.java:fromTvResolution",
    type="function",
    name="fromTvResolution",
    filePath="src/main/java/com/task/chart/cache/CacheNamespace.java",
    lineRange=[113, 129],
    summary="Resolves a TradingView resolution string to a CacheNamespace via Peach chart type, returning null when unmatched.",
    tags=["factory", "cache", "resolution"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/cache/ChartCacheStore.java:replacePair",
    type="function",
    name="replacePair",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[78, 85],
    summary="Replaces all cached bars for a pair under a namespace lock.",
    tags=["cache", "redis", "mutation"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/cache/ChartCacheStore.java:put",
    type="function",
    name="put",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[93, 102],
    summary="Upserts one bar into the Redis ZSET, removing any existing member at the same datetime score.",
    tags=["cache", "redis", "mutation"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/cache/ChartCacheStore.java:query",
    type="function",
    name="query",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[115, 137],
    summary="Returns bars in [fromSec, toSec] from Redis after warming the key from the warehouse if empty.",
    tags=["cache", "redis", "query"],
    complexity="moderate",
)
add_node(
    id="function:src/main/java/com/task/chart/cache/ChartCacheStore.java:nextTimeBefore",
    type="function",
    name="nextTimeBefore",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[147, 168],
    summary="Finds the latest bar datetime strictly before fromSec, falling back to the warehouse repository.",
    tags=["cache", "redis", "query"],
    complexity="moderate",
)
add_node(
    id="function:src/main/java/com/task/chart/cache/ChartCacheStore.java:size",
    type="function",
    name="size",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[177, 187],
    summary="Returns the ZSET cardinality for a pair after ensuring the cache is warmed.",
    tags=["cache", "redis", "query"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/cache/ChartCacheStore.java:warmFromWarehouseIfEmpty",
    type="function",
    name="warmFromWarehouseIfEmpty",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[206, 219],
    summary="Loads bars from ChartBarRepository and writes them to Redis when the pair key has no members.",
    tags=["cache", "warm-up", "data-access"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/cache/ChartCacheStore.java:replacePairUnlocked",
    type="function",
    name="replacePairUnlocked",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[228, 247],
    summary="Deletes the Redis key then bulk-adds bar JSON members scored by chart datetime.",
    tags=["cache", "redis", "mutation"],
    complexity="moderate",
)
add_node(
    id="function:src/main/java/com/task/chart/cache/ChartCacheStore.java:decode",
    type="function",
    name="decode",
    filePath="src/main/java/com/task/chart/cache/ChartCacheStore.java",
    lineRange=[270, 288],
    summary="Deserializes Redis ZSET members into CachedChartBar objects, failing fast on JSON errors.",
    tags=["serialization", "cache"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/controller/ChartDataController.java:health",
    type="function",
    name="health",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[73, 81],
    summary="Returns a HealthResponse for the datafeed liveness check.",
    tags=["api-handler", "health"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/controller/ChartDataController.java:config",
    type="function",
    name="config",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[88, 98],
    summary="Serves TradingView UDF configuration from ChartDataService.",
    tags=["api-handler", "datafeed"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/controller/ChartDataController.java:time",
    type="function",
    name="time",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[105, 115],
    summary="Returns current server Unix time via ChartDataService.",
    tags=["api-handler", "datafeed", "time"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/controller/ChartDataController.java:search",
    type="function",
    name="search",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[126, 141],
    summary="Searches FX symbols by query, exchange, type, and limit for the UDF search endpoint.",
    tags=["api-handler", "datafeed", "search"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/controller/ChartDataController.java:symbols",
    type="function",
    name="symbols",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[149, 163],
    summary="Resolves a symbol name to SymbolInfoDto for the UDF symbols endpoint.",
    tags=["api-handler", "datafeed", "symbol"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/controller/ChartDataController.java:marks",
    type="function",
    name="marks",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[174, 189],
    summary="Returns chart marks for a symbol, resolution, and time range.",
    tags=["api-handler", "datafeed", "marks"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/controller/ChartDataController.java:timescaleMarks",
    type="function",
    name="timescaleMarks",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[200, 215],
    summary="Returns timescale marks for a symbol, resolution, and time range.",
    tags=["api-handler", "datafeed", "marks"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/controller/ChartDataController.java:history",
    type="function",
    name="history",
    filePath="src/main/java/com/task/chart/controller/ChartDataController.java",
    lineRange=[230, 249],
    summary="Serves OHLC history bars, forwarding price/bid-ask and countBack to ChartDataService.",
    tags=["api-handler", "datafeed", "history"],
    complexity="moderate",
)
add_node(
    id="function:src/main/java/com/task/chart/dto/response/HistoryResponse.java:ok",
    type="function",
    name="ok",
    filePath="src/main/java/com/task/chart/dto/response/HistoryResponse.java",
    lineRange=[55, 65],
    summary="Builds a successful history response by pivoting BarDto list into columnar OHLC arrays.",
    tags=["factory", "serialization", "history"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/dto/response/HistoryResponse.java:empty",
    type="function",
    name="empty",
    filePath="src/main/java/com/task/chart/dto/response/HistoryResponse.java",
    lineRange=[73, 82],
    summary="Builds a no-data history response with empty columns and optional nextTime for UDF pagination.",
    tags=["factory", "serialization", "history"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:config",
    type="function",
    name="config",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[110, 124],
    summary="Assembles DatafeedConfigResponse from AppProperties TradingView settings.",
    tags=["service", "datafeed", "config"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:search",
    type="function",
    name="search",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[131, 160],
    summary="Validates search filters and queries active currency pairs, mapping them to SearchSymbolDto.",
    tags=["service", "search", "validation"],
    complexity="moderate",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:marks",
    type="function",
    name="marks",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[162, 173],
    summary="Loads TvMark rows for a pair/resolution window and maps them to MarkDto.",
    tags=["service", "marks", "data-access"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:timescaleMarks",
    type="function",
    name="timescaleMarks",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[175, 186],
    summary="Loads TvTimescaleMark rows for a pair/resolution window and maps them to TimescaleMarkDto.",
    tags=["service", "marks", "data-access"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateMarksRequest",
    type="function",
    name="validateMarksRequest",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[188, 201],
    summary="Rejects blank symbol/resolution or invalid from/to windows for marks endpoints.",
    tags=["validation", "marks"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toMarkDto",
    type="function",
    name="toMarkDto",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[203, 212],
    summary="Maps a TvMark entity to MarkDto for the UDF marks payload.",
    tags=["mapping", "dto", "marks"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolveSearchLimit",
    type="function",
    name="resolveSearchLimit",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[224, 234],
    summary="Clamps or defaults the search page size using TradingView max/default limit settings.",
    tags=["validation", "search"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSearchSymbol",
    type="function",
    name="toSearchSymbol",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[244, 254],
    summary="Maps a Ccypair to SearchSymbolDto with display ticker and Japanese description.",
    tags=["mapping", "dto", "search"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolve",
    type="function",
    name="resolve",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[256, 266],
    summary="Looks up an active currency pair and returns SymbolInfoDto for the current session.",
    tags=["service", "symbol", "data-access"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:requireCcypairCd",
    type="function",
    name="requireCcypairCd",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[268, 279],
    summary="Normalizes a symbol name to a currency-pair code and rejects blank or over-length values.",
    tags=["validation", "symbol"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:normalizeCcypairCd",
    type="function",
    name="normalizeCcypairCd",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[281, 290],
    summary="Uppercases and strips exchange prefixes/separators from a symbol name to a pair code.",
    tags=["normalization", "symbol"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:currentSession",
    type="function",
    name="currentSession",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[292, 314],
    summary="Selects summer or winter TradingView session hours from the overlapping Season row.",
    tags=["session", "data-access", "timezone"],
    complexity="moderate",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSymbolInfo",
    type="function",
    name="toSymbolInfo",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[316, 345],
    summary="Builds SymbolInfoDto from a Ccypair, session string, and TradingView configuration.",
    tags=["mapping", "dto", "symbol"],
    complexity="moderate",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    type="function",
    name="history",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[359, 433],
    summary="Validates history params, maps resolution to a cache namespace, queries Redis bars, and returns columnar HistoryResponse or nextTime.",
    tags=["service", "history", "cache", "validation"],
    complexity="complex",
)
add_node(
    id="function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateHistoryRequest",
    type="function",
    name="validateHistoryRequest",
    filePath="src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    lineRange=[435, 467],
    summary="Validates symbol, resolution, time window, and bid/ask price component for history requests.",
    tags=["validation", "history"],
    complexity="moderate",
)
add_node(
    id="function:src/main/java/com/task/chart/util/ResolutionMapper.java:periodMillis",
    type="function",
    name="periodMillis",
    filePath="src/main/java/com/task/chart/util/ResolutionMapper.java",
    lineRange=[88, 94],
    summary="Looks up bar period in milliseconds for a TradingView resolution string.",
    tags=["utility", "resolution", "lookup"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/util/ResolutionMapper.java:isHistoryResolution",
    type="function",
    name="isHistoryResolution",
    filePath="src/main/java/com/task/chart/util/ResolutionMapper.java",
    lineRange=[100, 102],
    summary="Returns whether the resolution is allowed on the history endpoint.",
    tags=["utility", "validation", "resolution"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/util/ResolutionMapper.java:isMarksResolution",
    type="function",
    name="isMarksResolution",
    filePath="src/main/java/com/task/chart/util/ResolutionMapper.java",
    lineRange=[108, 110],
    summary="Returns whether the resolution is allowed on marks endpoints.",
    tags=["utility", "validation", "resolution"],
    complexity="simple",
)
add_node(
    id="function:src/main/java/com/task/chart/util/ResolutionMapper.java:toPeachChartType",
    type="function",
    name="toPeachChartType",
    filePath="src/main/java/com/task/chart/util/ResolutionMapper.java",
    lineRange=[119, 141],
    summary="Converts a TradingView resolution into the Peach warehouse chart-type code used by CacheNamespace.",
    tags=["utility", "mapping", "resolution"],
    complexity="moderate",
)

# ---------------------------------------------------------------------------
# contains + exports
# ---------------------------------------------------------------------------
CONTAINS = []  # (filePath, nodeId, exported)

def contain(file_path, node_id, exported=False):
    add_edge(f"file:{file_path}", node_id, "contains", 1.0)
    if exported:
        add_edge(f"file:{file_path}", node_id, "exports", 0.8)


contain("src/main/java/com/task/chart/cache/CacheNamespace.java", "class:src/main/java/com/task/chart/cache/CacheNamespace.java:CacheNamespace", True)
contain("src/main/java/com/task/chart/cache/CacheNamespace.java", "function:src/main/java/com/task/chart/cache/CacheNamespace.java:fromTvResolution", True)

contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "class:src/main/java/com/task/chart/cache/ChartCacheStore.java:ChartCacheStore", True)
contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:replacePair", True)
contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:put", True)
contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:query", True)
contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:nextTimeBefore", True)
contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:size", True)
contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:warmFromWarehouseIfEmpty", False)
contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:replacePairUnlocked", False)
contain("src/main/java/com/task/chart/cache/ChartCacheStore.java", "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:decode", False)

contain("src/main/java/com/task/chart/controller/ChartDataController.java", "class:src/main/java/com/task/chart/controller/ChartDataController.java:ChartDataController", True)
contain("src/main/java/com/task/chart/controller/ChartDataController.java", "function:src/main/java/com/task/chart/controller/ChartDataController.java:health", True)
contain("src/main/java/com/task/chart/controller/ChartDataController.java", "function:src/main/java/com/task/chart/controller/ChartDataController.java:config", True)
contain("src/main/java/com/task/chart/controller/ChartDataController.java", "function:src/main/java/com/task/chart/controller/ChartDataController.java:time", True)
contain("src/main/java/com/task/chart/controller/ChartDataController.java", "function:src/main/java/com/task/chart/controller/ChartDataController.java:search", True)
contain("src/main/java/com/task/chart/controller/ChartDataController.java", "function:src/main/java/com/task/chart/controller/ChartDataController.java:symbols", True)
contain("src/main/java/com/task/chart/controller/ChartDataController.java", "function:src/main/java/com/task/chart/controller/ChartDataController.java:marks", True)
contain("src/main/java/com/task/chart/controller/ChartDataController.java", "function:src/main/java/com/task/chart/controller/ChartDataController.java:timescaleMarks", True)
contain("src/main/java/com/task/chart/controller/ChartDataController.java", "function:src/main/java/com/task/chart/controller/ChartDataController.java:history", True)

contain("src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java", "class:src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java:DatafeedConfigResponse", True)
contain("src/main/java/com/task/chart/dto/response/HealthResponse.java", "class:src/main/java/com/task/chart/dto/response/HealthResponse.java:HealthResponse", True)
contain("src/main/java/com/task/chart/dto/response/HistoryResponse.java", "class:src/main/java/com/task/chart/dto/response/HistoryResponse.java:HistoryResponse", True)
contain("src/main/java/com/task/chart/dto/response/HistoryResponse.java", "function:src/main/java/com/task/chart/dto/response/HistoryResponse.java:ok", True)
contain("src/main/java/com/task/chart/dto/response/HistoryResponse.java", "function:src/main/java/com/task/chart/dto/response/HistoryResponse.java:empty", True)
contain("src/main/java/com/task/chart/dto/response/MarkDto.java", "class:src/main/java/com/task/chart/dto/response/MarkDto.java:MarkDto", True)
contain("src/main/java/com/task/chart/dto/response/SearchSymbolDto.java", "class:src/main/java/com/task/chart/dto/response/SearchSymbolDto.java:SearchSymbolDto", True)
contain("src/main/java/com/task/chart/dto/response/ServerTimeResponse.java", "class:src/main/java/com/task/chart/dto/response/ServerTimeResponse.java:ServerTimeResponse", True)
contain("src/main/java/com/task/chart/dto/response/SymbolInfoDto.java", "class:src/main/java/com/task/chart/dto/response/SymbolInfoDto.java:SymbolInfoDto", True)
contain("src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java", "class:src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java:TimescaleMarkDto", True)

contain("src/main/java/com/task/chart/entity/Season.java", "class:src/main/java/com/task/chart/entity/Season.java:Season", True)
contain("src/main/java/com/task/chart/entity/TvMark.java", "class:src/main/java/com/task/chart/entity/TvMark.java:TvMark", True)
contain("src/main/java/com/task/chart/entity/TvTimescaleMark.java", "class:src/main/java/com/task/chart/entity/TvTimescaleMark.java:TvTimescaleMark", True)

contain("src/main/java/com/task/chart/repository/SeasonRepository.java", "class:src/main/java/com/task/chart/repository/SeasonRepository.java:SeasonRepository", True)
contain("src/main/java/com/task/chart/repository/TvMarkRepository.java", "class:src/main/java/com/task/chart/repository/TvMarkRepository.java:TvMarkRepository", True)
contain("src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java", "class:src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java:TvTimescaleMarkRepository", True)

contain("src/main/java/com/task/chart/service/ChartDataService.java", "class:src/main/java/com/task/chart/service/ChartDataService.java:ChartDataService", True)

contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "class:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:ChartDataServiceImpl", True)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:config", True)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:search", True)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:marks", True)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:timescaleMarks", True)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateMarksRequest", False)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toMarkDto", False)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolveSearchLimit", False)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSearchSymbol", False)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolve", True)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:requireCcypairCd", False)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:normalizeCcypairCd", False)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:currentSession", False)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSymbolInfo", False)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history", True)
contain("src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java", "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateHistoryRequest", False)

contain("src/main/java/com/task/chart/util/ResolutionMapper.java", "class:src/main/java/com/task/chart/util/ResolutionMapper.java:ResolutionMapper", True)
contain("src/main/java/com/task/chart/util/ResolutionMapper.java", "function:src/main/java/com/task/chart/util/ResolutionMapper.java:periodMillis", True)
contain("src/main/java/com/task/chart/util/ResolutionMapper.java", "function:src/main/java/com/task/chart/util/ResolutionMapper.java:isHistoryResolution", True)
contain("src/main/java/com/task/chart/util/ResolutionMapper.java", "function:src/main/java/com/task/chart/util/ResolutionMapper.java:isMarksResolution", True)
contain("src/main/java/com/task/chart/util/ResolutionMapper.java", "function:src/main/java/com/task/chart/util/ResolutionMapper.java:toPeachChartType", True)

# ---------------------------------------------------------------------------
# imports (exact batchImportData)
# ---------------------------------------------------------------------------
BATCH_IMPORTS = {
    "src/main/java/com/task/chart/cache/CacheNamespace.java": [
        "src/main/java/com/task/chart/util/ResolutionMapper.java"
    ],
    "src/main/java/com/task/chart/cache/ChartCacheStore.java": [],
    "src/main/java/com/task/chart/controller/ChartDataController.java": [
        "src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java",
        "src/main/java/com/task/chart/dto/response/HealthResponse.java",
        "src/main/java/com/task/chart/dto/response/HistoryResponse.java",
        "src/main/java/com/task/chart/dto/response/MarkDto.java",
        "src/main/java/com/task/chart/dto/response/SearchSymbolDto.java",
        "src/main/java/com/task/chart/dto/response/ServerTimeResponse.java",
        "src/main/java/com/task/chart/dto/response/SymbolInfoDto.java",
        "src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java",
        "src/main/java/com/task/chart/service/ChartDataService.java",
    ],
    "src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java": [],
    "src/main/java/com/task/chart/dto/response/HealthResponse.java": [],
    "src/main/java/com/task/chart/dto/response/HistoryResponse.java": [],
    "src/main/java/com/task/chart/dto/response/MarkDto.java": [],
    "src/main/java/com/task/chart/dto/response/SearchSymbolDto.java": [],
    "src/main/java/com/task/chart/dto/response/ServerTimeResponse.java": [],
    "src/main/java/com/task/chart/dto/response/SymbolInfoDto.java": [],
    "src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java": [],
    "src/main/java/com/task/chart/entity/Season.java": [],
    "src/main/java/com/task/chart/entity/TvMark.java": [],
    "src/main/java/com/task/chart/entity/TvTimescaleMark.java": [],
    "src/main/java/com/task/chart/repository/SeasonRepository.java": [
        "src/main/java/com/task/chart/entity/Season.java"
    ],
    "src/main/java/com/task/chart/repository/TvMarkRepository.java": [
        "src/main/java/com/task/chart/entity/TvMark.java"
    ],
    "src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java": [
        "src/main/java/com/task/chart/entity/TvTimescaleMark.java"
    ],
    "src/main/java/com/task/chart/service/ChartDataService.java": [
        "src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java",
        "src/main/java/com/task/chart/dto/response/HistoryResponse.java",
        "src/main/java/com/task/chart/dto/response/MarkDto.java",
        "src/main/java/com/task/chart/dto/response/SearchSymbolDto.java",
        "src/main/java/com/task/chart/dto/response/SymbolInfoDto.java",
        "src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java",
    ],
    "src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java": [
        "src/main/java/com/task/chart/cache/CacheNamespace.java",
        "src/main/java/com/task/chart/cache/CachedChartBar.java",
        "src/main/java/com/task/chart/cache/ChartCacheStore.java",
        "src/main/java/com/task/chart/config/AppProperties.java",
        "src/main/java/com/task/chart/constants/PriceComponent.java",
        "src/main/java/com/task/chart/dto/response/BarDto.java",
        "src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java",
        "src/main/java/com/task/chart/dto/response/HistoryResponse.java",
        "src/main/java/com/task/chart/dto/response/MarkDto.java",
        "src/main/java/com/task/chart/dto/response/SearchSymbolDto.java",
        "src/main/java/com/task/chart/dto/response/SymbolInfoDto.java",
        "src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java",
        "src/main/java/com/task/chart/entity/Ccypair.java",
        "src/main/java/com/task/chart/entity/Season.java",
        "src/main/java/com/task/chart/entity/TvMark.java",
        "src/main/java/com/task/chart/entity/TvTimescaleMark.java",
        "src/main/java/com/task/chart/exception/ResourceNotFoundException.java",
        "src/main/java/com/task/chart/exception/ServerErrorException.java",
        "src/main/java/com/task/chart/exception/ValidationException.java",
        "src/main/java/com/task/chart/repository/CcypairRepository.java",
        "src/main/java/com/task/chart/repository/SeasonRepository.java",
        "src/main/java/com/task/chart/repository/TvMarkRepository.java",
        "src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java",
        "src/main/java/com/task/chart/service/ChartDataService.java",
        "src/main/java/com/task/chart/service/SymbolCatalog.java",
        "src/main/java/com/task/chart/util/ResolutionMapper.java",
    ],
    "src/main/java/com/task/chart/util/ResolutionMapper.java": [],
}

import_count = 0
for src, targets in BATCH_IMPORTS.items():
    for tgt in targets:
        add_edge(f"file:{src}", f"file:{tgt}", "imports", 0.7)
        import_count += 1

# ---------------------------------------------------------------------------
# implements
# ---------------------------------------------------------------------------
add_edge(
    "class:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:ChartDataServiceImpl",
    "class:src/main/java/com/task/chart/service/ChartDataService.java:ChartDataService",
    "implements",
    0.9,
)

# ---------------------------------------------------------------------------
# depends_on (same-package runtime use missed by import scanner)
# ---------------------------------------------------------------------------
add_edge(
    "file:src/main/java/com/task/chart/cache/ChartCacheStore.java",
    "file:src/main/java/com/task/chart/cache/CacheNamespace.java",
    "depends_on",
    0.6,
)

# ---------------------------------------------------------------------------
# calls
# ---------------------------------------------------------------------------
def calls(src, tgt):
    add_edge(src, tgt, "calls", 0.8)


calls(
    "function:src/main/java/com/task/chart/cache/CacheNamespace.java:fromTvResolution",
    "function:src/main/java/com/task/chart/util/ResolutionMapper.java:toPeachChartType",
)
calls(
    "function:src/main/java/com/task/chart/controller/ChartDataController.java:config",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:config",
)
calls(
    "function:src/main/java/com/task/chart/controller/ChartDataController.java:search",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:search",
)
calls(
    "function:src/main/java/com/task/chart/controller/ChartDataController.java:symbols",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolve",
)
calls(
    "function:src/main/java/com/task/chart/controller/ChartDataController.java:marks",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:marks",
)
calls(
    "function:src/main/java/com/task/chart/controller/ChartDataController.java:timescaleMarks",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:timescaleMarks",
)
calls(
    "function:src/main/java/com/task/chart/controller/ChartDataController.java:history",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
)
calls(
    "function:src/main/java/com/task/chart/controller/ChartDataController.java:time",
    "class:src/main/java/com/task/chart/service/ChartDataService.java:ChartDataService",
)
calls(
    "function:src/main/java/com/task/chart/controller/ChartDataController.java:health",
    "class:src/main/java/com/task/chart/dto/response/HealthResponse.java:HealthResponse",
)

calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:config",
    "class:src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java:DatafeedConfigResponse",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:config",
    "class:src/main/java/com/task/chart/config/AppProperties.java:AppProperties",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:search",
    "class:src/main/java/com/task/chart/exception/ValidationException.java:ValidationException",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:search",
    "class:src/main/java/com/task/chart/repository/CcypairRepository.java:CcypairRepository",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:search",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSearchSymbol",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:marks",
    "class:src/main/java/com/task/chart/repository/TvMarkRepository.java:TvMarkRepository",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:marks",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateMarksRequest",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:marks",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toMarkDto",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:timescaleMarks",
    "class:src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java:TvTimescaleMarkRepository",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:timescaleMarks",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateMarksRequest",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateMarksRequest",
    "function:src/main/java/com/task/chart/util/ResolutionMapper.java:isMarksResolution",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateMarksRequest",
    "class:src/main/java/com/task/chart/exception/ValidationException.java:ValidationException",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toMarkDto",
    "class:src/main/java/com/task/chart/dto/response/MarkDto.java:MarkDto",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSearchSymbol",
    "class:src/main/java/com/task/chart/dto/response/SearchSymbolDto.java:SearchSymbolDto",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSearchSymbol",
    "class:src/main/java/com/task/chart/entity/Ccypair.java:Ccypair",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolve",
    "class:src/main/java/com/task/chart/repository/CcypairRepository.java:CcypairRepository",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolve",
    "class:src/main/java/com/task/chart/exception/ResourceNotFoundException.java:ResourceNotFoundException",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolve",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSymbolInfo",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:resolve",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:currentSession",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:requireCcypairCd",
    "class:src/main/java/com/task/chart/exception/ValidationException.java:ValidationException",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:currentSession",
    "class:src/main/java/com/task/chart/repository/SeasonRepository.java:SeasonRepository",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:currentSession",
    "class:src/main/java/com/task/chart/entity/Season.java:Season",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:currentSession",
    "class:src/main/java/com/task/chart/exception/ServerErrorException.java:ServerErrorException",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSymbolInfo",
    "class:src/main/java/com/task/chart/dto/response/SymbolInfoDto.java:SymbolInfoDto",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSymbolInfo",
    "class:src/main/java/com/task/chart/entity/Ccypair.java:Ccypair",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:toSymbolInfo",
    "function:src/main/java/com/task/chart/config/AppProperties.java:getTradingView",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/cache/CacheNamespace.java:fromTvResolution",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:query",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/cache/ChartCacheStore.java:nextTimeBefore",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/dto/response/HistoryResponse.java:ok",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/dto/response/HistoryResponse.java:empty",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/cache/CachedChartBar.java:toBarDto",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/constants/PriceComponent.java:from",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/constants/PriceComponent.java:fromBidAsk",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "class:src/main/java/com/task/chart/service/SymbolCatalog.java:SymbolCatalog",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:history",
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateHistoryRequest",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateHistoryRequest",
    "function:src/main/java/com/task/chart/util/ResolutionMapper.java:isHistoryResolution",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateHistoryRequest",
    "function:src/main/java/com/task/chart/constants/PriceComponent.java:fromBidAsk",
)
calls(
    "function:src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java:validateHistoryRequest",
    "class:src/main/java/com/task/chart/exception/ValidationException.java:ValidationException",
)

# intra-file ChartCacheStore calls (same-file function-to-function is optional;
# contains already links them. Skip intra-file calls to reduce noise.)

# ---------------------------------------------------------------------------
# Split and write
# ---------------------------------------------------------------------------
BATCH_FILES = [
    "src/main/java/com/task/chart/cache/CacheNamespace.java",
    "src/main/java/com/task/chart/cache/ChartCacheStore.java",
    "src/main/java/com/task/chart/controller/ChartDataController.java",
    "src/main/java/com/task/chart/dto/response/DatafeedConfigResponse.java",
    "src/main/java/com/task/chart/dto/response/HealthResponse.java",
    "src/main/java/com/task/chart/dto/response/HistoryResponse.java",
    "src/main/java/com/task/chart/dto/response/MarkDto.java",
    "src/main/java/com/task/chart/dto/response/SearchSymbolDto.java",
    "src/main/java/com/task/chart/dto/response/ServerTimeResponse.java",
    "src/main/java/com/task/chart/dto/response/SymbolInfoDto.java",
    "src/main/java/com/task/chart/dto/response/TimescaleMarkDto.java",
    "src/main/java/com/task/chart/entity/Season.java",
    "src/main/java/com/task/chart/entity/TvMark.java",
    "src/main/java/com/task/chart/entity/TvTimescaleMark.java",
    "src/main/java/com/task/chart/repository/SeasonRepository.java",
    "src/main/java/com/task/chart/repository/TvMarkRepository.java",
    "src/main/java/com/task/chart/repository/TvTimescaleMarkRepository.java",
    "src/main/java/com/task/chart/service/ChartDataService.java",
    "src/main/java/com/task/chart/service/impl/ChartDataServiceImpl.java",
    "src/main/java/com/task/chart/util/ResolutionMapper.java",
]

NEIGHBOR_PATHS = {
    "src/main/java/com/task/chart/cache/CachedChartBar.java",
    "src/main/java/com/task/chart/config/AppProperties.java",
    "src/main/java/com/task/chart/constants/PriceComponent.java",
    "src/main/java/com/task/chart/dto/response/BarDto.java",
    "src/main/java/com/task/chart/entity/Ccypair.java",
    "src/main/java/com/task/chart/exception/ResourceNotFoundException.java",
    "src/main/java/com/task/chart/exception/ServerErrorException.java",
    "src/main/java/com/task/chart/exception/ValidationException.java",
    "src/main/java/com/task/chart/repository/CcypairRepository.java",
    "src/main/java/com/task/chart/service/SymbolCatalog.java",
    "src/main/java/com/task/chart/service/impl/ChartLayoutServiceImpl.java",
}
NEIGHBOR_SYMBOLS = {
    ("src/main/java/com/task/chart/cache/CachedChartBar.java", "toBarDto"),
    ("src/main/java/com/task/chart/cache/CachedChartBar.java", "CachedChartBar"),
    ("src/main/java/com/task/chart/config/AppProperties.java", "getTradingView"),
    ("src/main/java/com/task/chart/config/AppProperties.java", "AppProperties"),
    ("src/main/java/com/task/chart/constants/PriceComponent.java", "from"),
    ("src/main/java/com/task/chart/constants/PriceComponent.java", "fromBidAsk"),
    ("src/main/java/com/task/chart/constants/PriceComponent.java", "PriceComponent"),
    ("src/main/java/com/task/chart/dto/response/BarDto.java", "BarDto"),
    ("src/main/java/com/task/chart/entity/Ccypair.java", "Ccypair"),
    ("src/main/java/com/task/chart/entity/Ccypair.java", "getCcypairCd"),
    ("src/main/java/com/task/chart/entity/Ccypair.java", "getCcypairJp"),
    ("src/main/java/com/task/chart/entity/Ccypair.java", "getRateUnit"),
    ("src/main/java/com/task/chart/exception/ResourceNotFoundException.java", "ResourceNotFoundException"),
    ("src/main/java/com/task/chart/exception/ServerErrorException.java", "ServerErrorException"),
    ("src/main/java/com/task/chart/exception/ValidationException.java", "ValidationException"),
    ("src/main/java/com/task/chart/repository/CcypairRepository.java", "CcypairRepository"),
    ("src/main/java/com/task/chart/service/SymbolCatalog.java", "SymbolCatalog"),
}

IMPORT_TARGETS = set()
for ts in BATCH_IMPORTS.values():
    IMPORT_TARGETS.update(ts)

nodeCount = len(nodes)
edgeCount = len(edges)
print(f"TOTAL nodes={nodeCount} edges={edgeCount} imports={import_count}")

ids = [n["id"] for n in nodes]
assert len(ids) == len(set(ids)), "duplicate node ids"
assert import_count == 45, f"expected 45 imports, got {import_count}"

# No self-edges
for e in edges:
    assert e["source"] != e["target"], e

# Split. Formula is a lower bound; bump until no part exceeds 60 nodes / 120 edges
# because ChartDataServiceImpl concentrates most import/call edges.
if nodeCount <= 60 and edgeCount <= 120:
    parts = 1
else:
    parts = math.ceil(max(nodeCount / 60.0, edgeCount / 120.0))
    # Recompute after a trial split if a part still overflows.
    while True:
        trial_chunk = math.ceil(len(sorted(BATCH_FILES)) / parts)
        trial_groups = [sorted(BATCH_FILES)[i * trial_chunk:(i + 1) * trial_chunk] for i in range(parts)]
        overflow = False
        for group in trial_groups:
            group_set = set(group)
            part_nodes = [n for n in nodes if n.get("filePath") in group_set]
            part_ids = {n["id"] for n in part_nodes}
            part_edges = [e for e in edges if e["source"] in part_ids]
            if len(part_nodes) > 60 or len(part_edges) > 120:
                overflow = True
                break
        if not overflow or parts >= len(BATCH_FILES):
            break
        parts += 1

sorted_files = sorted(BATCH_FILES)
chunk = math.ceil(len(sorted_files) / parts)
file_groups = [sorted_files[i * chunk:(i + 1) * chunk] for i in range(parts)]

out_dir = UA_DIR / "intermediate"
out_dir.mkdir(parents=True, exist_ok=True)


def node_file_path(n):
    return n.get("filePath")


def edge_ok(e, part_node_ids, part_files):
    src, tgt = e["source"], e["target"]
    if src in part_node_ids or tgt in part_node_ids:
        pass
    # source must be in this part's nodes
    if src not in part_node_ids:
        return False
    if tgt in part_node_ids:
        return True
    # file:<path>
    if tgt.startswith("file:"):
        path = tgt[len("file:"):]
        return path in NEIGHBOR_PATHS or path in IMPORT_TARGETS or path in BATCH_FILES
    if tgt.startswith("function:") or tgt.startswith("class:"):
        rest = tgt.split(":", 1)[1]
        # function:path:symbol or class:path:Name
        # path may contain colons? no, windows paths not used
        # format: <prefix>:<relative-path>:<symbol>
        # relative path contains slashes
        idx = rest.rfind(":")
        path, symbol = rest[:idx], rest[idx + 1:]
        if path in BATCH_FILES:
            return True  # other part of same batch
        if path in NEIGHBOR_PATHS or path in IMPORT_TARGETS:
            return (path, symbol) in NEIGHBOR_SYMBOLS or True  # allow class/function on known neighbor files
        return False
    return False


written = []
if parts == 1:
    path = out_dir / "batch-1.json"
    path.write_text(json.dumps({"nodes": nodes, "edges": edges}, indent=2), encoding="utf-8")
    written.append((path.name, len(nodes), len(edges)))
else:
    for i, group in enumerate(file_groups, start=1):
        group_set = set(group)
        part_nodes = [n for n in nodes if node_file_path(n) in group_set]
        part_ids = {n["id"] for n in part_nodes}
        part_edges = [e for e in edges if e["source"] in part_ids]
        failed = []
        for e in part_edges:
            if not edge_ok(e, part_ids, group_set):
                failed.append(e)
        if failed:
            raise SystemExit(f"Part {i} validation failed: {failed[:5]}")
        path = out_dir / f"batch-1-part-{i}.json"
        path.write_text(json.dumps({"nodes": part_nodes, "edges": part_edges}, indent=2), encoding="utf-8")
        written.append((path.name, len(part_nodes), len(part_edges)))

print("PARTS", parts)
print("FILES", sorted_files)
print("GROUPS", [[p.split("/")[-1] for p in g] for g in file_groups])
for w in written:
    print("WROTE", w)
print("SUM nodes", sum(w[1] for w in written), "edges", sum(w[2] for w in written))
