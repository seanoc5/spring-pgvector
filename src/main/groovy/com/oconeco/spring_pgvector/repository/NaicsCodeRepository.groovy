package com.oconeco.spring_pgvector.repository

import com.oconeco.spring_pgvector.domain.NaicsCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for managing NaicsCode entities.
 */
@Repository
interface NaicsCodeRepository extends JpaRepository<NaicsCode, String> {

    /**
     * Find NAICS codes by title containing the search term.
     */
    Page<NaicsCode> findByTitleContainingIgnoreCase(String title, Pageable pageable)

    /**
     * Find NAICS codes by description containing the search term.
     */
    Page<NaicsCode> findByDescriptionContainingIgnoreCase(String description, Pageable pageable)

    /**
     * Find NAICS codes by title or description containing the search term (case insensitive).
     * This is used as a fallback when full-text search returns no results.
     */
    Page<NaicsCode> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        String title, String description, Pageable pageable)

    /**
     * Find NAICS codes by sector code.
     */
    Page<NaicsCode> findBySectorCode(String sectorCode, Pageable pageable)

    /**
     * Find NAICS codes by subsector code.
     */
    Page<NaicsCode> findBySubsectorCode(String subsectorCode, Pageable pageable)

    /**
     * Find NAICS codes by industry group code.
     */
    Page<NaicsCode> findByIndustryGroupCode(String industryGroupCode, Pageable pageable)

    /**
     * Find NAICS codes by active status.
     */
    Page<NaicsCode> findByIsActive(Boolean isActive, Pageable pageable)

    /**
     * Find NAICS codes by level.
     */
    Page<NaicsCode> findByLevel(Integer level, Pageable pageable)

    /**
     * Full-text search using PostgreSQL's ts_vector.
     * This query uses the search_vector column that will be automatically updated by a database trigger.
     */
    @Query(value = """
        SELECT n.*, 
               ts_rank(n.search_vector, to_tsquery('english', :tsQuery)) AS rank,
               ts_headline('english', n.title, to_tsquery('english', :tsQuery), 'StartSel=<b>, StopSel=</b>, MaxWords=50, MinWords=10, MaxFragments=3') AS highlighted_title,
               ts_headline('english', n.description, to_tsquery('english', :tsQuery), 'StartSel=<b>, StopSel=</b>, MaxWords=100, MinWords=20, MaxFragments=3') AS highlighted_description
        FROM naics_codes n
        WHERE n.search_vector @@ plainto_tsquery('english', :tsQuery)
           OR n.search_vector @@ to_tsquery('english', :tsQuery)
        ORDER BY rank DESC
    """, nativeQuery = true)
    Page<Object[]> searchByFullText(@Param("tsQuery") String tsQuery, Pageable pageable)

    /**
     * Count the total number of NAICS codes matching a search query.
     */
    @Query(value = """
        SELECT COUNT(*) 
        FROM naics_codes n
        WHERE n.search_vector @@ plainto_tsquery('english', :tsQuery)
           OR n.search_vector @@ to_tsquery('english', :tsQuery)
    """, nativeQuery = true)
    long countByFullTextSearch(@Param("tsQuery") String tsQuery)
}
