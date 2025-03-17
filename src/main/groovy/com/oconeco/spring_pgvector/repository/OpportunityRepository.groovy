package com.oconeco.spring_pgvector.repository

import com.oconeco.spring_pgvector.domain.Opportunity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing Opportunity entities.
 */
@Repository
interface OpportunityRepository extends JpaRepository<Opportunity, String> {

    /**
     * Find opportunities by their active/inactive status.
     */
    Page<Opportunity> findByActiveInactive(String status, Pageable pageable)

    /**
     * Find opportunities by NAICS code.
     */
    Page<Opportunity> findByNaicsContaining(String naics, Pageable pageable)

    /**
     * Find opportunities by contract opportunity type.
     */
    Page<Opportunity> findByContractOpportunityType(String type, Pageable pageable)

    /**
     * Full-text search using PostgreSQL's ts_vector.
     * This query uses the search_vector column that is automatically updated by a database trigger.
     */
    @Query(value = """
        SELECT o.*, 
               ts_rank(o.search_vector, to_tsquery('english', :tsQuery)) AS rank,
               ts_headline('english', o.title, to_tsquery('english', :tsQuery), 'StartSel=<b>, StopSel=</b>, MaxWords=50, MinWords=10, MaxFragments=3') AS highlighted_title,
               ts_headline('english', o.description, to_tsquery('english', :tsQuery), 'StartSel=<b>, StopSel=</b>, MaxWords=100, MinWords=20, MaxFragments=3') AS highlighted_description
        FROM opportunities o
        WHERE o.search_vector @@ to_tsquery('english', :tsQuery)
        ORDER BY rank DESC
    """, nativeQuery = true)
    Page<Object[]> searchByFullText(@Param("tsQuery") String tsQuery, Pageable pageable)

    /**
     * Count the total number of opportunities matching a search query.
     */
    @Query(value = "SELECT COUNT(*) FROM opportunities WHERE search_vector @@ to_tsquery('english', :tsQuery)",
           nativeQuery = true)
    long countByFullTextSearch(@Param("tsQuery") String tsQuery)
}
