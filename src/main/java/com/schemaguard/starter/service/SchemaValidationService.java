package com.schemaguard.starter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import com.schemaguard.starter.client.SchemaRegistryClient;
import com.schemaguard.starter.config.SchemaGuardProperties;
import com.schemaguard.starter.error.SchemaValidationError;
import com.schemaguard.starter.error.SchemaValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SchemaValidationService {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidationService.class);

    private final SchemaRegistryClient registryClient;
    private final Cache<String, JsonSchema> schemaCache;
    private final SchemaGuardProperties properties;
    private final ObjectMapper objectMapper;

    public SchemaValidationService(
            SchemaRegistryClient registryClient,
            Cache<String, JsonSchema> schemaCache,
            SchemaGuardProperties properties,
            ObjectMapper objectMapper) {
        this.registryClient = registryClient;
        this.schemaCache = schemaCache;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates the payload against the schema mapped to the topic.
     * 
     * @param topic   The Kafka topic
     * @param payload The raw JSON payload (or object, we convert to JsonNode)
     */
    public void validate(String topic, Object payload) {
        if (!properties.isEnabled()) {
            return;
        }

        String schemaId = properties.getTopics().get(topic);
        if (schemaId == null) {
            log.trace("No schema configured for topic: {}", topic);
            return; // No schema defined for this topic
        }

        // Fetch schema (with Cache and Fail-Open fallback)
        JsonSchema schema = schemaCache.get(schemaId, registryClient::fetchSchema);

        // Fail-Open state: Client returns null when down
        if (schema == null) {
            log.debug("Fail-Open: Bypassing validation for topic {} due to registry unavailability.", topic);
            return;
        }

        // Validate payload
        try {
            JsonNode jsonNode;
            if (payload instanceof String || payload instanceof byte[]) {
                jsonNode = objectMapper.readTree((String) payload);
            } else {
                jsonNode = objectMapper.valueToTree(payload);
            }

            Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

            if (!validationMessages.isEmpty()) {
                List<SchemaValidationError> errors = mapValidationMessages(validationMessages);
                throw new SchemaValidationException(
                        "Payload validation failed for topic: " + topic,
                        topic,
                        errors
                );
            }
        } catch (SchemaValidationException e) {
            throw e; // rethrow explicitly thrown exceptions
        } catch (Exception e) {
            // Unhandled parsing exceptions
            throw new RuntimeException("Failed to process payload for schema validation", e);
        }
    }

    private List<SchemaValidationError> mapValidationMessages(Set<ValidationMessage> messages) {
        List<SchemaValidationError> mappedErrors = new ArrayList<>();
        
        for (ValidationMessage msg : messages) {
            String type = msg.getType(); // e.g. "required", "type", "format"
            String path = msg.getInstanceLocation() != null ? msg.getInstanceLocation().toString() : "unknown";

            if ("required".equals(type)) {
                String missingProperty = msg.getArguments().length > 0 ? String.valueOf(msg.getArguments()[0]) : "unknown";
                mappedErrors.add(new SchemaValidationError.MissingField(
                        missingProperty, path, msg.getMessage()
                ));
            } else if ("type".equals(type)) {
                String expected = msg.getArguments().length > 0 ? String.valueOf(msg.getArguments()[0]) : "unknown";
                String actual = msg.getArguments().length > 1 ? String.valueOf(msg.getArguments()[1]) : "unknown";
                mappedErrors.add(new SchemaValidationError.TypeMismatch(
                        path, expected, actual, msg.getMessage()
                ));
            } else if ("format".equals(type)) {
                String format = msg.getArguments().length > 0 ? String.valueOf(msg.getArguments()[0]) : "unknown";
                mappedErrors.add(new SchemaValidationError.FormatMismatch(
                        path, format, msg.getMessage()
                ));
            } else {
                // Catch all as SchemaFetchError or similar for simplicity, 
                // in real production you might add more permutations
                mappedErrors.add(new SchemaValidationError.SchemaFetchError(msg.getMessage()));
            }
        }
        
        return mappedErrors;
    }
}
