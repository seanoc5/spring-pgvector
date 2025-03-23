package com.oconeco.spring_pgvector.util

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Utility class for filtering and formatting stack traces to focus on application code.
 * This is particularly useful during development to help identify issues in your own code
 * without the noise of framework stack traces.
 */
@Slf4j
@Component
class StackTraceUtil {

    @Value('${app.debug.filter-stack-traces:true}')
    private boolean filterStackTraces
    
    @Value('${app.debug.application-packages:com.oconeco}')
    private List<String> applicationPackages
    
    /**
     * Filter a stack trace to only show elements from application code.
     * 
     * @param throwable The throwable to filter the stack trace from
     * @return A throwable with a filtered stack trace
     */
    Throwable filterStackTrace(Throwable throwable) {
        if (!filterStackTraces || throwable == null) {
            return throwable
        }
        
        StackTraceElement[] filteredTrace = filterStackTraceElements(throwable.getStackTrace())
        throwable.setStackTrace(filteredTrace)
        
        // Also filter any nested exceptions
        Throwable cause = throwable.getCause()
        if (cause != null && cause != throwable) {
            filterStackTrace(cause)
        }
        
        return throwable
    }
    
    /**
     * Filter stack trace elements to only include application code.
     * 
     * @param stackTrace The original stack trace elements
     * @return Filtered stack trace elements
     */
    StackTraceElement[] filterStackTraceElements(StackTraceElement[] stackTrace) {
        if (!filterStackTraces || stackTrace == null) {
            return stackTrace
        }
        
        return stackTrace.findAll { element ->
            applicationPackages.any { packagePrefix ->
                element.className.startsWith(packagePrefix)
            }
        } as StackTraceElement[]
    }
    
    /**
     * Get a string representation of a throwable with a filtered stack trace.
     * 
     * @param throwable The throwable to get a string representation of
     * @return A string representation of the throwable with a filtered stack trace
     */
    String getFilteredStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return ""
        }
        
        if (!filterStackTraces) {
            return getStackTraceAsString(throwable)
        }
        
        // Create a copy of the throwable to avoid modifying the original
        Throwable copy = cloneThrowable(throwable)
        filterStackTrace(copy)
        return getStackTraceAsString(copy)
    }
    
    /**
     * Create a simple clone of a throwable to avoid modifying the original.
     */
    private Throwable cloneThrowable(Throwable throwable) {
        try {
            Throwable clone = throwable.getClass().newInstance()
            clone.setStackTrace(throwable.getStackTrace())
            if (throwable.getCause() != null && throwable.getCause() != throwable) {
                clone.initCause(cloneThrowable(throwable.getCause()))
            }
            return clone
        } catch (Exception e) {
            // If cloning fails, just return the original
            log.warn("Failed to clone throwable: {}", e.getMessage())
            return throwable
        }
    }
    
    /**
     * Get a string representation of a throwable's stack trace.
     */
    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
}
