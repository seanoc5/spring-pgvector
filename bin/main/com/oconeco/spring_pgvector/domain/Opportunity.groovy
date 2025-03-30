package com.oconeco.spring_pgvector.domain

import com.oconeco.spring_pgvector.solr.SolrEntityListener
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import groovy.transform.ToString

/**
 * Entity representing a SAM.gov contract opportunity.
 */
@Entity
@Table(name = "opportunities")
@EntityListeners(SolrEntityListener.class)
@ToString(includeNames = true, includePackage = false)
class Opportunity {

    @Id
    @Column(name = "notice_id")
    String noticeId

    @Column(columnDefinition = "TEXT")
    String title

    @Column(columnDefinition = "TEXT")
    String description

    @Column(name = "current_response_date")
    Date currentResponseDate

    @Column(name = "last_modified_date")
    Date lastModifiedDate

    @Column(name = "last_published_date")
    Date lastPublishedDate

    @Column(name = "contract_opportunity_type")
    String contractOpportunityType

    @Column(name = "poc_information", columnDefinition = "TEXT")
    String pocInformation

    @Column(name = "active_inactive")
    String activeInactive

    @Column(columnDefinition = "TEXT")
    String awardee

    @Column(name = "contract_award_number")
    String contractAwardNumber

    @Column(name = "contract_award_date")
    Date contractAwardDate

    @Column
    String naics

    @Column(name = "naics_label")
    String naicsLabel

    @Column
    String psc

    @Column(name = "modification_number")
    String modificationNumber

    @Column(name = "set_aside")
    String setAside

    @CreationTimestamp
    @Column(name = "created_at")
    Date createdAt

    @UpdateTimestamp
    @Column(name = "updated_at")
    Date updatedAt

    // Vector embedding and search vector are managed at the database level
    // We don't need to map them directly in the entity
}
