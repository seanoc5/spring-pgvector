package com.oconeco.spring_pgvector.service

import com.oconeco.spring_pgvector.domain.Opportunity
import com.oconeco.spring_pgvector.repository.OpportunityRepository
import com.oconeco.spring_pgvector.repository.NaicsCodeRepository
import com.oconeco.spring_pgvector.solr.SolrSyncService
import groovy.util.logging.Slf4j
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.text.SimpleDateFormat

/**
 * Service for managing SAM.gov contract opportunities.
 */
@Service
@Slf4j
class OpportunityService {

    @Autowired
    private OpportunityRepository opportunityRepository

    @Autowired
    private NaicsCodeRepository naicsCodeRepository

    @Autowired
    private SolrSyncService solrSyncService

    // Date format for parsing dates from the CSV
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy hh:mm a z")

    /**
     * Import opportunities from a CSV file.
     *
     * @param csvFile The CSV file to import
     * @return The number of records imported
     */
    @Transactional
    int importFromCsv(File csvFile) {
        log.debug "Importing data from CSV file: ${csvFile.absolutePath}"

        if (!csvFile.exists()) {
            throw new FileNotFoundException("CSV file not found: ${csvFile.absolutePath}")
        }

        // Parse the CSV file
        BufferedReader csvReader = csvFile.newReader('UTF-8')

        // todo - replace deprecated `build()` below
        Iterable<CSVRecord> records = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(csvReader)

        int count = 0
        List<Opportunity> batchOpportunities = []
        int batchSize = 100

        // Process each record
        records.each { CSVRecord record ->
            try {
                Opportunity opportunity = new Opportunity(
                    noticeId: record.get("Notice ID"),
                    title: record.get("Title"),
                    description: record.get("Description"),
                    currentResponseDate: parseDate(record.get("Current Response Date")),
                    lastModifiedDate: parseDate(record.get("Last Modified Date")),
                    lastPublishedDate: parseDate(record.get("Last Published Date")),
                    contractOpportunityType: record.get("Contract Opportunity Type"),
                    pocInformation: record.get("POC Information"),
                    activeInactive: record.get("Active/Inactive"),
                    awardee: record.get("Awardee"),
                    contractAwardNumber: record.get("Contract Award Number"),
                    contractAwardDate: parseDate(record.get("Contract Award Date")),
                    naics: record.get("NAICS"),
                    naicsLabel: record.get("NAICS Label") ?: lookupNaicsLabel(record.get("NAICS")),
                    psc: record.get("PSC"),
                    modificationNumber: record.get("Modification Number"),
                    setAside: record.get("Set Aside")
                )

                batchOpportunities << opportunity
                count++

                // Process in batches for better performance
                if (batchOpportunities.size() >= batchSize) {
                    saveAndSyncBatch(batchOpportunities)
                    log.info "Processed ${count} records"
                    batchOpportunities = []
                }
            } catch (Exception e) {
                log.error "Error processing record: ${e.message}", e
            }
        }

        // Save any remaining opportunities
        if (batchOpportunities) {
            saveAndSyncBatch(batchOpportunities)
        }

        log.info "Imported ${count} records from CSV file"
        return count
    }

    /**
     * Save a batch of opportunities and sync them to Solr
     */
    private void saveAndSyncBatch(List<Opportunity> opportunities) {
        if (!opportunities){
            log.warn "No opportunities to save, cowardly returning without doing anything useful"
            return
        }

        // Save to database
        List<Opportunity> savedOpportunities = opportunities.collect { opportunity ->
            def rc = opportunityRepository.save(opportunity)
            log.debug("Save result: ${rc}")
            return rc
        }

        // Sync to Solr
        def rc = solrSyncService.saveAll(savedOpportunities)
        log.info("results of solrSyncService.saveAll: ${rc}")
    }

    /**
     * Search opportunities using full-text search.
     *
     * @param query The search query
     * @param pageable Pagination information
     * @return A page of search results
     */
    Page<Map<String, Object>> searchOpportunities(String query, Pageable pageable) {
        if (!query || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty")
        }

        // Convert the query to a tsquery expression
        String tsQuery = query.trim()
                .replaceAll(/\s+/, " ")  // Normalize whitespace
                .split(" ")
                .collect { word -> word + ":*" }  // Add prefix matching
                .join(" & ")             // AND operator between words

        log.info("tsQuery: ${tsQuery}")

        // Get the total count
        long totalCount = opportunityRepository.countByFullTextSearch(tsQuery)

        // Get the paginated results
        Page<Object[]> searchResults = opportunityRepository.searchByFullText(tsQuery, pageable)

        // Transform the results into a list of maps
        List<Map<String, Object>> transformedResults = searchResults.getContent().collect { Object[] row ->
            // The first element might be the ID string instead of the Opportunity object
            // Handle both cases appropriately
            def opportunity
            if (row[0] instanceof Opportunity) {
                opportunity = row[0]
            } else if (row[0] instanceof String) {
                // If it's just the ID, fetch the full opportunity
                opportunity = opportunityRepository.findById(row[0].toString()).orElse(null)
            } else {
                log.warn "Unexpected type for row[0]: ${row[0]?.class?.name}"
                opportunity = null
            }

            Float rank = row.length > 1 ? (row[20] as Float) : 0.0f
            String highlightedTitle = row.length > 2 ? (row[21] as String) : ""
            String highlightedDescription = row.length > 3 ? (row[22] as String) : ""

            return [
                opportunity: opportunity,
                rank: rank,
                highlightedTitle: highlightedTitle,
                highlightedDescription: highlightedDescription
            ]
        }

        return new PageImpl<>(transformedResults, pageable, totalCount)
    }

