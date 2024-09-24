package org.example.multilevelcache.manager.impl;

import org.example.multilevelcache.manager.DistributedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 实现的分布式锁
 *
 * @author liuzw
 * @date 2024/7/2
 */
public class DistributedRedisLock implements DistributedLock {
  private static final String LOCK_TITLE = "redisLock_";

  private final RedissonClient redissonClient;

  public DistributedRedisLock(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Override
  public boolean acquire(String lockName) {
    //声明key对象
    String key = getKey(lockName);
    //获取锁对象
    RLock myLock = redissonClient.getLock(key);
    //加锁，并且设置锁过期时间3秒，防止死锁的产生  uuid+threadId
    myLock.lock(30, TimeUnit.SECONDS);
    //加锁成功
    return true;
  }

  @Override
  public void release(String lockName) {
    //必须是和加锁时的同一个key
    String key = getKey(lockName);
    //获取所对象
    RLock myLock = redissonClient.getLock(key);
    //释放锁（解锁）
    myLock.unlock();
  }

  private static String getKey(String lockName) {
    return LOCK_TITLE + lockName;
  }
}
