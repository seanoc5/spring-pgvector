package com.oconeco.spring_pgvector.service


import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.repository.NaicsCodeRepository
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
class NaicsCodeService {

    @Autowired
    private NaicsCodeRepository naicsCodeRepository

    /**
     * Get all NAICS codes with pagination.
     */
    Page<NaicsCode> getAllNaicsCodes(Pageable pageable) {
        return naicsCodeRepository.findAll(pageable)
    }

    /**
     * Get a NAICS code by its code.
     */
    NaicsCode getNaicsCodeByCode(String code) {
        return naicsCodeRepository.findById(code).orElse(null)
    }

    /**
     * Create a new NAICS code.
     */
    @Transactional
    NaicsCode createNaicsCode(NaicsCode naicsCode) {
        if (!naicsCode.code) {
            throw new IllegalArgumentException("NAICS code cannot be empty")
        }

        if (naicsCodeRepository.existsById(naicsCode.code)) {
            throw new IllegalArgumentException("NAICS code ${naicsCode.code} already exists")
        }

        return naicsCodeRepository.save(naicsCode)
    }

    /**
     * Update an existing NAICS code.
     */
    @Transactional
    NaicsCode updateNaicsCode(String code, NaicsCode naicsCode) {
        if (!naicsCodeRepository.existsById(code)) {
            throw new IllegalArgumentException("NAICS code ${code} not found")
        }

        naicsCode.code = code  // Ensure the code is not changed
        return naicsCodeRepository.save(naicsCode)
    }

    /**
     * Delete a NAICS code.
     */
    @Transactional
    void deleteNaicsCode(String code) {
        if (!naicsCodeRepository.existsById(code)) {
            throw new IllegalArgumentException("NAICS code ${code} not found")
        }

        naicsCodeRepository.deleteById(code)
    }

    /**
     * Search NAICS codes by title.
     */
    Page<NaicsCode> searchByTitle(String title, Pageable pageable) {
        return naicsCodeRepository.findByTitleContainingIgnoreCase(title, pageable)
    }

    /**
     * Search NAICS codes by description.
     */
    Page<NaicsCode> searchByDescription(String description, Pageable pageable) {
        return naicsCodeRepository.findByDescriptionContainingIgnoreCase(description, pageable)
    }

    /**
     * Search NAICS codes by sector code.
     */
    Page<NaicsCode> searchBySectorCode(String sectorCode, Pageable pageable) {
        return naicsCodeRepository.findBySectorCode(sectorCode, pageable)
    }

    /**
     * Search NAICS codes by active status.
     */
    Page<NaicsCode> searchByActiveStatus(Boolean isActive, Pageable pageable) {
        return naicsCodeRepository.findByIsActive(isActive, pageable)
    }

    /**
     * Find NAICS codes by level.
     */
    Page<NaicsCode> findByLevel(Integer level, Pageable pageable) {
        if (level == null || level < 1 || level > 6) {
            throw new IllegalArgumentException("Level must be between 1 and 6")
        }
        return naicsCodeRepository.findByLevel(level, pageable)
    }

