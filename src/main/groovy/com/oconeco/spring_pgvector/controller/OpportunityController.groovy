package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.domain.Opportunity
import com.oconeco.spring_pgvector.service.OpportunityService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * REST controller for managing SAM.gov contract opportunities.
 */
@RestController
@RequestMapping("/api/opportunities")
@Slf4j
class OpportunityController {

    @Autowired
    private OpportunityService opportunityService

    /**
     * Get all opportunities with pagination.
     */
    @GetMapping
    ResponseEntity<Page<Opportunity>> getAllOpportunities(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "lastPublishedDate") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        log.info "Getting all opportunities with pagination: page={}, size={}, sortBy={}, sortDir={}", page, size, sortBy, sortDir
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))

        Page<Opportunity> opportunities = opportunityService.getAllOpportunities(pageRequest)
        return ResponseEntity.ok(opportunities)
    }

    /**
     * Get a specific opportunity by its notice ID.
     */
    @GetMapping("/{noticeId}")
    ResponseEntity<Opportunity> getOpportunityByNoticeId(@PathVariable String noticeId) {
        Opportunity opportunity = opportunityService.getOpportunityByNoticeId(noticeId)

        log.info "Getting opportunity by notice ID: {}", noticeId
        if (opportunity) {
            return ResponseEntity.ok(opportunity)
        } else {
            return ResponseEntity.notFound().build()
        }
    }

    /**
     * Search opportunities using full-text search.
     */
    @GetMapping("/search")
    ResponseEntity<Page<Map<String, Object>>> searchOpportunities(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        log.info "Searching opportunities: query={}, page={}, size={}", query, page, size
        try {
            PageRequest pageRequest = PageRequest.of(page, size)
            Page<Map<String, Object>> results = opportunityService.searchOpportunities(query, pageRequest)
            return ResponseEntity.ok(results)
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null)
        } catch (Exception e) {
            log.error "Error searching opportunities: ${e.message}", e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }

    /**
     * Import opportunities from a CSV file.
     */
    @PostMapping("/import")
    ResponseEntity<Map<String, Object>> importFromCsv(@RequestParam(name = "file") MultipartFile file) {
        log.info "Importing opportunities from CSV: $file"
        try {
            // Create a temporary file
            File tempFile = File.createTempFile("opportunities", ".csv")
            file.transferTo(tempFile)

            // Import the data
            int count = opportunityService.importFromCsv(tempFile)

            // Clean up
            tempFile.delete()

            return ResponseEntity.ok([
                status: "success",
                message: "Successfully imported ${count} opportunities",
                count: count
            ])
        } catch (Exception e) {
            log.error "Error importing CSV: ${e.message}", e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body([
                status: "error",
                message: "Error importing CSV: ${e.message}"
            ])
        }
    }
}
