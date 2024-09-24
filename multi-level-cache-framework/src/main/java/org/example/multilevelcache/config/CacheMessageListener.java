package org.example.multilevelcache.config;

import org.example.multilevelcache.domain.CacheMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.charset.StandardCharsets;

/**
 * 实现Redis消息的监听
 *
 * @author liuzw
 * @date 2024/1/19
 */
public class CacheMessageListener implements MessageListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheMessageListener.class);
  private final RedisTemplate<Object, Object> redisTemplate;
  private final RedisCaffeineCacheManager redisCaffeineCacheManager;

  public CacheMessageListener(RedisTemplate<Object, Object> redisTemplate,
                              RedisCaffeineCacheManager redisCaffeineCacheManager) {
    super();
    this.redisTemplate = redisTemplate;
    this.redisCaffeineCacheManager = redisCaffeineCacheManager;
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    CacheMessage cacheMessage = (CacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
    if (null == cacheMessage) {
      LOGGER.warn("cache message parse error,skip clear local cache");
      return;
    }
    LOGGER.debug("receive a redis topic message, clear local cache, the cacheName is {}, the key is {}", cacheMessage.getCacheName(), cacheMessage.getKey());
    redisCaffeineCacheManager.clearLocal(cacheMessage.getCacheName(), cacheMessage.getKey());
  }
}