    /**
     * Search NAICS codes using full-text search.
     */
    Page<Map<String, Object>> searchNaicsCodes(String query, Pageable pageable) {
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
        long totalCount = naicsCodeRepository.countByFullTextSearch(tsQuery)

        // Get the paginated results
        Page<Object[]> searchResults = naicsCodeRepository.searchByFullText(tsQuery, pageable)

        // Transform the results into a list of maps
        List<Map<String, Object>> transformedResults = searchResults.getContent().collect { Object[] row ->
            def naicsCode
            if (row[0] instanceof NaicsCode) {
                naicsCode = row[0]
            } else if (row[0] instanceof String) {
                naicsCode = naicsCodeRepository.findById(row[0].toString()).orElse(null)
            } else {
                log.warn "Unexpected type for row[0]: ${row[0]?.class?.name}"
                naicsCode = null
            }

            Float rank = row.length > 1 ? (row[1] as Float) : 0.0f
            String highlightedTitle = row.length > 2 ? (row[2] as String) : ""
            String highlightedDescription = row.length > 3 ? (row[3] as String) : ""

            return [
                naicsCode: naicsCode,
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
                NaicsCode naicsCode = new NaicsCode(
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

                naicsCodeRepository.save(naicsCode)
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
     * Import NAICS codes from an Excel file.
     * 
     * @param excelFile The Excel file to import
     * @return The number of NAICS codes imported
     */
    @Transactional
    int importFromExcel(File excelFile) {
        log.info "Importing NAICS codes from Excel file: ${excelFile.absolutePath}"

        if (!excelFile.exists()) {
            throw new FileNotFoundException("Excel file not found: ${excelFile.absolutePath}")
        }

        // Open the Excel workbook
        def workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(excelFile)
        def sheet = workbook.getSheetAt(0) // Get the first sheet

        // Get the header row to map column names to indices
        def headerRow = sheet.getRow(0)
        def columnMap = [:]
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            def cell = headerRow.getCell(i)
            if (cell) {
                columnMap[cell.getStringCellValue().trim()] = i
            }
        }
        
        // Verify required columns exist
        def requiredColumns = ['Seq. No.', '2022 NAICS US   Code', '2022 NAICS US Title', 'Description']
        def missingColumns = requiredColumns.findAll { !columnMap.containsKey(it) }
        
        if (missingColumns) {
            throw new IllegalArgumentException("Missing required columns in Excel file: ${missingColumns.join(', ')}")
        }
        
        int count = 0
        
        // Process each row starting from row 1 (skipping header)
        for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            def row = sheet.getRow(rowNum)
            if (row == null) continue
            
            try {
                // Extract cell values
                def codeCell = row.getCell(columnMap['2022 NAICS US   Code'])
                if (codeCell == null) continue
                
                // Convert cell to string based on cell type
                def code = getCellValueAsString(codeCell)
                if (!code) continue
                
                def title = getCellValueAsString(row.getCell(columnMap['2022 NAICS US Title']))
                def description = getCellValueAsString(row.getCell(columnMap['Description']))
                
                // Determine NAICS level based on code length
                int level = determineNaicsLevel(code)
                
                // Create and save the NAICS code entity
                def naicsCode = new NaicsCode(
                    code: code,
                    title: title,
                    description: description,
                    level: level,
                    yearIntroduced: 2022, // Based on the column header "2022 NAICS US Code"
                    isActive: true
                )
                
                // Set hierarchical relationships based on code length
                setHierarchicalRelationships(naicsCode)
                
                // Save to database
                naicsCodeRepository.save(naicsCode)
                count++
                
                if (count % 100 == 0) {
                    log.info "Processed ${count} NAICS codes"
                }
            } catch (Exception e) {
                log.error "Error processing NAICS code at row ${rowNum + 1}: ${e.message}", e
            }
        }
        
        // Close the workbook to release resources
        workbook.close()
        
        log.info "Imported ${count} NAICS codes from Excel file"
        return count
    }
    
    /**
     * Get cell value as string regardless of cell type
     */
    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return ""
        
        switch (cell.getCellType()) {
            case org.apache.poi.ss.usermodel.CellType.STRING:
                return cell.getStringCellValue()?.trim()
            case org.apache.poi.ss.usermodel.CellType.NUMERIC:
                // Handle numeric cells, including dates
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue()?.toString()
                } else {
                    // For NAICS codes that might be stored as numbers, ensure they're formatted properly
                    def value = cell.getNumericCellValue()
                    if (value == Math.floor(value)) {
                        return String.format("%.0f", value)
                    } else {
                        return String.valueOf(value)
                    }
                }
            case org.apache.poi.ss.usermodel.CellType.BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue())
            case org.apache.poi.ss.usermodel.CellType.FORMULA:
                // Try to evaluate the formula
                def evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator()
                def cellValue = evaluator.evaluate(cell)
                return getCellValueAsString(cellValue)
            default:
                return ""
        }
    }
    
    /**
     * Determine NAICS level based on code length
     */
    private int determineNaicsLevel(String code) {
        if (!code) return 0
        
        code = code.replaceAll("\\D", "") // Remove non-digits
        
        switch (code.length()) {
            case 2: return 1 // Sector level (2 digits)
            case 3: return 2 // Subsector level (3 digits)
            case 4: return 3 // Industry Group level (4 digits)
            case 5: return 4 // NAICS Industry level (5 digits)
            case 6: return 5 // National Industry level (6 digits)
            default: return 0
        }
    }
    
    /**
     * Set hierarchical relationships for a NAICS code based on its code
     */
    private void setHierarchicalRelationships(NaicsCode naicsCode) {
        if (!naicsCode.code) return
        
        String code = naicsCode.code.replaceAll("\\D", "") // Remove non-digits
        
        // Set sector (2-digit) information
        if (code.length() >= 2) {
            String sectorCode = code.substring(0, 2)
            naicsCode.sectorCode = sectorCode
            
            // Try to get the sector title from the database
            def sector = naicsCodeRepository.findById(sectorCode).orElse(null)
            if (sector) {
                naicsCode.sectorTitle = sector.title
            }
        }
        
        // Set subsector (3-digit) information
        if (code.length() >= 3) {
            String subsectorCode = code.substring(0, 3)
            naicsCode.subsectorCode = subsectorCode
            
            // Try to get the subsector title from the database
            def subsector = naicsCodeRepository.findById(subsectorCode).orElse(null)
            if (subsector) {
                naicsCode.subsectorTitle = subsector.title
            }
        }
        
        // Set industry group (4-digit) information
        if (code.length() >= 4) {
            String industryGroupCode = code.substring(0, 4)
            naicsCode.industryGroupCode = industryGroupCode
            
            // Try to get the industry group title from the database
            def industryGroup = naicsCodeRepository.findById(industryGroupCode).orElse(null)
            if (industryGroup) {
                naicsCode.industryGroupTitle = industryGroup.title
            }
        }
        
        // Set NAICS industry (5-digit) information
        if (code.length() >= 5) {
            String naicsIndustryCode = code.substring(0, 5)
            naicsCode.naicsIndustryCode = naicsIndustryCode
            
            // Try to get the NAICS industry title from the database
            def naicsIndustry = naicsCodeRepository.findById(naicsIndustryCode).orElse(null)
            if (naicsIndustry) {
                naicsCode.naicsIndustryTitle = naicsIndustry.title
            }
        }
        
        // Set national industry (6-digit) information
        if (code.length() >= 6) {
            String nationalIndustryCode = code.substring(0, 6)
            naicsCode.nationalIndustryCode = nationalIndustryCode
            
            // If this is a 6-digit code, it is itself a national industry
            if (code.length() == 6 && naicsCode.code == nationalIndustryCode) {
                naicsCode.nationalIndustryTitle = naicsCode.title
            } else {
                // Try to get the national industry title from the database
                def nationalIndustry = naicsCodeRepository.findById(nationalIndustryCode).orElse(null)
                if (nationalIndustry) {
                    naicsCode.nationalIndustryTitle = nationalIndustry.title
                }
            }
        }
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
