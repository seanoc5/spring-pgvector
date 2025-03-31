package com.oconeco.spring_pgvector.solr

import com.oconeco.spring_pgvector.domain.NaicsCode
import org.apache.solr.common.SolrInputDocument
import org.springframework.stereotype.Component

/**
 * Converts NaicsCode domain objects to Solr documents
 */
@Component
class NaicsCodeSolrConverter implements SolrDocumentConverter<NaicsCode> {

    @Override
    SolrInputDocument toSolrDocument(NaicsCode naicsCode) {
        SolrInputDocument doc = new SolrInputDocument()

        // Required fields
        doc.addField("id", getSolrId(naicsCode))
        doc.addField("type_s", "naics_code")

        // Core fields
        doc.addField("code", naicsCode.code)
        doc.addField("level", naicsCode.level)
        doc.addField("title", naicsCode.title)
        doc.addField("description", naicsCode.description)

        // Hierarchy fields
        doc.addField("sector_code", naicsCode.sectorCode)
        doc.addField("sector_title", naicsCode.sectorTitle)
        doc.addField("subsector_code", naicsCode.subsectorCode)
        doc.addField("subsector_title", naicsCode.subsectorTitle)
        doc.addField("industry_group_code", naicsCode.industryGroupCode)
        doc.addField("industry_group_title", naicsCode.industryGroupTitle)
        doc.addField("naics_industry_code", naicsCode.naicsIndustryCode)
        doc.addField("naics_industry_title", naicsCode.naicsIndustryTitle)
        doc.addField("national_industry_code", naicsCode.nationalIndustryCode)
        doc.addField("national_industry_title", naicsCode.nationalIndustryTitle)

        // Metadata fields
        doc.addField("year_introduced_i", naicsCode.yearIntroduced)
        doc.addField("year_updated_i", naicsCode.yearUpdated)
        doc.addField("is_active_b", naicsCode.isActive)

        // Dates
        doc.addField("created_dt", naicsCode.createdAt)
        doc.addField("updated_dt", naicsCode.updatedAt)

        return doc
    }

    @Override
    String getSolrId(NaicsCode naicsCode) {
        return "naics_${naicsCode.code}"
    }

    @Override
    Class<NaicsCode> getEntityType() {
        return NaicsCode.class
    }
}
