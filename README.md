# 多级缓存框架 【multi-level-cache】

## 实现原理
`多级缓存实现: 使用 caffeine 和 redis 实现二级缓存`
1. 基于 Springboot 提供的 Cache 和 CacheManager 接口，实现对应的 二级缓存操作
   - 其实只有二级缓存。

## multi-level-cache-framework
###
```xml
    <!-- 引入两级缓存依赖,同时会默认引入 Redis 配置 -->
    <dependency>
      <groupId>org.example</groupId>
      <artifactId>multi-level-cache-framework</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
```

### yaml 配置
```yaml
spring:
  # 两级缓存配置
  multi-cache:
    cache-prefix: airtrioa
    caffeine:
      expire-after-access: 500000
    redis:
      default-expiration: 600000
#  cache:
#    cache-names: userIdCache
  # Redis 配置
  redis:
    port: 6379
    host: 172.28.112.146
```
