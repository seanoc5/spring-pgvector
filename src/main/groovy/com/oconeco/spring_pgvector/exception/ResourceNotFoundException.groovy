package com.oconeco.spring_pgvector.exception

/**
 * Exception thrown when a requested resource is not found.
 */
class ResourceNotFoundException extends BaseException {
    ResourceNotFoundException(String resourceType, String identifier) {
        super(
            "RESOURCE_NOT_FOUND",
            "The requested ${resourceType} was not found",
            "${resourceType} with identifier '${identifier}' not found"
        )
    }
} 