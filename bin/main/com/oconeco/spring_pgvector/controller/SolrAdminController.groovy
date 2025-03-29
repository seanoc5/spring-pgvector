package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.service.SolrAdminService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for Solr admin operations like reindexing.
 */
@RestController
@RequestMapping("/api/solr")
@Slf4j
class SolrAdminController {

    @Autowired
    private SolrAdminService solrAdminService
    
    /**
     * Reindex all NAICS codes to Solr.
     * @return A response with the number of records reindexed
     */
    @PostMapping("/reindex/naics")
    ResponseEntity<Map<String, Object>> reindexNaicsCodes() {
        log.info("Received request to reindex all NAICS codes to Solr")
        
        try {
            int count = solrAdminService.reindexNaicsCodes()
            return ResponseEntity.ok([
                success: true,
                message: "Successfully reindexed ${count} NAICS codes to Solr",
                count: count
            ])
        } catch (Exception e) {
            log.error("Error reindexing NAICS codes to Solr", e)
            return ResponseEntity.internalServerError().body([
                success: false,
                message: "Error reindexing NAICS codes: ${e.message}"
            ])
        }
    }
    
    /**
     * Reindex all opportunities to Solr.
     * @return A response with the number of records reindexed
     */
    @PostMapping("/reindex/opportunities")
    ResponseEntity<Map<String, Object>> reindexOpportunities() {
        log.info("Received request to reindex all opportunities to Solr")
        
        try {
            int count = solrAdminService.reindexOpportunities()
            return ResponseEntity.ok([
                success: true,
                message: "Successfully reindexed ${count} opportunities to Solr",
                count: count
            ])
        } catch (Exception e) {
            log.error("Error reindexing opportunities to Solr", e)
            return ResponseEntity.internalServerError().body([
                success: false,
                message: "Error reindexing opportunities: ${e.message}"
            ])
        }
    }
    
    /**
     * Reindex all entities to Solr.
     * @return A response with the number of records reindexed by entity type
     */
    @PostMapping("/reindex/all")
    ResponseEntity<Map<String, Object>> reindexAll() {
        log.info("Received request to reindex all entities to Solr")
        
        try {
            Map<String, Integer> counts = solrAdminService.reindexAll()
            
            return ResponseEntity.ok([
                success: true,
                message: "Successfully reindexed all entities to Solr",
                counts: counts
            ])
        } catch (Exception e) {
            log.error("Error reindexing all entities to Solr", e)
            return ResponseEntity.internalServerError().body([
                success: false,
                message: "Error reindexing all entities: ${e.message}"
            ])
        }
    }
}
