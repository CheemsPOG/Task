/*
 * Copyright (c) 2023 Central Tanshi FX Co.,Ltd
 */

package com.task.chart.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

/**
 * Redis hot cache for Peach {@code cache_set_*} (doc 121), backed by {@code t_chart_*} on miss.
 *
 * <p>Key: {@code peach:{cache_set_day}:USDJPY} — score = unix seconds, member = JSON bar.
 *
 * <br><br>
 * <table border="1" cellspacing="1" cellpadding="1" class="HISTORY">
 *   <colgroup>
 *     <col span="1" style="width:10%;">
 *     <col span="2" style="width:15%;">
 *   </colgroup>
 *   <tr><th colspan="4">History</th></tr>
 *   <tr><th>Ver  </th><th>Date      </th><th>Author   </th><th>Comment </th></tr>
 *   <tr><td>1.0.0</td><td>2026/08/21</td><td>Task</td><td>In-memory Phase 1</td></tr>
 *   <tr><td>1.1.0</td><td>2026/08/21</td><td>Task</td><td>Redis ZSET</td></tr>
 *   <tr><td>1.2.0</td><td>2026/08/21</td><td>Task</td><td>DB fallback / warm from t_chart_*</td></tr>
 * </table>
 * <p>
 *
 * @author Task
 * @version 1.2.0
 */
@Component
public class ChartCacheStore {

	private static final String KEY_PREFIX = "peach:";

	private final StringRedisTemplate redis;
	private final ChartBarRepository chartBarRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Object[] locks = new Object[CacheNamespace.values().length];

	/**
	 * Creates the Redis chart cache.
	 *
	 * @param redis string Redis template
	 * @param chartBarRepository warehouse tables
	 */
	public ChartCacheStore(StringRedisTemplate redis, ChartBarRepository chartBarRepository) {
		this.redis = redis;
		this.chartBarRepository = chartBarRepository;
		for (int i = 0; i < locks.length; i++) {
			locks[i] = new Object();
		}
	}

	/**
	 * Replaces Redis bars for one pair (caller persists warehouse separately).
	 *
	 * @param namespace cache namespace
	 * @param curpairCd currency pair CD
	 * @param bars bars to store
	 */
	public void replacePair(CacheNamespace namespace, String curpairCd, Collection<CachedChartBar> bars) {
		Object lock = locks[namespace.ordinal()];
		synchronized (lock) {
			replacePairUnlocked(namespace, curpairCd, bars);
		}
	}

	/**
	 * Upserts one bar in Redis.
	 *
	 * @param namespace cache namespace
	 * @param bar bar to put
	 */
	public void put(CacheNamespace namespace, CachedChartBar bar) {
		Object lock = locks[namespace.ordinal()];
		synchronized (lock) {
			String key = redisKey(namespace, bar.curpairCd());
			redis.opsForZSet().removeRangeByScore(key, bar.chartDatetimeSec(), bar.chartDatetimeSec());
			redis.opsForZSet().add(key, toJson(bar), bar.chartDatetimeSec());
		}
	}

	/**
	 * Reads bars (Redis; warms from {@code t_chart_*} when the key is empty).
	 *
	 * @param namespace cache namespace
	 * @param curpairCd currency pair CD
	 * @param fromSec inclusive start, or {@code null}
	 * @param toSec inclusive end, or {@code null}
	 * @return ascending list
	 */
	public List<CachedChartBar> query(
			CacheNamespace namespace,
			String curpairCd,
			Long fromSec,
			Long toSec) {
		Object lock = locks[namespace.ordinal()];
		synchronized (lock) {
			warmFromWarehouseIfEmpty(namespace, curpairCd);
			String key = redisKey(namespace, curpairCd);
			double from = fromSec == null ? Double.NEGATIVE_INFINITY : fromSec.doubleValue();
			double to = toSec == null ? Double.POSITIVE_INFINITY : toSec.doubleValue();
			if (fromSec != null && toSec != null && fromSec > toSec) {
				return List.of();
			}
			Set<String> members = redis.opsForZSet().rangeByScore(key, from, to);
			return decode(members);
		}
	}

	/**
	 * Latest chart datetime strictly before {@code fromSec}.
	 *
	 * @param namespace cache namespace
	 * @param curpairCd currency pair CD
	 * @param fromSec from query parameter
	 * @return prior bar unix seconds, or {@code null}
	 */
	public Long nextTimeBefore(CacheNamespace namespace, String curpairCd, long fromSec) {
		Object lock = locks[namespace.ordinal()];
		synchronized (lock) {
			warmFromWarehouseIfEmpty(namespace, curpairCd);
			String key = redisKey(namespace, curpairCd);
			Set<String> members = redis.opsForZSet().reverseRangeByScore(
					key,
					Double.NEGATIVE_INFINITY,
					fromSec - 1.0,
					0,
					1);
			List<CachedChartBar> bars = decode(members);
			if (!bars.isEmpty()) {
				return bars.get(0).chartDatetimeSec();
			}
			return chartBarRepository.nextTimeBefore(namespace, curpairCd, fromSec);
		}
	}

	/**
	 * Redis bar count for a pair (diagnostics).
	 *
	 * @param namespace cache namespace
	 * @param curpairCd currency pair CD
	 * @return size
	 */
	public int size(CacheNamespace namespace, String curpairCd) {
		Object lock = locks[namespace.ordinal()];
		synchronized (lock) {
			warmFromWarehouseIfEmpty(namespace, curpairCd);
			Long card = redis.opsForZSet().zCard(redisKey(namespace, curpairCd));
			return card == null ? 0 : card.intValue();
		}
	}

	/**
	 * Redis key for a Peach cache namespace + pair.
	 *
	 * @param namespace cache namespace
	 * @param curpairCd currency pair CD
	 * @return key e.g. {@code peach:cache_set_day:USDJPY}
	 */
	public static String redisKey(CacheNamespace namespace, String curpairCd) {
		return KEY_PREFIX + namespace.cacheName() + ":" + curpairCd;
	}

	private void warmFromWarehouseIfEmpty(CacheNamespace namespace, String curpairCd) {
		Long card = redis.opsForZSet().zCard(redisKey(namespace, curpairCd));
		if (card != null && card > 0) {
			return;
		}
		List<CachedChartBar> fromDb = chartBarRepository.query(namespace, curpairCd, null, null);
		if (!fromDb.isEmpty()) {
			replacePairUnlocked(namespace, curpairCd, fromDb);
		}
	}

	private void replacePairUnlocked(
			CacheNamespace namespace,
			String curpairCd,
			Collection<CachedChartBar> bars) {
		String key = redisKey(namespace, curpairCd);
		redis.delete(key);
		if (bars == null || bars.isEmpty()) {
			return;
		}
		Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(bars.size());
		for (CachedChartBar bar : bars) {
			tuples.add(ZSetOperations.TypedTuple.of(toJson(bar), (double) bar.chartDatetimeSec()));
		}
		redis.opsForZSet().add(key, tuples);
	}

	private String toJson(CachedChartBar bar) {
		try {
			return objectMapper.writeValueAsString(bar);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Failed to serialize chart bar", ex);
		}
	}

	private List<CachedChartBar> decode(Set<String> members) {
		if (members == null || members.isEmpty()) {
			return List.of();
		}
		List<CachedChartBar> bars = new ArrayList<>(members.size());
		for (String member : members) {
			try {
				bars.add(objectMapper.readValue(member, CachedChartBar.class));
			} catch (JsonProcessingException ex) {
				throw new IllegalStateException("Failed to deserialize chart bar", ex);
			}
		}
		return bars;
	}
}
