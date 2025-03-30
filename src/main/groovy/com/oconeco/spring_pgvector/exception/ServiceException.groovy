package com.oconeco.spring_pgvector.exception

/**
 * Exception thrown when a service operation fails.
 */
class ServiceException extends BaseException {
    ServiceException(String message) {
        super(
            "SERVICE_ERROR",
            "An error occurred while processing your request",
            message
        )
    }

    ServiceException(String userMessage, String developerMessage) {
        super(
            "SERVICE_ERROR",
            userMessage,
            developerMessage
        )
    }
} 