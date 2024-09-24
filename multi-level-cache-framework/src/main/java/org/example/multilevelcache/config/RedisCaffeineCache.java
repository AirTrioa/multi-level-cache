package org.example.multilevelcache.config;

import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.example.multilevelcache.config.porperties.MultiCacheProperties;
import org.example.multilevelcache.domain.CacheMessage;
import org.example.multilevelcache.manager.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Redis、Caffeine 两级缓存实现类
 *
 * @author liuzw
 * @date 2024/1/19
 */
public class RedisCaffeineCache extends AbstractValueAdaptingCache {
  private static final Logger LOGGER = LoggerFactory.getLogger(RedisCaffeineCache.class);
  /**
   * CacheName
   */
  private String name;
  /**
   * redis
   */
  private RedisTemplate<Object, Object> redisTemplate;
  /**
   * caffeine cache
   */
  private Cache<Object, Object> caffeineCache;
  /**
   * 缓存前缀
   */
  private String cachePrefix;
  /**
   * 全局过期时间，单位毫秒，默认不过期
   */
  private long defaultExpiration = 0;
  /**
   * 每个cacheName的过期时间，单位毫秒，优先级比defaultExpiration高
   */
  private Map<String, Long> expires;
  /**
   * 缓存更新时通知其他节点的topic名称
   */
  private String topic;
  /**
   * 分布式锁
   */
  private DistributedLock lock;

  protected RedisCaffeineCache(boolean allowNullValues) {
    super(allowNullValues);
  }

  public RedisCaffeineCache(String name, RedisTemplate<Object, Object> redisTemplate,
                            Cache<Object, Object> caffeineCache,
                            MultiCacheProperties properties,
                            DistributedLock lock) {
    super(properties.isCacheNullValues());
    this.name = name;
    this.redisTemplate = redisTemplate;
    this.caffeineCache = caffeineCache;
    this.cachePrefix = properties.getCachePrefix();
    this.defaultExpiration = properties.getRedis().getDefaultExpiration();
    this.expires = properties.getRedis().getExpires();
    this.topic = properties.getRedis().getTopic();
    this.lock = lock;
  }

  public Cache<Object, Object> getCaffeineCache() {
    return caffeineCache;
  }

  @Override
  protected Object lookup(Object key) {
    // 1. 先从 caffeine 中获取缓存，获取不到则从 redis 中获取缓存
    Object cacheKey = getKey(key);
    Object value = caffeineCache.getIfPresent(key);
    if (value != null) {
      LOGGER.debug("get cache from caffeine, the key is : {}", cacheKey);
      return value;
    }

    value = redisTemplate.opsForValue().get(cacheKey);

    // 2. 获取 redis 缓存后，将 缓存数据 put 一下
    if (value != null) {
      LOGGER.debug("get cache from redis and put in caffeine, the key is : {}", cacheKey);
      caffeineCache.put(key, value);
    }
    return value;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Object getNativeCache() {
    return this;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T get(Object key, Callable<T> valueLoader) {
    Object value = lookup(key);
    if (value != null) {
      return (T) value;
    }

    ReentrantLock lock = new ReentrantLock();
    try {
      lock.lock();
      value = lookup(key);
      if (value != null) {
        return (T) value;
      }
      value = valueLoader.call();
      Object storeValue = toStoreValue(valueLoader.call());
      put(key, storeValue);
      return (T) value;
    } catch (Exception e) {
      try {
        // 将异常转换成 cache 的异常
        Class<?> c = Class.forName("org.springframework.cache.Cache$ValueRetrievalException");
        Constructor<?> constructor = c.getConstructor(Object.class, Callable.class, Throwable.class);
        throw (RuntimeException) constructor.newInstance(key, valueLoader, e.getCause());
      } catch (Exception e1) {
        throw new IllegalStateException(e1);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void put(Object key, Object value) {
    if (!super.isAllowNullValues() && value == null) {
      // 1. 若 value == null 且 缓存中不允许null值，则清除后直接返回，不进行 put null
      this.evict(key);
      return;
    }
    // 2. 根据过期时间，设置 redis 缓存
    long expire = getExpire();
    if (expire > 0) {
      redisTemplate.opsForValue().set(getKey(key), toStoreValue(value), expire, TimeUnit.MILLISECONDS);
    } else {
      redisTemplate.opsForValue().set(getKey(key), toStoreValue(value));
    }

    // 3. 发送 CacheMessage 事件，用于清除其他节点的 Caffeine 缓存
    push(new CacheMessage(this.name, key));
    // 4. 设置本节点的 caffeine 缓存
    caffeineCache.put(key, value);
  }

  @Override
  public void evict(Object key) {
    // 1. 先清除 redis 中缓存数据（防止短时间内，又存在请求将 redis 缓存加载到 caffeine）
    redisTemplate.delete(getKey(key));
    // 2. 发送 CacheMessage 事件，用于清除其他节点的 Caffeine 缓存
    push(new CacheMessage(this.name, key));
    // 3. 清除本节点的 caffeine 缓存
    caffeineCache.invalidate(key);
  }

  @Override
  public void clear() {
    // 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
    Set<Object> keys = redisTemplate.keys(this.name.concat(":"));
    for (Object key : keys) {
      redisTemplate.delete(key);
    }
    push(new CacheMessage(this.name, null));
    caffeineCache.invalidateAll();
  }

  /**
   * 缓存中 key 对应的 value 如果为null，则设置缓存值
   */
  @Override
  public ValueWrapper putIfAbsent(Object key, Object value) {
    Object cacheKey = getKey(key);
    Object prevValue;
    // 分布式锁
    String lockName = cacheKey.toString();
    lock.acquire(lockName);
    // 1. 获取原始值
    prevValue = redisTemplate.opsForValue().get(cacheKey);
    if (prevValue == null) {
      // 2. 原始值为 null 的情况下，设置到 缓存中
      put(key, value);
    }
    // 释放分布式锁
    lock.release(lockName);
    return toValueWrapper(prevValue);
  }

  /**
   * 对 缓存key 进行处理，需要加上 缓存名称、缓存前缀
   *
   * @param key key
   * @return this.name:cachePrefix:key
   */
  private Object getKey(Object key) {
    if (StringUtils.isNotBlank(cachePrefix)) {
      return String.join(":", this.name, this.cachePrefix, key.toString());
    }
    return String.join(":", this.name, key.toString());
  }

  /**
   * 获取过期时间，和配置文件中 CacheName 进行对比
   *
   * @return 过期时间
   */
  private long getExpire() {
    long expire = defaultExpiration;
    Long cacheNameExpire = expires.get(this.name);
    return cacheNameExpire == null ? expire : cacheNameExpire;
  }

  /**
   * 推送缓存消息更新
   *
   * @param message 缓存消息
   */
  private void push(CacheMessage message) {
    redisTemplate.convertAndSend(topic, message);
  }

  /**
   * 清理本地缓存
   *
   * @param key key
   */
  public void clearLocal(Object key) {
    LOGGER.debug("clear local cache, the key is : {}", key);
    if (key == null) {
      caffeineCache.invalidateAll();
    } else {
      caffeineCache.invalidate(key);
    }
  }
}
