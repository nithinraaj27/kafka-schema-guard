package com.schemaguard.starter.client;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.schemaguard.starter.config.SchemaGuardProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SchemaRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(SchemaRegistryClient.class);
    private final WebClient webClient;
    private final JsonSchemaFactory schemaFactory;
    private final SchemaGuardProperties properties;

    public SchemaRegistryClient(WebClient.Builder webClientBuilder, SchemaGuardProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder.baseUrl(properties.getRegistryUrl()).build();
        this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @CircuitBreaker(name = "schemaRegistry", fallbackMethod = "failOpenFallback")
    @Retry(name = "schemaRegistry", fallbackMethod = "failOpenFallback")
    public JsonSchema fetchSchema(String schemaId) {
        log.debug("Fetching schema '{}' from central registry...", schemaId);
        
        String schemaJson = webClient.get()
                .uri("/{id}", schemaId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (schemaJson == null || schemaJson.isBlank()) {
            throw new RuntimeException("Received empty schema for ID: " + schemaId);
        }

        return schemaFactory.getSchema(schemaJson);
    }

    /**
     * Fallback method called when the API is unreachable or times out.
     * Implementing the "Fail-Open" strategy: we return null to signify 
     * that validation should be skipped (passed) rather than failing the message.
     */
    public JsonSchema failOpenFallback(String schemaId, Throwable t) {
        log.warn("Fail-Open triggered for schema '{}'. Returning null schema to bypass validation. Reason: {}", schemaId, t.getMessage());
        return null; // A null schema will bypass validation in the service
    }
}
