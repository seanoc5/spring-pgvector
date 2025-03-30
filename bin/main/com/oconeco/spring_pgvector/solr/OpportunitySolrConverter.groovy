package com.oconeco.spring_pgvector.solr

import com.oconeco.spring_pgvector.domain.Opportunity
import org.apache.solr.common.SolrInputDocument
import org.springframework.stereotype.Component

/**
 * Converts Opportunity domain objects to Solr documents
 */
@Component
class OpportunitySolrConverter implements SolrDocumentConverter<Opportunity> {

    @Override
    SolrInputDocument toSolrDocument(Opportunity opportunity) {
        SolrInputDocument doc = new SolrInputDocument()

        // Required fields
        doc.addField("id", getSolrId(opportunity))
        doc.addField("type_s", "opportunity")

        // Core fields
        doc.addField("notice_id_s", opportunity.noticeId)
        doc.addField("title", opportunity.title)
//        doc.addField("title_exact_s", opportunity.title)
        doc.addField("description", opportunity.description)

        // Date fields
        doc.addField("current_response_date_dt", opportunity.currentResponseDate)
        doc.addField("last_modified_date_dt", opportunity.lastModifiedDate)
        doc.addField("last_published_date_dt", opportunity.lastPublishedDate)
        doc.addField("contract_award_date_dt", opportunity.contractAwardDate)

        // Classification fields
        doc.addField("contract_opportunity_type_s", opportunity.contractOpportunityType)
        doc.addField("naics", opportunity.naics)
        doc.addField("psc", opportunity.psc)

        // Additional fields
        doc.addField("poc_information_t", opportunity.pocInformation)
        doc.addField("active_inactive_s", opportunity.activeInactive)
        doc.addField("awardee_t", opportunity.awardee)
        doc.addField("contract_award_number_s", opportunity.contractAwardNumber)
        doc.addField("modification_number_s", opportunity.modificationNumber)
        doc.addField("set_aside", opportunity.setAside)

        // Metadata fields
        doc.addField("created_dt", opportunity.createdAt)
        doc.addField("updated_dt", opportunity.updatedAt)

        // Combined text field for general search
        String allText = [
            opportunity.title,
            opportunity.description,
            opportunity.pocInformation,
            opportunity.awardee
        ].findAll { it }.join(" ")

        doc.addField("text", allText)

        return doc
    }

    @Override
    String getSolrId(Opportunity opportunity) {
        return "opportunity_${opportunity.noticeId}"
    }

    @Override
    Class<Opportunity> getEntityType() {
        return Opportunity.class
    }
}
