# 基于 Caffeine + Redis 实现二级缓存
## 实现原理
`二级缓存实现: 使用 caffeine 和 redis 实现二级缓存`
1. 基于 `Springboot` 提供的 `Cache` 和 `CacheManager` 接口，实现对应的缓存操作;
2. 针对集群部署时，`Caffeine` 内存缓存更新不一致的情况，使用 `RedisMessage` 进行通知缓存清理。
   - `Redis` 本身实现了一个 简单的 `MQ` 机制(仅支持广播)。

## 使用
###
```xml
    <!-- 引入二级缓存依赖,同时会默认引入 Redis 配置 -->
    <dependency>
      <groupId>org.example</groupId>
      <artifactId>multi-level-cache-framework</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
```

### yaml 配置
```yaml
spring:
  # 二级缓存配置
  multi-cache:
    cache-prefix: airtrioa
    caffeine:
      # caffeine 过期时间
      expire-after-access: 500000
    redis:
      # redis 的过期时间
      default-expiration: 600000
  # Redis 配置
  redis:
    # redis 的配置
    port: 6379
    host: 127.0.0.1
```
## multi-level-cache-framework 详细实现
1. 首先，继承 `AbstractValueAdaptingCache` ，实现 `Cache` 对象
```java
public class RedisCaffeineCache extends AbstractValueAdaptingCache {
   private static final Logger LOGGER = LoggerFactory.getLogger(RedisCaffeineCache.class);
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

   // 依次实现相关 Cache 的接口

   /**
    * 命中缓存
    */
   protected Object lookup(Object key) {
      // 1. 先从 caffeine 中获取缓存，获取不到则从 redis 中获取缓存
      // 2. 获取 redis 缓存后，将 缓存数据 put 一下
   }

   /**
    * 设置缓存
    */
   @Override
   public void put(Object key, Object value) {
      // 1. 若 value == null 且 缓存中不允许null值，则清除后直接返回，不进行 put null
      // 2. 根据过期时间，设置 redis 缓存
      // 3. 发送 CacheMessage 事件，用于清除其他节点的 Caffeine 缓存
      // 4. 设置本节点的 caffeine 缓存
   }

   /**
    * 清除缓存
    */
   @Override
   public void evict(Object key) {
      // 1. 先清除 redis 中缓存数据（防止短时间内，又存在请求将 redis 缓存加载到 caffeine）
      // 2. 发送 CacheMessage 事件，用于清除其他节点的 Caffeine 缓存
      // 3. 清除本节点的 caffeine 缓存
   }

   /**
    * 清空所有缓存
    */
   @Override
   public void clear() {
      // 先整体清除 redis 缓存
      // 发送多个 CacheMessage
      // 整体清除本节点 caffeine 缓存
   }

   @Override
   public ValueWrapper putIfAbsent(Object key, Object value) {
      // 1. 缓存中 key 对应的 value 如果为null，则设置缓存值
      // 2. 不为空，则直接返回 缓存中的值
      // 3. 注意，这边需要使用 redis 分布式锁进行 获取值+设置值
   }
}
```
2. 其次，实现 `CacheManager` 接口
```java
public class RedisCaffeineCacheManager implements CacheManager {
   private static final Logger LOGGER = LoggerFactory.getLogger(RedisCaffeineCacheManager.class);
   private final ConcurrentMap<String, Cache> cacheMap = new ConcurrentHashMap<>();
   private final MultiCacheProperties properties;
   private final RedisTemplate<Object, Object> redisTemplate;
   private final Set<String> cacheNames;
   @Override
   public Cache getCache(String name) {
      // 从 CacheMap 中获取 Cache
   }

   public com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache() {
      // 根据配置文件，构建 Caffeine Cache
   }


   public void clearLocal(String cacheName, Object key) {
      // 获取 RedisCaffeineCache，执行 clear 
   }
}
```
3. 然后，定义 `RedisCacheMessage` 以及创建 事件监听器
```java
public class CacheMessage implements Serializable {
   private static final long serialVersionUID = 5987219310442078193L;
   /**
    * 缓存名称
    */
   private String cacheName;
   /**
    * 缓存的key
    */
   private Object key;
}
public class CacheMessageListener implements MessageListener {
   private static final Logger LOGGER = LoggerFactory.getLogger(CacheMessageListener.class);
   private final RedisTemplate<Object, Object> redisTemplate;
   private final RedisCaffeineCacheManager redisCaffeineCacheManager;
   @Override
   public void onMessage(Message message, byte[] pattern) {
     // 清除 Cache
   }
}
```
4. 最后，实现自动化配置类 `MultiLevelCacheAutoConfiguration`
```java
/**
 * 在 Redis自动配置后启动
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class MultiLevelCacheAutoConfiguration {
   private MultiCacheProperties properties;

   /**
    * 注入RedisTemplate
    */
   public RedisTemplate<Object, Object> cacheValueRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
      // 设置 key 为 string
      // 设置 value 为 json
   }

   /**
    * 注入 RedisCaffeineCacheManager
    */
   @Bean
   public RedisCaffeineCacheManager cacheManager(@Qualifier("cacheValueRedisTemplate") RedisTemplate<Object, Object> redisTemplate,
                                                 DistributedLock distributedLock) {
      return new RedisCaffeineCacheManager(properties, redisTemplate, distributedLock);
   }

   /**
    * 注册 Redis事件监听
    */
   @Bean
   public RedisMessageListenerContainer redisMessageListenerContainer(RedisTemplate<Object, Object> cacheValueRedisTemplate,
                                                                      RedisCaffeineCacheManager redisCaffeineCacheManager) {
   }
}
```
##源代码参考
https://github.com/AirTrioa/multi-level-cache/tree/master/multi-level-cache-framework
