package org.example.multilevelcache.domain;

import java.io.Serializable;

/**
 * 用于缓存更新消息，Redis消息发布/订阅
 *
 * @author liuzw
 * @date 2024/1/19
 */
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

  public CacheMessage() {
  }

  public CacheMessage(String cacheName, Object key) {
    super();
    this.cacheName = cacheName;
    this.key = key;
  }

  public String getCacheName() {
    return cacheName;
  }

  public void setCacheName(String cacheName) {
    this.cacheName = cacheName;
  }

  public Object getKey() {
    return key;
  }

  public void setKey(Object key) {
    this.key = key;
  }
}
