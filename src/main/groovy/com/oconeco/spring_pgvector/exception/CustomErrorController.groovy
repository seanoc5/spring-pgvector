package com.oconeco.spring_pgvector.exception

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.thymeleaf.exceptions.TemplateInputException

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
                    
                    // Check for specific error types and provide more helpful messages
                    if (exception) {
                        Throwable rootCause = findRootCause(exception)
                        model.addAttribute("trace", exception.getStackTrace())
                        
                        // Check for template-related errors
                        if (rootCause instanceof TemplateInputException || 
                            rootCause.toString().contains("template") || 
                            rootCause.toString().contains("Template")) {
                            
                            model.addAttribute("friendlyMessage", "It looks like a template file is missing or has an error.")
                            
                            // Extract template name from error message if possible
                            String errorMsg = rootCause.toString()
                            if (errorMsg.contains("template")) {
                                int startIdx = errorMsg.indexOf("template")
                                int endIdx = errorMsg.indexOf(".html")
                                if (startIdx > 0 && endIdx > startIdx) {
                                    String templatePath = errorMsg.substring(startIdx, endIdx + 5)
                                    model.addAttribute("missingResource", templatePath)
                                }
                            }
                            
                            // If path contains admin/reports, suggest creating the missing report template
                            String pathStr = path?.toString() ?: ""
                            if (pathStr.contains("/admin/reports/")) {
                                model.addAttribute("suggestedAction", 
                                    "You may need to create the missing report template for: " + pathStr)
                            }
                        }
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
    
    /**
     * Find the root cause of an exception by unwrapping nested exceptions
     */
    private Throwable findRootCause(Throwable throwable) {
        Throwable rootCause = throwable
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause()
        }
        return rootCause
    }
}
