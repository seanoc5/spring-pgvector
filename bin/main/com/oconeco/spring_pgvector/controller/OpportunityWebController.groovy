package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.domain.Opportunity
import com.oconeco.spring_pgvector.exception.ResourceNotFoundException
import com.oconeco.spring_pgvector.exception.ServiceException
import com.oconeco.spring_pgvector.exception.ValidationException
import com.oconeco.spring_pgvector.service.OpportunityService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * Web controller for SAM.gov contract opportunities.
 * Provides web pages for viewing and searching opportunities.
 */
@Controller
@RequestMapping("/opportunities")
@Slf4j
class OpportunityWebController {

    @Autowired
    private OpportunityService opportunityService

    /**
     * Display the opportunities list page.
     */
    @GetMapping(["", "/"])
    String listOpportunities(
            Model model,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "lastPublishedDate") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))

        Page<Opportunity> opportunities = opportunityService.getAllOpportunities(pageRequest)

        model.addAttribute("opportunities", opportunities)
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", opportunities.totalPages)
        model.addAttribute("totalItems", opportunities.totalElements)
        model.addAttribute("sortBy", sortBy)
        model.addAttribute("sortDir", sortDir)
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc")

        return "opportunities/list"
    }

    /**
     * Display a specific opportunity.
     */
    @GetMapping("/{noticeId}")
    String viewOpportunity(@PathVariable String noticeId, Model model) {
        Opportunity opportunity = opportunityService.getOpportunityByNoticeId(noticeId)

        if (opportunity) {
            model.addAttribute("opportunity", opportunity)
            return "opportunities/view"
        } else {
            return "redirect:/opportunities"
        }
    }

    /**
     * Display the create opportunity form.
     */
    @GetMapping("/create")
    String createOpportunityForm(Model model) {
        model.addAttribute("opportunity", new Opportunity())
        return "opportunities/form"
    }

    /**
     * Handle opportunity creation.
     */
    @PostMapping
    String createOpportunity(
            @ModelAttribute Opportunity opportunity,
            RedirectAttributes redirectAttributes) {
        try {
            Opportunity saved = opportunityService.createOpportunity(opportunity)
            redirectAttributes.addFlashAttribute("success", "Opportunity created successfully")
            return "redirect:/opportunities/${saved.noticeId}"
        } catch (ValidationException e) {
            log.error "Validation error creating opportunity: ${e.developerMessage}", e
            redirectAttributes.addFlashAttribute("error", e.userMessage)
            return "redirect:/opportunities/create"
        } catch (ServiceException e) {
            log.error "Service error creating opportunity: ${e.developerMessage}", e
            redirectAttributes.addFlashAttribute("error", e.userMessage)
            return "redirect:/opportunities/create"
        }
    }

    /**
     * Display the edit opportunity form.
     */
    @GetMapping("/{noticeId}/edit")
    String editOpportunityForm(@PathVariable String noticeId, Model model) {
        try {
            Opportunity opportunity = opportunityService.getOpportunityByNoticeId(noticeId)
            if (opportunity) {
                model.addAttribute("opportunity", opportunity)
                return "opportunities/form"
            } else {
                throw new ResourceNotFoundException("Opportunity", noticeId)
            }
        } catch (ResourceNotFoundException e) {
            log.error "Resource not found: ${e.developerMessage}", e
            return "redirect:/opportunities"
        }
    }

    /**
     * Handle opportunity update.
     */
    @PostMapping("/{noticeId}")
    String updateOpportunity(
            @PathVariable String noticeId,
            @ModelAttribute Opportunity opportunity,
            RedirectAttributes redirectAttributes) {
        try {
            Opportunity updated = opportunityService.updateOpportunity(noticeId, opportunity)
            redirectAttributes.addFlashAttribute("success", "Opportunity updated successfully")
            return "redirect:/opportunities/${updated.noticeId}"
        } catch (ValidationException e) {
            log.error "Validation error updating opportunity: ${e.developerMessage}", e
            redirectAttributes.addFlashAttribute("error", e.userMessage)
            return "redirect:/opportunities/${noticeId}/edit"
        } catch (ServiceException e) {
            log.error "Service error updating opportunity: ${e.developerMessage}", e
            redirectAttributes.addFlashAttribute("error", e.userMessage)
            return "redirect:/opportunities/${noticeId}/edit"
        } catch (ResourceNotFoundException e) {
            log.error "Resource not found: ${e.developerMessage}", e
            redirectAttributes.addFlashAttribute("error", e.userMessage)
            return "redirect:/opportunities"
        }
    }

    /**
     * Handle opportunity deletion.
     */
    @DeleteMapping("/{noticeId}")
    @ResponseBody
    Map<String, String> deleteOpportunity(@PathVariable String noticeId) {
        try {
            opportunityService.deleteOpportunity(noticeId)
            return [success: "true", message: "Opportunity deleted successfully"]
        } catch (Exception e) {
            log.error "Error deleting opportunity: ${e.message}", e
            return [success: "false", message: "Error deleting opportunity: ${e.message}"]
        }
    }

    /**
     * Display the search page.
     */
    @GetMapping("/search")
    String searchPage() {
        return "opportunities/search"
    }

    /**
     * Perform a search and display results.
     */
    @GetMapping("/search/results")
    String searchResults(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Model model) {

        try {
            PageRequest pageRequest = PageRequest.of(page, size)
            Page<Map<String, Object>> results = opportunityService.searchOpportunities(query, pageRequest)

            model.addAttribute("query", query)
            model.addAttribute("results", results)
            model.addAttribute("currentPage", page)
            model.addAttribute("totalPages", results.totalPages)
            model.addAttribute("totalItems", results.totalElements)

            return "opportunities/search-results"
        } catch (Exception e) {
            log.error "Error searching opportunities: ${e.message}", e
            model.addAttribute("error", "An error occurred while searching: ${e.message}")
            return "opportunities/search"
        }
    }

    /**
     * Display the CSV import page.
     */
    @GetMapping("/import")
    String importPage() {
        return "opportunities/import"
    }

    /**
     * Handle CSV file upload and import.
     */
    @PostMapping("/import")
    String importCsv(
            @RequestParam(name = "file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        if (file.empty) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file to upload")
            return "redirect:/opportunities/import"
        }

        try {
            // Create a temporary file
            File tempFile = File.createTempFile("opportunities", ".csv")
            file.transferTo(tempFile)

            // Import the data
            int count = opportunityService.importFromCsv(tempFile)

            // Clean up
            tempFile.delete()

            redirectAttributes.addFlashAttribute("success", "Successfully imported ${count} opportunities")
            return "redirect:/opportunities"
        } catch (Exception e) {
            log.error "Error importing CSV: ${e.message}", e
            redirectAttributes.addFlashAttribute("error", "Error importing CSV: ${e.message}")
            return "redirect:/opportunities/import"
        }
    }
}
