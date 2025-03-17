package com.oconeco.controller

import com.oconeco.entity.Opportunity
import com.oconeco.service.OpportunityService
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
    @GetMapping
    String listOpportunities(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastPublishedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
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
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
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
            @RequestParam("file") MultipartFile file,
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
