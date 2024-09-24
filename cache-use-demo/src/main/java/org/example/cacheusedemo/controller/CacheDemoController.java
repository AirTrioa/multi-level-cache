package org.example.cacheusedemo.controller;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * CacheDemoController
 *
 * @author liuzw
 * @date 2024/1/19
 */
@RestController
@RequestMapping("/cache-demo")
public class CacheDemoController {
  private static final String CACHE_NAME = "testCache1";
  private final ExampleCacheService exampleCacheService;

  public CacheDemoController(ExampleCacheService exampleCacheService) {
    this.exampleCacheService = exampleCacheService;
  }

  @Cacheable(cacheNames = CACHE_NAME, key = "#id")
  @GetMapping("/{id}")
  public TestEx index1(@PathVariable("id") String id) {
    exampleCacheService.showCaffeineCache(CACHE_NAME);
    return new TestEx(id, UUID.randomUUID() + "_" + id);
  }

  /**
   * 模拟删除掉缓存
   *
   * @param id
   */
  @CacheEvict(cacheNames = CACHE_NAME, key = "#id")
  @DeleteMapping("/{id}")
  public void index2(@PathVariable String id) {
    exampleCacheService.showCaffeineCache(CACHE_NAME);
  }

  /**
   * 模拟更新缓存
   *
   * @param id
   * @return
   */
  @CachePut(cacheNames = CACHE_NAME, key = "#id")
  @PutMapping("/{id}")
  public String index3(@PathVariable String id) {
    return UUID.randomUUID() + "_" + id + "已更新";
  }

}
