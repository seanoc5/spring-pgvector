package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.service.SolrAdminService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Web controller for Solr admin interface.
 */
@Controller
@RequestMapping("/admin/solr")
@Slf4j
class SolrAdminWebController {

    @Value('${solr.host}')
    private String solrHost
    
    @Value('${solr.collection}')
    private String solrCollection
    
    @Autowired
    private SolrAdminService solrAdminService
    
    /**
     * Display the Solr admin dashboard.
     * @param model The Spring MVC model
     * @return The name of the template to render
     */
    @GetMapping
    String solrAdminDashboard(Model model) {
        log.info("Displaying Solr admin dashboard")
        
        model.addAttribute("solrUrl", solrHost)
        model.addAttribute("solrCollection", solrCollection)
        
        return "solr-admin"
    }
    
    /**
     * Reindex all NAICS codes to Solr.
     * @return A JSON response with the number of records reindexed
     */
    @PostMapping("/reindex/naics")
    @ResponseBody
    Map<String, Object> reindexNaicsCodes() {
        log.info("Received web request to reindex all NAICS codes to Solr")
        
        try {
            int count = solrAdminService.reindexNaicsCodes()
            return [
                success: true,
                message: "Successfully reindexed ${count} NAICS codes to Solr",
                count: count
            ]
        } catch (Exception e) {
            log.error("Error reindexing NAICS codes to Solr", e)
            return [
                success: false,
                message: "Error reindexing NAICS codes: ${e.message}"
            ]
        }
    }
    
    /**
     * Reindex all opportunities to Solr.
     * @return A JSON response with the number of records reindexed
     */
    @PostMapping("/reindex/opportunities")
    @ResponseBody
    Map<String, Object> reindexOpportunities() {
        log.info("Received web request to reindex all opportunities to Solr")
        
        try {
            int count = solrAdminService.reindexOpportunities()
            return [
                success: true,
                message: "Successfully reindexed ${count} opportunities to Solr",
                count: count
            ]
        } catch (Exception e) {
            log.error("Error reindexing opportunities to Solr", e)
            return [
                success: false,
                message: "Error reindexing opportunities: ${e.message}"
            ]
        }
    }
    
    /**
     * Reindex all entities to Solr.
     * @return A JSON response with the number of records reindexed by entity type
     */
    @PostMapping("/reindex/all")
    @ResponseBody
    Map<String, Object> reindexAll() {
        log.info("Received web request to reindex all entities to Solr")
        
        try {
            Map<String, Integer> counts = solrAdminService.reindexAll()
            
            return [
                success: true,
                message: "Successfully reindexed all entities to Solr",
                counts: counts
            ]
        } catch (Exception e) {
            log.error("Error reindexing all entities to Solr", e)
            return [
                success: false,
                message: "Error reindexing all entities: ${e.message}"
            ]
        }
    }
}
