package com.oconeco.spring_pgvector.solr

import org.apache.solr.common.SolrInputDocument

/**
 * Interface for converting domain objects to Solr documents
 */
interface SolrDocumentConverter<T> {
    /**
     * Convert a domain object to a SolrInputDocument
     * @param entity The domain entity to convert
     * @return A SolrInputDocument ready to be indexed in Solr
     */
    SolrInputDocument toSolrDocument(T entity)
    
    /**
     * Get the unique ID for the Solr document
     * @param entity The domain entity
     * @return The ID to use for the Solr document
     */
    String getSolrId(T entity)
    
    /**
     * Get the entity class this converter handles
     * @return The class of entities this converter handles
     */
    Class<T> getEntityType()
}
