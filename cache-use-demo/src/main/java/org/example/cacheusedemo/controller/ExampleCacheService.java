package org.example.cacheusedemo.controller;

import com.github.benmanes.caffeine.cache.Cache;
import info.library.util.GsonUtils;
import org.example.multilevelcache.config.RedisCaffeineCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * @author liuzw
 * @date 2024/1/10
 */
@Service
public class ExampleCacheService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExampleCacheService.class);
  private final CacheManager cacheManager;

  public ExampleCacheService(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * 输出所有的 Cache
   * fixme: 只有使用 Caffeine 才能用该方法
   */
  public void showCaffeineCache(String cacheName) {
    RedisCaffeineCache cache = (RedisCaffeineCache) cacheManager.getCache(cacheName);
    final Cache<Object, Object> caffeineCache = cache.getCaffeineCache();
    LOGGER.info("缓存当前的数据：{}", GsonUtils.writeToJson(caffeineCache.asMap().values()));
  }
}
