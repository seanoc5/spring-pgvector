package com.oconeco.spring_pgvector.service

import com.oconeco.spring_pgvector.service.NaicsCodeService
import com.oconeco.spring_pgvector.service.OpportunityService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service for Solr admin operations like reindexing.
 */
@Service
@Slf4j
class SolrAdminService {

    @Autowired
    private NaicsCodeService naicsCodeService
    
    @Autowired
    private OpportunityService opportunityService
    
    /**
     * Reindex all NAICS codes to Solr.
     * @return The number of records reindexed
     */
    int reindexNaicsCodes() {
        log.info("Reindexing all NAICS codes to Solr")
        return naicsCodeService.reindexAllToSolr()
    }
    
    /**
     * Reindex all opportunities to Solr.
     * @return The number of records reindexed
     */
    int reindexOpportunities() {
        log.info("Reindexing all opportunities to Solr")
        return opportunityService.reindexAllToSolr()
    }
    
    /**
     * Reindex all entities to Solr.
     * @return A map with counts of reindexed records by entity type
     */
    Map<String, Integer> reindexAll() {
        log.info("Reindexing all entities to Solr")
        
        int naicsCount = naicsCodeService.reindexAllToSolr()
        int opportunityCount = opportunityService.reindexAllToSolr()
        
        return [
            naicsCodes: naicsCount,
            opportunities: opportunityCount,
            total: naicsCount + opportunityCount
        ]
    }
}
