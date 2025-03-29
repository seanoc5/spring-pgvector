package com.oconeco.spring_pgvector.solr

import groovy.util.logging.Slf4j
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Aspect that intercepts JPA repository save and delete operations
 * to automatically sync with Solr.
 */
@Slf4j
@Aspect
@Component
class SolrSyncAspect {

    @Autowired
    private SolrSyncService solrSyncService

    /**
     * Pointcut for any save method in Spring Data JPA repositories
     */
    @Pointcut("execution(* org.springframework.data.jpa.repository.JpaRepository+.save*(..)) || " +
              "execution(* org.springframework.data.repository.CrudRepository+.save*(..))")
    void saveOperation() {}

    /**
     * Pointcut for any delete method in Spring Data JPA repositories
     */
    @Pointcut("execution(* org.springframework.data.jpa.repository.JpaRepository+.delete*(..)) || " +
              "execution(* org.springframework.data.repository.CrudRepository+.delete*(..))")
    void deleteOperation() {}

    /**
     * After a save operation completes, sync the entity with Solr
     */
    @AfterReturning(pointcut = "saveOperation()", returning = "result")
    void afterSave(JoinPoint joinPoint, Object result) {
        try {
            if (result == null) {
                return
            }

            if (result instanceof Iterable) {
                // Handle saveAll operations
                List entities = []
                result.each { entity ->
                    entities << entity
                }
                if (entities) {
                    log.debug("Syncing ${entities.size()} entities to Solr after repository save operation")
                    solrSyncService.saveAll(entities)
                }
            } else {
                // Handle single entity save
                log.debug("Syncing entity to Solr after repository save operation: ${result.class.simpleName}")
                solrSyncService.save(result)
            }
        } catch (Exception e) {
            log.error("Error syncing to Solr after save operation: ${e.message}", e)
        }
    }

    /**
     * After a delete operation completes, remove the entity from Solr
     */
    @AfterReturning("deleteOperation()")
    void afterDelete(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.args
            if (args.length == 0) {
                return
            }

            Object arg = args[0]
            if (arg instanceof Iterable) {
                // Handle deleteAll operations
                List entities = []
                arg.each { entity ->
                    entities << entity
                }
                if (entities) {
                    log.debug("Deleting ${entities.size()} entities from Solr after repository delete operation")
                    solrSyncService.deleteAll(entities)
                }
            } else if (arg != null && !(arg instanceof Number) && !(arg instanceof String)) {
                // Handle single entity delete (ignoring deleteById with just an ID)
                log.debug("Deleting entity from Solr after repository delete operation: ${arg.class.simpleName}")
                solrSyncService.delete(arg)
            }
        } catch (Exception e) {
            log.error("Error syncing to Solr after delete operation: ${e.message}", e)
        }
    }
}
