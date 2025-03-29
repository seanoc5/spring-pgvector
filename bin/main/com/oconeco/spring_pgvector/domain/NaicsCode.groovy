package com.oconeco.spring_pgvector.domain

import com.oconeco.spring_pgvector.solr.SolrEntityListener
import jakarta.persistence.*
import org.hibernate.annotations.Array
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import groovy.transform.ToString
import org.hibernate.type.SqlTypes


/**
 * Entity representing a US Census NAICS (North American Industry Classification System) code.
 */
@Entity
@Table(name = "naics_codes")
@EntityListeners(SolrEntityListener.class)
@ToString(includeNames = true, includePackage = false)
class NaicsCode {

    @Id
    @Column(name = "code")
    String code

    @Column(name = "level")
    Integer level

    @Column(nullable = false)
    String title

    @Column(columnDefinition = "TEXT")
    String description

    @Column(name = "sector_code")
    String sectorCode

    @Column(name = "sector_title")
    String sectorTitle

    @Column(name = "subsector_code")
    String subsectorCode

    @Column(name = "subsector_title")
    String subsectorTitle

    @Column(name = "industry_group_code")
    String industryGroupCode

    @Column(name = "industry_group_title")
    String industryGroupTitle

    @Column(name = "naics_industry_code")
    String naicsIndustryCode

    @Column(name = "naics_industry_title")
    String naicsIndustryTitle

    @Column(name = "national_industry_code")
    String nationalIndustryCode

    @Column(name = "national_industry_title")
    String nationalIndustryTitle

    @Column(name = "year_introduced")
    Integer yearIntroduced

    @Column(name = "year_updated")
    Integer yearUpdated

    @Column(name = "is_active")
    Boolean isActive = true

    /**
     * PostgreSQL tsvector for full-text search.
     * This field is managed by a database trigger and should not be modified directly.
     */
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector

    /**
     * Vector embedding for semantic search using pgvector.
     * This field stores the embedding vector generated from the title and description.
     */
//    @Column(name = "embedding_vector", columnDefinition = "vector(768)")
//    @Convert(converter = PgVectorConverter.class)
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    private float[] embeddingVector

    @CreationTimestamp
    @Column(name = "created_at")
    Date createdAt

    @UpdateTimestamp
    @Column(name = "updated_at")
    Date updatedAt

    @Override
    String toString() {
        return "$code) $title"
    }
}
