package com.oconeco.spring_pgvector.exception

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.NoHandlerFoundException
import org.thymeleaf.exceptions.TemplateInputException

import jakarta.servlet.http.HttpServletRequest

import java.nio.file.AccessDeniedException

/**
 * Global exception handler that provides centralized exception handling for the application.
 * This handles various exceptions and provides appropriate error pages or responses.
 */
@ControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class)


    /**
     * Handle general exceptions.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ModelAndView handleException(Exception ex, HttpServletRequest request) {
        log.error("Exception occurred: {}", ex.getMessage(), ex)

        ModelAndView modelAndView = new ModelAndView("error/general")
        modelAndView.addObject("timestamp", new Date())
        modelAndView.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
        modelAndView.addObject("error", "Internal Server Error")
        modelAndView.addObject("message", ex.getMessage())
        modelAndView.addObject("path", request.getRequestURI())
        modelAndView.addObject("trace", ex.getStackTrace())

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
}
