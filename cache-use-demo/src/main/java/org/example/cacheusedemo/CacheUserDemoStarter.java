package org.example.cacheusedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * CacheUserDemoStarter
 *
 * @author liuzw
 * @date 2024/1/19
 */
@EnableCaching
@SpringBootApplication
public class CacheUserDemoStarter {

  public static void main(String[] args) {
    SpringApplication.run(CacheUserDemoStarter.class, args);
  }
}
