package org.example.multilevelcache;

import org.example.multilevelcache.config.CacheMessageListener;
import org.example.multilevelcache.config.RedisCaffeineCacheManager;
import org.example.multilevelcache.config.porperties.MultiCacheProperties;
import org.example.multilevelcache.manager.DistributedLock;
import org.example.multilevelcache.manager.impl.DistributedRedisLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Objects;

/**
 * 多级缓存自动配置类，在 Redis自动配置后启动
 *
 * @author liuzw
 * @date 2024/1/19
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableConfigurationProperties(MultiCacheProperties.class)
public class MultiLevelCacheAutoConfiguration {
  private final MultiCacheProperties properties;

  public MultiLevelCacheAutoConfiguration(MultiCacheProperties properties) {
    this.properties = properties;
  }

  /**
   * 注入RedisTemplate
   */
  @Bean("cacheValueRedisTemplate")
  public RedisTemplate<Object, Object> cacheValueRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<Object, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);
    // 指定 Redis 存储 value 为 json 格式
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    // key 为 String 格式
    template.setKeySerializer(new StringRedisSerializer());
    return template;
  }

  /**
   * 依托 Redisson 实现的分布式锁
   */
  @Bean
  public DistributedLock distributedLock(RedissonClient redissonClient) {
    return new DistributedRedisLock(redissonClient);
  }

  @Bean
  public RedisCaffeineCacheManager cacheManager(@Qualifier("cacheValueRedisTemplate") RedisTemplate<Object, Object> redisTemplate,
                                                DistributedLock distributedLock) {
    return new RedisCaffeineCacheManager(properties, redisTemplate, distributedLock);
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(@Qualifier("cacheValueRedisTemplate") RedisTemplate<Object, Object> redisTemplate,
                                                                     RedisCaffeineCacheManager redisCaffeineCacheManager) {
    RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
    redisMessageListenerContainer.setConnectionFactory(Objects.requireNonNull(redisTemplate.getConnectionFactory()));
    CacheMessageListener cacheMessageListener = new CacheMessageListener(redisTemplate, redisCaffeineCacheManager);
    redisMessageListenerContainer.addMessageListener(cacheMessageListener, new ChannelTopic(properties.getRedis().getTopic()));
    return redisMessageListenerContainer;
  }
}
