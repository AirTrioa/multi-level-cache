package org.example.multilevelcache.manager;

/**
 * 分布式锁接口
 *
 * @author liuzw
 * @date 2024/9/24
 */
public interface DistributedLock {
  /**
   * 获取分布式锁
   *
   * @param lockName 锁名称
   * @return 加锁成功
   */
  boolean acquire(String lockName);

  /**
   * 释放锁
   *
   * @param lockName 锁名称
   */
  void release(String lockName);
}
