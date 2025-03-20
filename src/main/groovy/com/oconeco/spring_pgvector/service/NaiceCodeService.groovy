package com.oconeco.spring_pgvector.service

import com.oconeco.spring_pgvector.domain.NaiceCode
import com.oconeco.spring_pgvector.repository.NaiceCodeRepository
import groovy.util.logging.Slf4j
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing US Census NAICS codes.
 */
@Service
@Slf4j
class NaiceCodeService {

    @Autowired
    private NaiceCodeRepository naiceCodeRepository

    /**
     * Get all NAICS codes with pagination.
     */
    Page<NaiceCode> getAllNaiceCodes(Pageable pageable) {
        return naiceCodeRepository.findAll(pageable)
    }

    /**
     * Get a NAICS code by its code.
     */
    NaiceCode getNaiceCodeByCode(String code) {
        return naiceCodeRepository.findById(code).orElse(null)
    }

    /**
     * Create a new NAICS code.
     */
    @Transactional
    NaiceCode createNaiceCode(NaiceCode naiceCode) {
        if (!naiceCode.code) {
            throw new IllegalArgumentException("NAICS code cannot be empty")
        }
        
        if (naiceCodeRepository.existsById(naiceCode.code)) {
            throw new IllegalArgumentException("NAICS code ${naiceCode.code} already exists")
        }
        
        return naiceCodeRepository.save(naiceCode)
    }

    /**
     * Update an existing NAICS code.
     */
    @Transactional
    NaiceCode updateNaiceCode(String code, NaiceCode naiceCode) {
        if (!naiceCodeRepository.existsById(code)) {
            throw new IllegalArgumentException("NAICS code ${code} not found")
        }
        
        naiceCode.code = code  // Ensure the code is not changed
        return naiceCodeRepository.save(naiceCode)
    }

    /**
     * Delete a NAICS code.
     */
    @Transactional
    void deleteNaiceCode(String code) {
        if (!naiceCodeRepository.existsById(code)) {
            throw new IllegalArgumentException("NAICS code ${code} not found")
        }
        
        naiceCodeRepository.deleteById(code)
    }

    /**
     * Search NAICS codes by title.
     */
    Page<NaiceCode> searchByTitle(String title, Pageable pageable) {
        return naiceCodeRepository.findByTitleContainingIgnoreCase(title, pageable)
    }

    /**
     * Search NAICS codes by description.
     */
    Page<NaiceCode> searchByDescription(String description, Pageable pageable) {
        return naiceCodeRepository.findByDescriptionContainingIgnoreCase(description, pageable)
    }

    /**
     * Search NAICS codes by sector code.
     */
    Page<NaiceCode> searchBySectorCode(String sectorCode, Pageable pageable) {
        return naiceCodeRepository.findBySectorCode(sectorCode, pageable)
    }

    /**
     * Search NAICS codes by active status.
     */
    Page<NaiceCode> searchByActiveStatus(Boolean isActive, Pageable pageable) {
        return naiceCodeRepository.findByIsActive(isActive, pageable)
    }

    /**
     * Search NAICS codes using full-text search.
     */
    Page<Map<String, Object>> searchNaiceCodes(String query, Pageable pageable) {
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
        long totalCount = naiceCodeRepository.countByFullTextSearch(tsQuery)

        // Get the paginated results
        Page<Object[]> searchResults = naiceCodeRepository.searchByFullText(tsQuery, pageable)

        // Transform the results into a list of maps
        List<Map<String, Object>> transformedResults = searchResults.getContent().collect { Object[] row ->
            def naiceCode
            if (row[0] instanceof NaiceCode) {
                naiceCode = row[0]
            } else if (row[0] instanceof String) {
                naiceCode = naiceCodeRepository.findById(row[0].toString()).orElse(null)
            } else {
                log.warn "Unexpected type for row[0]: ${row[0]?.class?.name}"
                naiceCode = null
            }

            Float rank = row.length > 1 ? (row[1] as Float) : 0.0f
            String highlightedTitle = row.length > 2 ? (row[2] as String) : ""
            String highlightedDescription = row.length > 3 ? (row[3] as String) : ""

            return [
                naiceCode: naiceCode,
                rank: rank,
                highlightedTitle: highlightedTitle,
                highlightedDescription: highlightedDescription
            ]
        }

        return new PageImpl<>(transformedResults, pageable, totalCount)
    }

    /**
     * Import NAICS codes from a CSV file.
     */
    @Transactional
    int importFromCsv(File csvFile) {
        log.info "Importing NAICS codes from CSV file: ${csvFile.absolutePath}"

        if (!csvFile.exists()) {
            throw new FileNotFoundException("CSV file not found: ${csvFile.absolutePath}")
        }

        // Parse the CSV file
        BufferedReader csvReader = csvFile.newReader('UTF-8')

        Iterable<CSVRecord> records = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(csvReader)

        int count = 0

        // Process each record
        records.each { CSVRecord record ->
            try {
                NaiceCode naiceCode = new NaiceCode(
                    code: record.get("Code"),
                    title: record.get("Title"),
                    description: record.get("Description"),
                    sectorCode: record.get("SectorCode"),
                    sectorTitle: record.get("SectorTitle"),
                    subsectorCode: record.get("SubsectorCode"),
                    subsectorTitle: record.get("SubsectorTitle"),
                    industryGroupCode: record.get("IndustryGroupCode"),
                    industryGroupTitle: record.get("IndustryGroupTitle"),
                    naicsIndustryCode: record.get("NAICSIndustryCode"),
                    naicsIndustryTitle: record.get("NAICSIndustryTitle"),
                    nationalIndustryCode: record.get("NationalIndustryCode"),
                    nationalIndustryTitle: record.get("NationalIndustryTitle"),
                    yearIntroduced: parseInteger(record.get("YearIntroduced")),
                    yearUpdated: parseInteger(record.get("YearUpdated")),
                    isActive: parseBoolean(record.get("IsActive"), true)
                )

                naiceCodeRepository.save(naiceCode)
                count++

                if (count % 100 == 0) {
                    log.info "Processed ${count} NAICS codes"
                }
            } catch (Exception e) {
                log.error "Error processing NAICS code record: ${e.message}", e
            }
        }

        log.info "Imported ${count} NAICS codes from CSV file"
        return count
    }

    /**
     * Parse an integer from a string, returning null if the string is empty or invalid.
     */
    private Integer parseInteger(String value) {
        if (!value || value.trim() == "" || value.trim() == "N/A") {
            return null
        }
        
        try {
            return Integer.parseInt(value.trim())
        } catch (NumberFormatException e) {
            log.warn "Could not parse integer: ${value}"
            return null
        }
    }

    /**
     * Parse a boolean from a string, returning the default value if the string is empty or invalid.
     */
    private Boolean parseBoolean(String value, Boolean defaultValue) {
        if (!value || value.trim() == "" || value.trim() == "N/A") {
            return defaultValue
        }
        
        value = value.trim().toLowerCase()
        if (value == "true" || value == "yes" || value == "y" || value == "1") {
            return true
        } else if (value == "false" || value == "no" || value == "n" || value == "0") {
            return false
        } else {
            log.warn "Could not parse boolean: ${value}, using default: ${defaultValue}"
            return defaultValue
        }
    }

    /**
     * Ensures the database schema is properly set up for NAICS codes.
     * This method is called during application startup to ensure the table and required indexes exist.
     */
    @Transactional
    void ensureSchemaSetup() {
        // This method would set up any required database triggers or indexes
        // For now, it's a placeholder for future implementation
        log.info "Ensuring schema setup for NAICS codes"
    }
}
