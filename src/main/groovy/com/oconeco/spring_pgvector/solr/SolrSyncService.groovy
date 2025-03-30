package com.oconeco.spring_pgvector.solr

import com.oconeco.spring_pgvector.configuration.SolrProperties
import groovy.util.logging.Slf4j
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.response.UpdateResponse
import org.apache.solr.common.SolrInputDocument
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Service for synchronizing domain objects with Solr
 */
@Slf4j
@Service
class SolrSyncService {

    private final SolrClient solrClient
    private final SolrProperties solrProperties
    private final Map<Class<?>, SolrDocumentConverter<?>> converters = [:]

    @Autowired
    SolrSyncService(
            SolrClient solrClient,
            SolrProperties solrProperties,
            List<SolrDocumentConverter<?>> documentConverters) {
        this.solrClient = solrClient
        this.solrProperties = solrProperties

        // Register all converters by their entity type
        documentConverters.each { converter ->
            Class<?> entityType = converter.getEntityType()
            converters[entityType] = converter
            log.info("Registered SolrDocumentConverter for entity type: ${entityType.simpleName}")
        }
    }

    /**
     * Save an entity to Solr
     * @param entity The entity to save
     * @return true if successful, false otherwise
     */
    boolean save(Object entity) {
        SolrDocumentConverter converter = getConverter(entity.class)
        if (!converter) {
            log.warn("No SolrDocumentConverter found for entity type: ${entity.class.simpleName}")
            return false
        }

        try {
            SolrInputDocument doc = converter.toSolrDocument(entity)
            UpdateResponse response = solrClient.add(solrProperties.collection, doc)
            solrClient.commit(solrProperties.collection)
            log.debug("Saved entity to Solr: ${converter.getSolrId(entity)} -- update response: ${response}")
            return true
        } catch (IOException | SolrServerException e) {
            log.error("Error saving entity to Solr: ${e.message}", e)
            return false
        }
    }

    /**
     * Save multiple entities to Solr
     * @param entities The entities to save
     * @return The number of successfully saved entities
     */
    int saveAll(Collection<?> entities) {
        if (!entities) return 0

        // Group entities by type for batch processing
        Map<Class<?>, List<Object>> entitiesByType = entities.groupBy { it.class }
        int savedCount = 0

        entitiesByType.each { entityClass, entityList ->
            SolrDocumentConverter converter = getConverter(entityClass)
            if (!converter) {
                log.warn("No SolrDocumentConverter found for entity type: ${entityClass.simpleName}")
                return
            }

            try {
                List<SolrInputDocument> docs = entityList.collect { entity ->
                    converter.toSolrDocument(entity)
                }

                UpdateResponse respose = solrClient.add(solrProperties.collection, docs)
                log.info("\t\tUpdate response: ${respose}")
                solrClient.commit(solrProperties.collection)
                savedCount += entityList.size()
                log.debug("Saved ${entityList.size()} ${entityClass.simpleName} entities to Solr")
            } catch (IOException | SolrServerException e) {
                log.error("Error batch saving entities to Solr: ${e.message}", e)
            }
        }

        return savedCount
    }

    /**
     * Delete an entity from Solr
     * @param entity The entity to delete
     * @return true if successful, false otherwise
     */
    boolean delete(Object entity) {
        SolrDocumentConverter converter = getConverter(entity.class)
        if (!converter) {
            log.warn("No SolrDocumentConverter found for entity type: ${entity.class.simpleName}")
            return false
        }

        try {
            String id = converter.getSolrId(entity)
            solrClient.deleteById(solrProperties.collection, id)
            solrClient.commit(solrProperties.collection)
            log.debug("Deleted entity from Solr: ${id}")
            return true
        } catch (IOException | SolrServerException e) {
            log.error("Error deleting entity from Solr: ${e.message}", e)
            return false
        }
    }

    /**
     * Delete multiple entities from Solr
     * @param entities The entities to delete
     * @return The number of successfully deleted entities
     */
    int deleteAll(Collection<?> entities) {
        if (!entities) return 0

        // Group entities by type for batch processing
        Map<Class<?>, List<Object>> entitiesByType = entities.groupBy { it.class }
        int deletedCount = 0

        entitiesByType.each { entityClass, entityList ->
            SolrDocumentConverter converter = getConverter(entityClass)
            if (!converter) {
                log.warn("No SolrDocumentConverter found for entity type: ${entityClass.simpleName}")
                return
            }

            try {
                List<String> ids = entityList.collect { entity ->
                    converter.getSolrId(entity)
                }

                solrClient.deleteById(solrProperties.collection, ids)
                solrClient.commit(solrProperties.collection)
                deletedCount += entityList.size()
                log.debug("Deleted ${entityList.size()} ${entityClass.simpleName} entities from Solr")
            } catch (IOException | SolrServerException e) {
                log.error("Error batch deleting entities from Solr: ${e.message}", e)
            }
        }

        return deletedCount
    }

    /**
     * Delete all documents of a specific type from Solr
     * @param entityClass The entity class
     * @return true if successful, false otherwise
     */
    boolean deleteAllByType(Class<?> entityClass) {
        SolrDocumentConverter converter = getConverter(entityClass)
        if (!converter) {
            log.warn("No SolrDocumentConverter found for entity type: ${entityClass.simpleName}")
            return false
        }

        try {
            // Get the type_s field value from a dummy entity
            Object dummyEntity = entityClass.newInstance()
            SolrInputDocument dummyDoc = converter.toSolrDocument(dummyEntity)
            String typeValue = dummyDoc.getFieldValue("type_s")

            // Delete by query
            UpdateResponse updateResponse = solrClient.deleteByQuery(solrProperties.collection, "type_s:${typeValue}")
            solrClient.commit(solrProperties.collection)
            log.warn("DELETED all ${entityClass.simpleName} entities from Solr")
            return true

        } catch (IOException | SolrServerException | InstantiationException | IllegalAccessException e) {
            log.error("Error deleting all entities of type ${entityClass.simpleName} from Solr: ${e.message}", e)
            return false
        }
    }

    /**
     * Get the converter for a specific entity type
     * @param entityClass The entity class
     * @return The converter or null if not found
     */
    private <T> SolrDocumentConverter<T> getConverter(Class<T> entityClass) {
        return (SolrDocumentConverter<T>) converters[entityClass]
    }
}
