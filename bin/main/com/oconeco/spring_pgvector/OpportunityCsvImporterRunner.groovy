package com.oconeco.spring_pgvector

import com.oconeco.spring_pgvector.service.OpportunityService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component


/**
 * Command-line runner for importing SAM.gov contract opportunities from a CSV file.
 * This runner is only activated when the 'csv-import' profile is active.
 */
@Component
@Profile("csv-import")
@Slf4j
class OpportunityCsvImporterRunner implements ApplicationRunner {

    @Autowired
    private OpportunityService opportunityService

    @Override
    void run(ApplicationArguments args) throws Exception {
        log.info "Starting SAM.gov CSV importer..."

        // Check if we're in search mode
        if (args.containsOption("search")) {
            String query = args.getOptionValues("search").get(0) ?: "software development"
            log.info "Search mode activated with query: '${query}'"
            demonstrateSearch(query)
            return
        }

        // Check for CSV file path
        if (args.containsOption("file")) {
            String csvFilePath = args.getOptionValues("file").get(0)
            File csvFile = new File(csvFilePath)

            if (csvFile.exists()) {
                log.info "Importing data from CSV file: ${csvFilePath}"
                int count = opportunityService.importFromCsv(csvFile)
                log.info "Import completed successfully. Imported ${count} records."
            } else {
                log.error "CSV file not found: ${csvFilePath}"
            }
        } else {
            log.info "No CSV file specified. Use --file=<path> to specify a CSV file to import."
            printUsage()
        }
        return
    }

    /**
     * Demonstrates the search functionality.
     */
    private void demonstrateSearch(String query) {
        try {
            // Use a small page size for demonstration
            def pageRequest = org.springframework.data.domain.PageRequest.of(0, 10)
            def results = opportunityService.searchOpportunities(query, pageRequest)

            log.info "Found ${results.totalElements} total matches for '${query}'"
            log.info "Showing page 1 (${results.numberOfElements} of ${results.totalElements} results):"

            results.content.eachWithIndex { result, index ->
                def opportunity = result.opportunity
                log.info "Result ${index + 1}:"
                log.info "  Notice ID: ${opportunity.noticeId}"
                log.info "  Title: ${opportunity.title}"
                log.info "  Rank: ${result.rank}"
                log.info "  Highlighted Title: ${result.highlightedTitle ?: 'No highlights'}"
                log.info "  Highlighted Description: ${result.highlightedDescription ?: 'No highlights'}"
                log.info "  Published: ${opportunity.lastPublishedDate}"
                log.info "  Type: ${opportunity.contractOpportunityType}"
                log.info "  Status: ${opportunity.activeInactive}"
                log.info "---"
            }

            log.info "Search demonstration completed."
        } catch (Exception e) {
            log.error "Error during search demonstration: ${e.message}", e
        }
    }

    /**
     * Prints usage information.
     */
    private void printUsage() {
        log.info """
        Usage:
          ./gradlew bootRun --args='--spring.profiles.active=csv-import --file=/path/to/file.csv'
          ./gradlew bootRun --args='--spring.profiles.active=csv-import --search="your search query"'
        """
    }
}
