package com.oconeco.spring_pgvector.solr

import groovy.util.logging.Slf4j
import jakarta.persistence.PostPersist
import jakarta.persistence.PostRemove
import jakarta.persistence.PostUpdate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * JPA Entity Listener that automatically synchronizes entities with Solr
 * on create, update, and delete operations.
 */
@Slf4j
@Component
class SolrEntityListener {
    
    private static ApplicationContext applicationContext
    
    @Autowired
    void setApplicationContext(ApplicationContext applicationContext) {
        SolrEntityListener.applicationContext = applicationContext
    }
    
    private static SolrSyncService getSolrSyncService() {
        return applicationContext.getBean(SolrSyncService)
    }
    
    @PostPersist
    void onPostPersist(Object entity) {
        try {
            log.debug("Entity persisted, syncing to Solr: ${entity.class.simpleName}")
            getSolrSyncService().save(entity)
        } catch (Exception e) {
            // Log but don't throw to avoid disrupting the transaction
            log.error("Error syncing entity to Solr after persist: ${e.message}", e)
        }
    }
    
    @PostUpdate
    void onPostUpdate(Object entity) {
        try {
            log.debug("Entity updated, syncing to Solr: ${entity.class.simpleName}")
            getSolrSyncService().save(entity)
        } catch (Exception e) {
            log.error("Error syncing entity to Solr after update: ${e.message}", e)
        }
    }
    
    @PostRemove
    void onPostRemove(Object entity) {
        try {
            log.debug("Entity removed, deleting from Solr: ${entity.class.simpleName}")
            getSolrSyncService().delete(entity)
        } catch (Exception e) {
            log.error("Error deleting entity from Solr after remove: ${e.message}", e)
        }
    }
}
