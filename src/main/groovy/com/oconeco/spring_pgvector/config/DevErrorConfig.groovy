package com.oconeco.spring_pgvector.config

import groovy.util.logging.Slf4j
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.context.request.WebRequest

/**
 * Development-specific error configuration that provides enhanced error details
 * while focusing on application code in stack traces.
 */
@Slf4j
@Configuration
@Profile("dev")
class DevErrorConfig {

    /**
     * Custom error attributes that provide full details during development
     * but filter stack traces to focus on application code.
     */
    @Bean
    DefaultErrorAttributes devErrorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, 
                    ErrorAttributeOptions.of(
                        ErrorAttributeOptions.Include.EXCEPTION,
                        ErrorAttributeOptions.Include.MESSAGE,
                        ErrorAttributeOptions.Include.BINDING_ERRORS,
                        ErrorAttributeOptions.Include.STACK_TRACE
                    )
                )
                
                // Get the exception from the request
                Throwable error = getError(webRequest)
                if (error != null) {
                    // Filter stack trace to only show application code
                    StackTraceElement[] stackTrace = error.getStackTrace()
                    if (stackTrace != null) {
                        StackTraceElement[] filteredTrace = filterStackTrace(stackTrace)
                        errorAttributes.put("filteredTrace", filteredTrace)
                        errorAttributes.put("hasApplicationCodeInTrace", filteredTrace.length > 0)
                    }
                }
                
                return errorAttributes
            }
            
            /**
             * Filter a stack trace to only show elements from application code.
             * @param stackTrace The full stack trace
             * @return A filtered stack trace containing only application code elements
             */
            private StackTraceElement[] filterStackTrace(StackTraceElement[] stackTrace) {
                if (stackTrace == null) return new StackTraceElement[0]
                
                return stackTrace.findAll { element ->
                    element.className.startsWith('com.oconeco.')
                } as StackTraceElement[]
            }
        }
    }
}
