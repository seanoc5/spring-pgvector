package com.oconeco.spring_pgvector.exception

import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException
import org.thymeleaf.exceptions.TemplateInputException

import java.nio.file.AccessDeniedException

/**
 * Global exception handler that provides centralized exception handling for the application.
 * This handles various exceptions and provides appropriate error pages or responses.
 */
@Slf4j
@ControllerAdvice
class GlobalExceptionHandler {
//    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class)

    /**
     * Handle custom base exceptions.
     */
    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Object handleBaseException(BaseException ex, HttpServletRequest request) {
        log.error("Base exception occurred: {}", ex.getDeveloperMessage(), ex)
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body([
                    success: false,
                    error: [
                        code: ex.getErrorCode(),
                        message: ex.getUserMessage(),
                        developerMessage: ex.getDeveloperMessage()
                    ]
                ])
        }

        ModelAndView modelAndView = new ModelAndView("error/general")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
        modelAndView.addObject("error", ex.getErrorCode())
        modelAndView.addObject("message", ex.getUserMessage())
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("developerMessage", ex.getDeveloperMessage())
        
        return modelAndView
    }

    /**
     * Handle resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Object handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.error("Resource not found: {}", ex.getDeveloperMessage(), ex)
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body([
                    success: false,
                    error: [
                        code: ex.getErrorCode(),
                        message: ex.getUserMessage(),
                        developerMessage: ex.getDeveloperMessage()
                    ]
                ])
        }

        ModelAndView modelAndView = new ModelAndView("error/404")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.NOT_FOUND.value())
        modelAndView.addObject("error", ex.getErrorCode())
        modelAndView.addObject("message", ex.getUserMessage())
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("developerMessage", ex.getDeveloperMessage())
        
        return modelAndView
    }

    /**
     * Handle validation exceptions.
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Object handleValidationException(ValidationException ex, HttpServletRequest request) {
        log.error("Validation error: {}", ex.getDeveloperMessage(), ex)
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body([
                    success: false,
                    error: [
                        code: ex.getErrorCode(),
                        message: ex.getUserMessage(),
                        developerMessage: ex.getDeveloperMessage()
                    ]
                ])
        }

        ModelAndView modelAndView = new ModelAndView("error/400")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.BAD_REQUEST.value())
        modelAndView.addObject("error", ex.getErrorCode())
        modelAndView.addObject("message", ex.getUserMessage())
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("developerMessage", ex.getDeveloperMessage())
        
        return modelAndView
    }

    /**
     * Handle template not found exceptions.
     */
    @ExceptionHandler(TemplateInputException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ModelAndView handleTemplateInputException(TemplateInputException ex, HttpServletRequest request) {
        log.error("Template not found: {}", ex.getMessage(), ex)

        ModelAndView modelAndView = new ModelAndView("error/template-error")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.NOT_FOUND.value())
        modelAndView.addObject("error", "TEMPLATE_NOT_FOUND")
        modelAndView.addObject("message", "The requested page template could not be found")
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("developerMessage", ex.getMessage())

        return modelAndView
    }

    /**
     * Handle static resource not found exceptions.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ModelAndView handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.error("Static resource not found: {}", ex.getMessage())

        ModelAndView modelAndView = new ModelAndView("error/404")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.NOT_FOUND.value())
        modelAndView.addObject("error", "RESOURCE_NOT_FOUND")
        modelAndView.addObject("message", "The requested resource could not be found")
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("developerMessage", ex.getMessage())

        return modelAndView
    }

    /**
     * Handle 404 errors.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ModelAndView handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.error("Page not found: {}", ex.getMessage())

        ModelAndView modelAndView = new ModelAndView("error/404")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.NOT_FOUND.value())
        modelAndView.addObject("error", "PAGE_NOT_FOUND")
        modelAndView.addObject("message", "The requested page does not exist")
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("developerMessage", ex.getMessage())

        return modelAndView
    }

    /**
     * Handle 403 forbidden errors.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ModelAndView handleAccessDenied(Exception ex, HttpServletRequest request) {
        log.error("Access denied: {}", ex.getMessage())

        ModelAndView modelAndView = new ModelAndView("error/403")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.FORBIDDEN.value())
        modelAndView.addObject("error", "ACCESS_DENIED")
        modelAndView.addObject("message", "You do not have permission to access this resource")
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("developerMessage", ex.getMessage())

        return modelAndView
    }

    /**
     * Handle general exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Object handleException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex)

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body([
                    success: false,
                    error: [
                        code: "INTERNAL_ERROR",
                        message: "An unexpected error occurred",
                        developerMessage: ex.getMessage()
                    ]
                ])
        }

        ModelAndView modelAndView = new ModelAndView("error/general")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
        modelAndView.addObject("error", "INTERNAL_ERROR")
        modelAndView.addObject("message", "An unexpected error occurred")
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("developerMessage", ex.getMessage())
        
        return modelAndView
    }

    /**
     * Check if the request is an API request.
     */
    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/")
    }
}
