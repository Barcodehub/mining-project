package com.mining.soft.infrastructure.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("reports",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(15, TimeUnit.SECONDS)
                        .build());
        manager.registerCustomCache("environmental-data",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(15, TimeUnit.SECONDS)
                        .build());
        manager.registerCustomCache("environmental-data-aggregate",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .build());
        manager.registerCustomCache("adapters",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());
        return manager;
    }
}
