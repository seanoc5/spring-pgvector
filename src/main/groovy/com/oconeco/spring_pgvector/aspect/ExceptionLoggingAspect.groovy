package com.oconeco.spring_pgvector.aspect

import com.oconeco.spring_pgvector.util.StackTraceUtil
import groovy.util.logging.Slf4j
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Aspect for logging exceptions with filtered stack traces.
 * This helps developers focus on application code during debugging.
 */
@Slf4j
@Aspect
@Component
class ExceptionLoggingAspect {

    @Autowired
    private StackTraceUtil stackTraceUtil
    
    /**
     * Log exceptions thrown from controller methods with filtered stack traces.
     */
    @AfterThrowing(
        pointcut = "execution(* com.oconeco.spring_pgvector.controller.*.*(..))",
        throwing = "ex"
    )
    void logControllerException(JoinPoint joinPoint, Throwable ex) {
        String methodName = joinPoint.getSignature().toShortString()
        log.error("Exception in controller method [{}]: {}", methodName, ex.getMessage())
        log.error("APPLICATION CODE STACK TRACE:\n{}", stackTraceUtil.getFilteredStackTraceAsString(ex))
    }
    
    /**
     * Log exceptions thrown from service methods with filtered stack traces.
     */
    @AfterThrowing(
        pointcut = "execution(* com.oconeco.spring_pgvector.service.*.*(..))",
        throwing = "ex"
    )
    void logServiceException(JoinPoint joinPoint, Throwable ex) {
        String methodName = joinPoint.getSignature().toShortString()
        log.error("Exception in service method [{}]: {}", methodName, ex.getMessage())
        log.error("APPLICATION CODE STACK TRACE:\n{}", stackTraceUtil.getFilteredStackTraceAsString(ex))
    }
    
    /**
     * Log exceptions thrown from repository methods with filtered stack traces.
     */
    @AfterThrowing(
        pointcut = "execution(* com.oconeco.spring_pgvector.repository.*.*(..))",
        throwing = "ex"
    )
    void logRepositoryException(JoinPoint joinPoint, Throwable ex) {
        String methodName = joinPoint.getSignature().toShortString()
        log.error("Exception in repository method [{}]: {}", methodName, ex.getMessage())
        log.error("APPLICATION CODE STACK TRACE:\n{}", stackTraceUtil.getFilteredStackTraceAsString(ex))
    }
}
