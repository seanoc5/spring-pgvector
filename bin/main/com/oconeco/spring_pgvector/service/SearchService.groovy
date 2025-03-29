package com.oconeco.spring_pgvector.service

import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.repository.NaicsCodeRepository
import groovy.util.logging.Slf4j
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocument
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

import java.util.regex.Pattern

/**
 * Unified search service that provides access to all search implementations:
 * - Solr search
 * - PostgreSQL full-text search
 * - Spring-AI vector store search
 */
@Service
@Slf4j
class SearchService {

    @Autowired
    private NaicsCodeRepository naicsCodeRepository

    @Autowired
    private EmbeddingModel embeddingModel

    @Autowired
    private VectorStore vectorStore

    @Autowired
    private SolrClient solrClient

    @Value('${solr.collection}')
    private String solrCollection

    /**
     * Search using PostgreSQL full-text search
     */
    Page<Map<String, Object>> searchWithPostgres(String query, Pageable pageable) {
        if (!query || query.trim().isEmpty()) {
            return emptySearchResults(pageable)
        }

        // Normalize the query
        String normalizedQuery = query.trim().replaceAll(/\s+/, " ")
        log.info("PostgreSQL search query: '${normalizedQuery}'")

        // First attempt: Use the query directly with plainto_tsquery (handles natural language)
        String plainTsQuery = normalizedQuery
        long totalCount = naicsCodeRepository.countByFullTextSearch(plainTsQuery)

        // If no results, try with formatted tsquery
        if (totalCount == 0) {
            String[] terms = normalizedQuery.split(" ")
            String formattedTsQuery

            if (terms.length > 1) {
                formattedTsQuery = terms.collect { word -> word + ":*" }.join(" | ")
                log.info("PostgreSQL: No results with plain query, trying OR-based query: '${formattedTsQuery}'")
            } else {
                formattedTsQuery = normalizedQuery + ":*"
                log.info("PostgreSQL: No results with plain query, trying prefix matching: '${formattedTsQuery}'")
            }

            totalCount = naicsCodeRepository.countByFullTextSearch(formattedTsQuery)

            // If still no results, try with stemming by removing the :* suffix
            if (totalCount == 0 && terms.length > 1) {
                formattedTsQuery = terms.join(" | ")
                log.info("PostgreSQL: No results with prefix matching, trying with stemming: '${formattedTsQuery}'")
                totalCount = naicsCodeRepository.countByFullTextSearch(formattedTsQuery)
            }

            // If we found results with a formatted query, use that
            if (totalCount > 0) {
                plainTsQuery = formattedTsQuery
            }
        }

        // If still no results, try simple LIKE search as fallback
        if (totalCount == 0) {
            log.info("PostgreSQL: No results with full-text search, falling back to LIKE search")
            return searchByLike(normalizedQuery, pageable)
        }

        // Get the paginated results
        Page<Object[]> searchResults = naicsCodeRepository.searchByFullText(plainTsQuery, pageable)

        // Transform the results into a list of maps
        List<Map<String, Object>> transformedResults = searchResults.getContent().collect { Object[] row ->
            def naicsCode
            if (row[0] instanceof NaicsCode) {
                naicsCode = row[0]
            } else if (row[0] instanceof String) {
                naicsCode = naicsCodeRepository.findById(row[0].toString()).orElse(null)
            } else {
                log.warn "PostgreSQL: Unexpected type for row[0]: ${row[0]?.class?.name}"
                naicsCode = null
            }

            Float rank = row.length > 1 ? (row[1] as Float) : 0.0f
            String highlightedTitle = row.length > 2 ? (row[2] as String) : ""
            String highlightedDescription = row.length > 3 ? (row[3] as String) : ""

            return [
                    naicsCode             : naicsCode,
                    rank                  : rank,
                    highlightedTitle      : highlightedTitle,
                    highlightedDescription: highlightedDescription,
                    searchType            : "postgres"
            ]
        }

        return new PageImpl<>(transformedResults, pageable, totalCount)
    }

    /**
     * Search using Solr
     */
    Page<Map<String, Object>> searchWithSolr(String query, Pageable pageable) {
        if (!query || query.trim().isEmpty()) {
            return emptySearchResults(pageable)
        }

        String normalizedQuery = query.trim().replaceAll(/\s+/, " ")
        log.info("Solr search query: '${normalizedQuery}'")

        try {
            // Create Solr query
            SolrQuery solrQuery = new SolrQuery()
            solrQuery.setQuery(normalizedQuery)
            
            // Add highlighting
            solrQuery.setHighlight(true)
            solrQuery.addHighlightField("title")
            solrQuery.addHighlightField("description")
            solrQuery.setHighlightSimplePre("<b>")
            solrQuery.setHighlightSimplePost("</b>")
            
            // Add pagination
            solrQuery.setStart(pageable.getPageNumber() * pageable.getPageSize())
            solrQuery.setRows(pageable.getPageSize())
            
            // Execute query
            QueryResponse response = solrClient.query(solrCollection, solrQuery)
            
            // Get total count
            long totalCount = response.getResults().getNumFound()
            
            // Transform results
            List<Map<String, Object>> transformedResults = response.getResults().collect { SolrDocument doc ->
                String id = doc.getFieldValue("id") as String
                NaicsCode naicsCode = naicsCodeRepository.findById(id).orElse(null)
                
                // Get highlighting if available
                Map<String, List<String>> highlighting = response.getHighlighting()?.get(id)
                String highlightedTitle = highlighting?.get("title")?.join("... ") ?: naicsCode?.title
                String highlightedDescription = highlighting?.get("description")?.join("... ") ?: naicsCode?.description
                
                return [
                    naicsCode             : naicsCode,
                    rank                  : doc.getFieldValue("score") as Float,
                    highlightedTitle      : highlightedTitle,
                    highlightedDescription: highlightedDescription,
                    searchType            : "solr"
                ]
            }
            
            return new PageImpl<>(transformedResults, pageable, totalCount)
        } catch (Exception e) {
            log.error("Error searching with Solr: ${e.message}", e)
            return emptySearchResults(pageable)
        }
    }

