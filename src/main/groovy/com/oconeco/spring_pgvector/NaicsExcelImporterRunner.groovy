package com.oconeco.spring_pgvector

import com.oconeco.spring_pgvector.service.NaicsCodeService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * Command-line runner for importing NAICS codes from an Excel file.
 * This runner is only activated when the 'naics-excel-import' profile is active.
 */
@Component
@Profile("naics-excel-import")
@Slf4j
class NaicsExcelImporterRunner implements ApplicationRunner {

    @Autowired
    private NaicsCodeService naicsCodeService

    @Override
    void run(ApplicationArguments args) throws Exception {
        log.info "Starting NAICS Excel importer..."

        // Check for Excel file path
        if (args.containsOption("file")) {
            String excelFilePath = args.getOptionValues("file").get(0)
            File excelFile = new File(excelFilePath)

            if (excelFile.exists()) {
                log.info "Importing data from Excel file: ${excelFilePath}"
                int count = naicsCodeService.importFromExcel(excelFile)
                log.info "Import completed successfully. Imported ${count} NAICS codes."
            } else {
                log.error "Excel file not found: ${excelFilePath}"
            }
        } else {
            log.info "No Excel file specified. Use --file=<path> to specify an Excel file to import."
            printUsage()
        }
    }

    /**
     * Prints usage information.
     */
    private void printUsage() {
        log.info """
        Usage:
          ./gradlew bootRun --args='--spring.profiles.active=naics-excel-import --file=/path/to/naics_codes.xlsx'
        
        The Excel file should have the following columns in the first worksheet:
          - Seq. No.
          - 2022 NAICS US Code
          - 2022 NAICS US Title
          - Description
        """
    }
}
