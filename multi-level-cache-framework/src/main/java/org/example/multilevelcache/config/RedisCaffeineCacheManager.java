package org.example.multilevelcache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.example.multilevelcache.config.porperties.MultiCacheProperties;
import org.example.multilevelcache.manager.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 实现 两级缓存的管理类
 *
 * @author liuzw
 * @date 2024/1/19
 */
public class RedisCaffeineCacheManager implements CacheManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisCaffeineCacheManager.class);

  private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();

  /**
   * 配置文件
   */
  private final MultiCacheProperties properties;

  /**
   * RedisTemplate
   */
  private final RedisTemplate<Object, Object> redisTemplate;

  /**
   * 是否允许动态创建 Cache
   */
  private final boolean dynamic;

  /**
   * 缓存名称
   */
  private final Set<String> cacheNames;
  /**
   * 分布式锁
   */
  private final DistributedLock lock;

  public RedisCaffeineCacheManager(MultiCacheProperties properties,
                                   RedisTemplate<Object, Object> redisTemplate,
                                   DistributedLock lock) {
    super();
    this.properties = properties;
    this.redisTemplate = redisTemplate;
    this.dynamic = properties.isDynamic();
    this.cacheNames = properties.getCacheNames();
    this.lock = lock;
  }

  @Override
  public Cache getCache(String name) {
    Cache cache = cacheMap.get(name);
    if (cache != null) {
      return cache;
    }
    if (!dynamic && !cacheNames.contains(name)) {
      return null;
    }

    cache = new RedisCaffeineCache(name, redisTemplate, caffeineCache(), properties, lock);
    Cache oldCache = cacheMap.putIfAbsent(name, cache);
    LOGGER.debug("create cache instance, the cache name is : {}", name);
    return oldCache == null ? cache : oldCache;
  }

  public com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache() {
    Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();
    if (properties.getCaffeine().getExpireAfterAccess() > 0) {
      cacheBuilder.expireAfterAccess(properties.getCaffeine().getExpireAfterAccess(), TimeUnit.MILLISECONDS);
    }
    if (properties.getCaffeine().getExpireAfterWrite() > 0) {
      cacheBuilder.expireAfterWrite(properties.getCaffeine().getExpireAfterWrite(), TimeUnit.MILLISECONDS);
    }
    if (properties.getCaffeine().getInitialCapacity() > 0) {
      cacheBuilder.initialCapacity(properties.getCaffeine().getInitialCapacity());
    }
    if (properties.getCaffeine().getMaximumSize() > 0) {
      cacheBuilder.maximumSize(properties.getCaffeine().getMaximumSize());
    }
    if (properties.getCaffeine().getRefreshAfterWrite() > 0) {
      cacheBuilder.refreshAfterWrite(properties.getCaffeine().getRefreshAfterWrite(), TimeUnit.MILLISECONDS);
    }
    return cacheBuilder.build();
  }

  @Override
  public Collection<String> getCacheNames() {
    return this.cacheNames;
  }

  public void clearLocal(String cacheName, Object key) {
    Cache cache = cacheMap.get(cacheName);
    if (cache == null) {
      return;
    }
    RedisCaffeineCache redisCaffeineCache = (RedisCaffeineCache) cache;
    redisCaffeineCache.clearLocal(key);
  }
}
