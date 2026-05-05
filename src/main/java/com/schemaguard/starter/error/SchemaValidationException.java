package com.schemaguard.starter.error;

import java.util.List;

public class SchemaValidationException extends RuntimeException {

    private final List<SchemaValidationError> errors;
    private final String topic;

    public SchemaValidationException(String message, String topic, List<SchemaValidationError> errors) {
        super(message);
        this.topic = topic;
        this.errors = errors;
    }

    public List<SchemaValidationError> getErrors() {
        return errors;
    }

    public String getTopic() {
        return topic;
    }
}
