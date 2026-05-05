package com.schemaguard.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaguard.starter.cache.SchemaCacheConfig;
import com.schemaguard.starter.client.SchemaRegistryClient;
import com.schemaguard.starter.interceptor.SchemaGuardProducerPostProcessor;
import com.schemaguard.starter.service.SchemaValidationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;

import java.util.HashMap;
import java.util.Map;

@AutoConfiguration(before = KafkaAutoConfiguration.class)
@ConditionalOnClass(org.springframework.kafka.core.KafkaTemplate.class)
@ConditionalOnProperty(prefix = "schema-guard", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(SchemaGuardProperties.class)
@Import(SchemaCacheConfig.class)
public class SchemaGuardAutoConfiguration {

    @Bean
    public SchemaRegistryClient schemaRegistryClient(
            org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder,
            SchemaGuardProperties properties) {
        return new SchemaRegistryClient(webClientBuilder, properties);
    }

    @Bean
    public SchemaValidationService schemaValidationService(
            SchemaRegistryClient registryClient,
            com.github.benmanes.caffeine.cache.Cache<String, com.networknt.schema.JsonSchema> schemaCache,
            SchemaGuardProperties properties,
            ObjectMapper objectMapper) {
        return new SchemaValidationService(registryClient, schemaCache, properties, objectMapper);
    }

    /**
     * Customizes the KafkaProducerFactory to inject our ProducerPostProcessor proxy.
     */
    @Bean
    public DefaultKafkaProducerFactoryCustomizer schemaGuardProducerPostProcessorCustomizer(SchemaValidationService validationService) {
        return producerFactory -> {
            producerFactory.addPostProcessor(new SchemaGuardProducerPostProcessor<>(validationService));
        };
    }
}
