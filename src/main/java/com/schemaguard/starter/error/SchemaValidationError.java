package com.schemaguard.starter.error;

/**
 * Exhaustive error states representing the possible failure modes 
 * when validating a Kafka message against a schema.
 */
public sealed interface SchemaValidationError permits 
    SchemaValidationError.MissingField, 
    SchemaValidationError.TypeMismatch,
    SchemaValidationError.FormatMismatch,
    SchemaValidationError.SchemaFetchError {

    record MissingField(String fieldName, String path, String message) implements SchemaValidationError {}
    record TypeMismatch(String fieldName, String expectedType, String actualType, String message) implements SchemaValidationError {}
    record FormatMismatch(String fieldName, String expectedFormat, String message) implements SchemaValidationError {}
    record SchemaFetchError(String reason) implements SchemaValidationError {}
}
