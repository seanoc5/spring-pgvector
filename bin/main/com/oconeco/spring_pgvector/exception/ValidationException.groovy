package com.oconeco.spring_pgvector.exception

/**
 * Exception thrown when validation fails.
 */
class ValidationException extends BaseException {
    ValidationException(String message) {
        super(
            "VALIDATION_ERROR",
            "The provided data is invalid",
            message
        )
    }

    ValidationException(String userMessage, String developerMessage) {
        super(
            "VALIDATION_ERROR",
            userMessage,
            developerMessage
        )
    }
} 