    /**
     * Get all opportunities with pagination.
     */
    Page<Opportunity> getAllOpportunities(Pageable pageable) {
        return opportunityRepository.findAll(pageable)
    }

    /**
     * Get an opportunity by its notice ID.
     */
    Opportunity getOpportunityByNoticeId(String noticeId) {
        return opportunityRepository.findById(noticeId).orElse(null)
    }

    /**
     * Create a new opportunity.
     */
    @Transactional
    Opportunity createOpportunity(Opportunity opportunity) {
        if (!opportunity.noticeId) {
            throw new IllegalArgumentException("Notice ID cannot be empty")
        }

        if (opportunityRepository.existsById(opportunity.noticeId)) {
            throw new IllegalArgumentException("Opportunity with notice ID ${opportunity.noticeId} already exists")
        }

        // Save to database
        Opportunity savedOpportunity = opportunityRepository.save(opportunity)

        // Explicitly sync with Solr
        solrSyncService.save(savedOpportunity)

        return savedOpportunity
    }

    /**
     * Update an existing opportunity.
     */
    @Transactional
    Opportunity updateOpportunity(String noticeId, Opportunity opportunity) {
        if (!opportunityRepository.existsById(noticeId)) {
            throw new IllegalArgumentException("Opportunity with notice ID ${noticeId} not found")
        }

        opportunity.noticeId = noticeId  // Ensure the notice ID is not changed

        // Save to database
        Opportunity updatedOpportunity = opportunityRepository.save(opportunity)

        // Explicitly sync with Solr
        solrSyncService.save(updatedOpportunity)

        return updatedOpportunity
    }

    /**
     * Delete an opportunity.
     */
    @Transactional
    void deleteOpportunity(String noticeId) {
        if (!opportunityRepository.existsById(noticeId)) {
            throw new IllegalArgumentException("Opportunity with notice ID ${noticeId} not found")
        }

        // Get the entity before deleting it
        Opportunity opportunity = opportunityRepository.findById(noticeId).orElse(null)

        // Delete from database
        opportunityRepository.deleteById(noticeId)

        // Explicitly delete from Solr
        if (opportunity) {
            solrSyncService.delete(opportunity)
        }
    }

    /**
     * Reindex all opportunities in Solr.
     * This is useful for initial setup or when Solr schema changes.
     * @return The number of records reindexed
     */
    @Transactional(readOnly = true)
    int reindexAllToSolr() {
        log.info("Reindexing all opportunities to Solr")

        // First, delete all existing opportunity documents from Solr
        solrSyncService.deleteAllByType(Opportunity.class)

        // Batch process to avoid memory issues with large datasets
        int batchSize = 100
        int totalProcessed = 0
        boolean hasMore = true
        int page = 0

        while (hasMore) {
            Page<Opportunity> batch = opportunityRepository.findAll(Pageable.ofSize(batchSize).withPage(page))
            if (batch.hasContent()) {
                int batchProcessed = solrSyncService.saveAll(batch.content)
                totalProcessed += batchProcessed
                log.info("Reindexed batch ${page + 1} to Solr: ${batchProcessed} records")
            }

            page++
            hasMore = batch.hasNext()
        }

        log.info("Completed reindexing ${totalProcessed} opportunities to Solr")
        return totalProcessed
    }

    /**
     * Ensures the database schema is properly set up for opportunities.
     * This method is called during application startup to ensure the table and required indexes exist.
     */
    @Transactional
    void ensureSchemaSetup() {
        log.info "Ensuring database schema is properly set up for opportunities..."

        // This is now handled by Spring Data JPA and Hibernate
        // Additional custom schema setup can be added here if needed
    }

    /**
     * Parses a date string into a Date object.
     * Returns null if the date string is empty or invalid.
     */
    private Date parseDate(String dateStr) {
        if (!dateStr || dateStr.trim() == "N/A") {
            return null
        }

        try {
            // Handle different date formats
            if (dateStr.contains("CET") || dateStr.contains("EDT") || dateStr.contains("EST")) {
                // Format like "Mar 18, 2025 12:00 PM CET"
                return DATE_FORMAT.parse(dateStr)
            } else if (dateStr.contains(",")) {
                // Format like "Mar 27, 2025" without time
                SimpleDateFormat shortFormat = new SimpleDateFormat("MMM dd, yyyy")
                return shortFormat.parse(dateStr)
            } else if (dateStr.contains("-") && dateStr.length() >= 10) {
                // Try to parse as ISO format
                return new Date(java.sql.Timestamp.valueOf(dateStr.replace('T', ' ')
                        .substring(0, Math.min(19, dateStr.length()))).getTime())
            } else {
                log.warn "Unrecognized date format: ${dateStr}"
                return null
            }
        } catch (Exception e) {
            log.warn "Could not parse date: ${dateStr} - ${e.message}"
            return null
        }
    }

    /**
     * Look up NAICS label from the code
     * @param naicsCode The NAICS code to look up
     * @return The NAICS label if found, or null if not found
     */
    private String lookupNaicsLabel(String naicsCode) {
        if (!naicsCode) {
            return null
        }
        
        try {
            // Try to find the NAICS code in the database
            def naicsCodeEntity = naicsCodeRepository.findById(naicsCode).orElse(null)
            return naicsCodeEntity?.title
        } catch (Exception e) {
            log.warn("Error looking up NAICS label for code ${naicsCode}: ${e.message}")
            return null
        }
    }
}
