package com.oconeco.spring_pgvector.exception

/**
 * Base exception class for all custom exceptions in the application.
 * Provides a consistent structure for error messages and codes.
 */
class BaseException extends RuntimeException {
    private final String errorCode
    private final String userMessage
    private final String developerMessage

    BaseException(String message) {
        super(message)
        this.errorCode = "INTERNAL_ERROR"
        this.userMessage = "An unexpected error occurred"
        this.developerMessage = message
    }

    BaseException(String errorCode, String userMessage, String developerMessage) {
        super(developerMessage)
        this.errorCode = errorCode
        this.userMessage = userMessage
        this.developerMessage = developerMessage
    }

    String getErrorCode() {
        return errorCode
    }

    String getUserMessage() {
        return userMessage
    }

    String getDeveloperMessage() {
        return developerMessage
    }
} 