package com.schemaguard.starter.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.networknt.schema.JsonSchema;
import com.schemaguard.starter.config.SchemaGuardProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class SchemaCacheConfig {

    @Bean
    public Cache<String, JsonSchema> schemaCache(SchemaGuardProperties properties) {
        return Caffeine.newBuilder()
                .expireAfterWrite(properties.getCacheTtlMinutes(), TimeUnit.MINUTES)
                .maximumSize(properties.getCacheMaxSize())
                .recordStats()
                .build();
    }
}
