package com.oconeco.spring_pgvector.exception

import groovy.util.logging.Slf4j
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
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
     * Handle parameter name missing exceptions.
     * This occurs when @RequestParam in Groovy doesn't have an explicit name parameter.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ModelAndView handleParameterNameMissingException(IllegalArgumentException ex, HttpServletRequest request) {
        String message = ex.getMessage()
        
        // Check if this is the specific parameter name error
        if (message?.contains("Name for argument of type") && message?.contains("not specified")) {
            log.error("Parameter name missing in controller method: {}", message)
            
            ModelAndView modelAndView = new ModelAndView("error/400")
            modelAndView.addObject("timestamp", new Date())
            modelAndView.addObject("status", HttpStatus.BAD_REQUEST.value())
            modelAndView.addObject("error", "Missing Parameter Name")
            modelAndView.addObject("message", "A controller method is missing an explicit parameter name. In Groovy, all @RequestParam parameters must include a 'name' attribute. For example: @RequestParam(name = 'query') String query")
            modelAndView.addObject("path", request.getRequestURI())
            modelAndView.addObject("developerMessage", "Check controller methods and ensure all @RequestParam annotations include a 'name' attribute.")
            
            return modelAndView
        }
        
        // If it's a different IllegalArgumentException, delegate to the general exception handler
        return handleException(ex, request)
    }

    /**
     * Handle general exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ModelAndView handleException(Exception ex, HttpServletRequest request) {
        log.error("General Exception occurred: {}", ex.getMessage(), ex)

        ModelAndView modelAndView = new ModelAndView("error/general")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
        modelAndView.addObject("error", "Internal Server Error")
        modelAndView.addObject("message", ex.getMessage())
        modelAndView.addObject("path", request.getRequestURI())
        
        // Filter stack trace to only show application code
        def filteredTrace = filterStackTrace(ex.getStackTrace())
        modelAndView.addObject("trace", filteredTrace)
        modelAndView.addObject("fullTrace", ex.getStackTrace()) // Keep full trace available if needed

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
        modelAndView.addObject("error", "Template Not Found")
        modelAndView.addObject("message", ex.getMessage())
        modelAndView.addObject("path", request.getRequestURI())

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
        modelAndView.addObject("error", "Resource Not Found")
        modelAndView.addObject("message", "The static resource you are looking for could not be found: " + ex.getMessage())
        modelAndView.addObject("path", request.getRequestURI())

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
        modelAndView.addObject("error", "Page Not Found")
        modelAndView.addObject("message", "The page you are looking for does not exist")
        modelAndView.addObject("path", request.getRequestURI())

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
        modelAndView.addObject("error", "Access Denied")
        modelAndView.addObject("message", "You do not have permission to access this resource")
        modelAndView.addObject("path", request.getRequestURI())

        return modelAndView
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
