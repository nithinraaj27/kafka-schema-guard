package com.schemaguard.starter;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.schemaguard.starter.error.SchemaValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.schemaguard.starter.config.SchemaGuardAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;

@SpringBootTest(
    classes = {
        SchemaGuardAutoConfiguration.class,
        KafkaAutoConfiguration.class,
        WebClientAutoConfiguration.class,
        JacksonAutoConfiguration.class
    },
    properties = {
        "schema-guard.registry-url=http://localhost:8089/schemas",
        "schema-guard.topics.payment-events=payment-schema-v1",
        "spring.kafka.producer.key-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
    }
)
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9093", "port=9093" }, topics = "payment-events")
@WireMockTest(httpPort = 8089)
public class SchemaGuardIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setupWireMock() {
        // Stub the Micro-SaaS Schema Registry to return a JSON Schema for "payment-schema-v1"
        // Require 'amount' and 'currency'
        String jsonSchema = """
                {
                  "$schema": "https://json-schema.org/draft/2020-12/schema",
                  "type": "object",
                  "properties": {
                    "amount": { "type": "number" },
                    "currency": { "type": "string" }
                  },
                  "required": ["amount", "currency"]
                }
                """;

        stubFor(get(urlEqualTo("/schemas/payment-schema-v1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonSchema)));
    }

    @Test
    void whenValidPayload_thenMessageIsPublishedSuccessfully() {
        // Prepare valid payload matching the schema
        Map<String, Object> validPayload = new HashMap<>();
        validPayload.put("amount", 150.0);
        validPayload.put("currency", "USD");

        // The interceptor should validate it cleanly and let it pass to Kafka
        assertDoesNotThrow(() -> {
            kafkaTemplate.send("payment-events", validPayload).get();
        });
    }

    @Test
    void whenInvalidPayload_thenThrowsSchemaValidationException() {
        // Prepare invalid payload missing the 'currency' field
        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("amount", 150.0);
        // missing currency!

        SchemaValidationException sve = assertThrows(SchemaValidationException.class, () -> {
            kafkaTemplate.send("payment-events", invalidPayload);
        });
        
        assertTrue(sve.getErrors().stream().anyMatch(e -> e.getClass().getSimpleName().equals("MissingField")));
    }
}