    /**
     * Search using Spring-AI vector store
     */
    Page<Map<String, Object>> searchWithVectorStore(String query, Pageable pageable) {
        if (!query || query.trim().isEmpty()) {
            return emptySearchResults(pageable)
        }

        String normalizedQuery = query.trim().replaceAll(/\s+/, " ")
        log.info("Vector search query: '${normalizedQuery}'")

        try {
            // Generate embedding for the query
            float[] queryEmbedding = embeddingModel.embed(normalizedQuery)
            
            // Search for similar documents in vector store
            List<Document> similarDocuments = vectorStore.similaritySearch(normalizedQuery, pageable.getPageSize())
            
            // Transform results
            List<Map<String, Object>> transformedResults = similarDocuments.collect { Document doc ->
                // Extract NAICS code ID from document metadata
                String naicsCodeId = doc.getMetadata().get("naicsCodeId") as String
                NaicsCode naicsCode = naicsCodeRepository.findById(naicsCodeId).orElse(null)
                
                // Calculate similarity score (normalized between 0 and 1)
                float score = doc.getMetadata().get("score") != null ? 
                    doc.getMetadata().get("score") as float : 0.5f
                
                // Create simple highlighting based on query terms
                String highlightedTitle = naicsCode?.title
                String highlightedDescription = naicsCode?.description
                
                if (naicsCode) {
                    // Simple highlighting for vector search results
                    String[] queryTerms = normalizedQuery.split(/\s+/)
                    for (String term : queryTerms) {
                        if (term.length() > 2) { // Only highlight meaningful terms
                            String pattern = /(?i)(${Pattern.quote(term)})/
                            highlightedTitle = highlightedTitle?.replaceAll(pattern, '<b>$1</b>')
                            highlightedDescription = highlightedDescription?.replaceAll(pattern, '<b>$1</b>')
                        }
                    }
                }
                
                return [
                    naicsCode             : naicsCode,
                    rank                  : score,
                    highlightedTitle      : highlightedTitle,
                    highlightedDescription: highlightedDescription,
                    searchType            : "vector",
                    content               : doc.getText() // Include the actual document content
                ]
            }
            
            // Filter out null NAICS codes (in case some documents don't have valid references)
            transformedResults = transformedResults.findAll { it.naicsCode != null }
            
            return new PageImpl<>(transformedResults, pageable, transformedResults.size())
        } catch (Exception e) {
            log.error("Error searching with vector store: ${e.message}", e)
            return emptySearchResults(pageable)
        }
    }

    /**
     * Search using all available methods and combine results
     */
    Map<String, Page<Map<String, Object>>> searchWithAllMethods(String query, Pageable pageable) {
        Map<String, Page<Map<String, Object>>> results = [:]
        
        results.postgres = searchWithPostgres(query, pageable)
        results.solr = searchWithSolr(query, pageable)
        results.vector = searchWithVectorStore(query, pageable)
        
        return results
    }

    /**
     * Fallback search using LIKE operator when full-text search returns no results.
     */
    private Page<Map<String, Object>> searchByLike(String query, Pageable pageable) {
        // Search in title and description using LIKE
        Page<NaicsCode> results = naicsCodeRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable)

        // Transform to the same format as full-text search results
        List<Map<String, Object>> transformedResults = results.content.collect { NaicsCode naicsCode ->
            // Highlight the matching parts (simple implementation)
            String highlightedTitle = naicsCode.title?.replaceAll(
                    /(?i)(${Pattern.quote(query)})/,
                    '<b>$1</b>'
            )

            String highlightedDescription = naicsCode.description?.replaceAll(
                    /(?i)(${Pattern.quote(query)})/,
                    '<b>$1</b>'
            )

            return [
                    naicsCode             : naicsCode,
                    rank                  : 0.5f, // Default rank for LIKE results
                    highlightedTitle      : highlightedTitle ?: naicsCode.title,
                    highlightedDescription: highlightedDescription ?: naicsCode.description,
                    searchType            : "postgres-like"
            ]
        }

        return new PageImpl<>(transformedResults, pageable, results.totalElements)
    }
    
    /**
     * Returns empty search results
     */
    private Page<Map<String, Object>> emptySearchResults(Pageable pageable) {
        return new PageImpl<>([], pageable, 0)
    }
}
