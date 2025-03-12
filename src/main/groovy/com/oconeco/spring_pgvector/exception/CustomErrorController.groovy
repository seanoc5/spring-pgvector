package com.oconeco.spring_pgvector.exception

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Custom error controller to handle Spring Boot's default error path.
 * This replaces the default WhiteLabel error page and routes to our custom error pages.
 */
@Controller
class CustomErrorController implements ErrorController {

    /**
     * Handle errors and route to the appropriate error template.
     */
    @RequestMapping("/error")
    String handleError(HttpServletRequest request, Model model) {
        // Get the error status
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE)
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)
        
        // Add common error attributes to the model
        model.addAttribute("timestamp", new Date())
        model.addAttribute("path", path ?: request.getRequestURI())
        model.addAttribute("message", message ?: "An error occurred")
        
        // Route to the appropriate error template based on status code
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString())
            model.addAttribute("status", statusCode)
            
            switch (statusCode) {
                case 404:
                    model.addAttribute("error", "Page Not Found")
                    return "error/404"
                case 403:
                    model.addAttribute("error", "Access Denied")
                    return "error/403"
                case 500:
                    model.addAttribute("error", "Internal Server Error")
                    if (exception) {
                        model.addAttribute("trace", exception.getStackTrace())
                    }
                    return "error/general"
                default:
                    model.addAttribute("error", HttpStatus.valueOf(statusCode).getReasonPhrase())
                    return "error/general"
            }
        }
        
        // Default to general error page
        model.addAttribute("status", 500)
        model.addAttribute("error", "Unknown Error")
        return "error/general"
    }
}
