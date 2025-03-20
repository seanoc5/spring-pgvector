package com.oconeco.spring_pgvector.domain

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import groovy.transform.ToString

/**
 * Entity representing a US Census NAICS (North American Industry Classification System) code.
 */
@Entity
@Table(name = "naice_codes")
@ToString(includeNames = true, includePackage = false)
class NaiceCode {

    @Id
    @Column(name = "code")
    String code

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

    @CreationTimestamp
    @Column(name = "created_at")
    Date createdAt

    @UpdateTimestamp
    @Column(name = "updated_at")
    Date updatedAt
}
