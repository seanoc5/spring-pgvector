package com.oconeco.spring_pgvector.service

import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.repository.NaicsCodeRepository
import groovy.util.logging.Slf4j
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * Service for managing US Census NAICS codes.
 */
@Service
@Slf4j
class NaicsCodeService {

    @Autowired
    private NaicsCodeRepository naicsCodeRepository

    @Autowired
    private EmbeddingModel embeddingModel

    @Autowired
    private EmbeddingService embeddingService

    NaicsCodeService() {}

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

        // Generate and set the embedding vector
        List<Document> chunks = generateAndSetEmbedding(naicsCode)

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

        // Generate and set the embedding vector
        List<Document> chunks = generateAndSetEmbedding(naicsCode)
        def foo = naicsCodeRepository.save(naicsCode)
        return foo
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
     * Generate and set the embedding vector for a NAICS code.
     * This combines the title and description to create a rich text representation
     * for the embedding model.
     */
    private List<Document> generateAndSetEmbedding(NaicsCode naicsCode) {
        // Create a rich text representation by combining title and description
        StringBuilder textToEmbed = new StringBuilder()

        if (naicsCode.title) {
            textToEmbed.append(naicsCode.title)
        }

        if (naicsCode.description) {
            if (textToEmbed.length() > 0) {
                textToEmbed.append(". ")
            }
            textToEmbed.append(naicsCode.description)
        }

        // Add sector and industry information if available
        if (naicsCode.sectorTitle) {
            textToEmbed.append(". Sector: ${naicsCode.sectorTitle}")
        }

        if (naicsCode.industryGroupTitle) {
            textToEmbed.append(". Industry Group: ${naicsCode.industryGroupTitle}")
        }

        String finalText = textToEmbed.toString().trim()
        float[] embeddings = embeddingModel.embed(finalText)
        naicsCode.embeddingVector = embeddings
        log.debug("Embedding: ${embeddings}")

        List<Document> chunks = []

        if (finalText) {
            try {
                log.info("Generating embedding for NAICS code ${naicsCode.code}: ${finalText.take(100)}${finalText.length() > 100 ? '...' : ''}")
                Document document = new Document(finalText, [code: naicsCode.code, type:'document'])
                chunks = embeddingService.embedDocuments([document])

                // Explicitly set embeddingVector to null if we can't generate it
//                naicsCode.embeddingVector = null

                if(chunks) {
                    log.info("\t\tGot back (${chunks.size()}) chunks")
                } else {
                    log.warn "No chunks returned??? $chunks"
                }
            } catch (Exception e) {
                log.error("Error generating embedding for NAICS code ${naicsCode.code}", e)
                // Explicitly set embeddingVector to null
//                naicsCode.embeddingVector = null
            }
        } else {
            log.warn("No text available to generate embedding for NAICS code ${naicsCode.code}")
            // Explicitly set embeddingVector to null
//            naicsCode.embeddingVector = null
        }
        return chunks
    }

    /**
     * Find semantically similar NAICS codes using vector similarity search.
     */
    List<Map<String, Object>> findSimilarNaicsCodes(String query, int limit) {
        try {
            float[] queryEmbedding = embeddingService.embedQuery(query)
            List<Object[]> results = naicsCodeRepository.findSimilarByEmbedding(queryEmbedding, limit)

            return transformVectorSearchResults(results)
        } catch (Exception e) {
            log.error("Error searching for similar NAICS codes", e)
            return []
        }
    }

    /**
     * Find semantically similar NAICS codes using vector similarity search with pagination.
     */
    Page<Map<String, Object>> findSimilarNaicsCodesPaged(String query, Pageable pageable) {
        try {
            float[] queryEmbedding = embeddingService.embedQuery(query)
            Page<Object[]> results = naicsCodeRepository.findSimilarByEmbeddingPaged(queryEmbedding, pageable)

            List<Map<String, Object>> transformedResults = transformVectorSearchResults(results.content)
            return new PageImpl<>(transformedResults, pageable, results.totalElements)
        } catch (Exception e) {
            log.error("Error searching for similar NAICS codes with pagination", e)
            return Page.empty(pageable)
        }
    }

    /**
     * Transform vector search results into a list of maps.
     */
    private List<Map<String, Object>> transformVectorSearchResults(List<Object[]> results) {
        return results.collect { Object[] row ->
            NaicsCode naicsCode = row[0] instanceof NaicsCode ? row[0] : naicsCodeRepository.findById(row[0].toString()).orElse(null)
            BigDecimal similarity = row[row.length - 1] instanceof Number ? new BigDecimal(row[row.length - 1].toString()) : BigDecimal.ZERO

            if (naicsCode) {
                return [
                        code       : naicsCode.code,
                        title      : naicsCode.title,
                        description: naicsCode.description,
                        level      : naicsCode.level,
                        sectorCode : naicsCode.sectorCode,
                        sectorTitle: naicsCode.sectorTitle,
                        similarity : similarity.setScale(4, BigDecimal.ROUND_HALF_UP)
                ]
            } else {
                return [:]
            }
        }.findAll { it }
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
            // todo -- catch this at the controller, and handle there with perhaps a redirect to a basic 'list'?
            log.info("Empty query, so just do a basic list functionality...")
            // Instead of throwing an exception, return all NAICS codes
            Page<NaicsCode> allCodes = getAllNaicsCodes(pageable)

            // Transform to the same format as full-text search results
            return new org.springframework.data.domain.PageImpl<>(
                allCodes.content.collect { naicsCode ->
                    [
                        naicsCode: naicsCode,
                        rank: 0.0,
                        highlightedTitle: naicsCode.title,
                        highlightedDescription: naicsCode.description
                    ]
                },
                pageable,
                allCodes.totalElements
            )
        }

        // Normalize the query
        String normalizedQuery = query.trim().replaceAll(/\s+/, " ")

        // Try different query formats for PostgreSQL full-text search

        // First attempt: Use the query directly with plainto_tsquery (handles natural language)
        String plainTsQuery = normalizedQuery

        log.info("Search query: '${normalizedQuery}', using plain query first")

        // Get the total count
        long totalCount = naicsCodeRepository.countByFullTextSearch(plainTsQuery)

        // If no results, try with formatted tsquery
        if (totalCount == 0) {
            // Create different query formats for to_tsquery:
            // 1. OR-based query for multiple terms: word1:* | word2:* | ...
            // 2. Single term with prefix matching: word:*
            String[] terms = normalizedQuery.split(" ")

            String formattedTsQuery
            if (terms.length > 1) {
                formattedTsQuery = terms.collect { word -> word + ":*" }.join(" | ")
                log.info("---- No results with plain query, trying OR-based query: '${formattedTsQuery}'")
            } else {
                formattedTsQuery = normalizedQuery + ":*"
                log.info("---- No results with plain query, trying prefix matching: '${formattedTsQuery}'")
            }

            totalCount = naicsCodeRepository.countByFullTextSearch(formattedTsQuery)

            // If still no results, try with stemming by removing the :* suffix
            if (totalCount == 0 && terms.length > 1) {
                formattedTsQuery = terms.join(" | ")
                log.info("---- No results with prefix matching, trying with stemming: '${formattedTsQuery}'")
                totalCount = naicsCodeRepository.countByFullTextSearch(formattedTsQuery)
            }

            // If we found results with a formatted query, use that
            if (totalCount > 0) {
                log.info("---- Using plainTsQuery: `$formattedTsQuery`")
                plainTsQuery = formattedTsQuery
            }
        }

        // If still no results, try simple LIKE search as fallback
        if (totalCount == 0) {
            log.info("---- No results with full-text search, falling back to LIKE search")
            return searchByLike(normalizedQuery, pageable)
        }

        // Get the paginated results
        Page<Object[]> searchResults = naicsCodeRepository.searchByFullText(plainTsQuery, pageable)

        // Transform the results into a list of maps
        List<Map<String, Object>> transformedResults = searchResults.getContent().collect { Object[] row ->
            def naicsCode
            // todo -- refactor/fix-me, this is LLM generated, and looks sub-par to me...
            if (row[0] instanceof NaicsCode) {
                naicsCode = row[0]
            } else if (row[0] instanceof String) {
                naicsCode = naicsCodeRepository.findById(row[0].toString()).orElse(null)
            } else {
                log.warn "Unexpected type for row[0]: ${row[0]?.class?.name}"
                naicsCode = null
            }

            Float rank = row.length > 1 ? (row[21] as Float) : 0.0f
            String highlightedTitle = row.length > 2 ? (row[22] as String) : ""
            String highlightedDescription = row.length > 3 ? (row[23] as String) : ""

            return [
                    naicsCode             : naicsCode,
                    rank                  : rank,
                    highlightedTitle      : highlightedTitle,
                    highlightedDescription: highlightedDescription
            ]
        }

        return new org.springframework.data.domain.PageImpl<>(transformedResults, pageable, totalCount)
    }

    /**
     * Fallback search using LIKE operator when full-text search returns no results.
     */
    private Page<Map<String, Object>> searchByLike(String query, Pageable pageable) {
        // Search in title and description using LIKE
        Page<NaicsCode> results = naicsCodeRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable)

        // Transform to the same format as full-text search results
        List<Map<String, Object>> transformedResults = results.content.collect { NaicsCode naicsCode ->
            // Highlight the matching parts (simple implementation)
            String highlightedTitle = naicsCode.title?.replaceAll(
                    /(?i)(${Pattern.quote(query)})/,
                    '<b>$1</b>'
            )

            String highlightedDescription = naicsCode.description?.replaceAll(
                    /(?i)(${Pattern.quote(query)})/,
                    '<b>$1</b>'
            )

            return [
                    naicsCode             : naicsCode,
                    rank                  : 0.5f, // Default rank for LIKE results
                    highlightedTitle      : highlightedTitle ?: naicsCode.title,
                    highlightedDescription: highlightedDescription ?: naicsCode.description
            ]
        }

        return new org.springframework.data.domain.PageImpl<>(transformedResults, pageable, results.totalElements)
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

                // Generate and set the embedding vector
                List<Document> chunks = generateAndSetEmbedding(naicsCode)

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
     * Import NAICS codes from a CSV file with option to clear existing records.
     * @param csvFile The CSV file to import
     * @param clearExisting Whether to clear existing NAICS codes before import
     * @return The number of NAICS codes imported
     */
    @Transactional
    int importFromCsv(File csvFile, boolean clearExisting) {
        log.info "Importing NAICS codes from CSV file: ${csvFile.absolutePath} (clearExisting: ${clearExisting})"

        if (clearExisting) {
            clearAllNaicsCodes()
        }

        return importFromCsv(csvFile)
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

        Map<String, NaicsCode> codesMap = [:]
        // Process each row starting from row 1 (skipping header)
        for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            def row = sheet.getRow(rowNum)
            if (row == null) continue

            try {
                // Extract cell values
                def codeCell = row.getCell(columnMap['2022 NAICS US   Code'])
                if (codeCell == null){
                    log.debug "Null Code cell in row:[$row] -- skipping"
                    continue
                }

                // Convert cell to string based on cell type
                def code = getCellValueAsString(codeCell)
                if (!code){
                    log.debug "Empty code value? row: $row -- skipping"
                    continue
                }

                def title = cleanTextValue(row.getCell(columnMap['2022 NAICS US Title']).getStringCellValue())
                def description = cleanTextValue(row.getCell(columnMap['Description']).getStringCellValue())
                if(!title || !description){
                    log.warn "Empty title or description? row: $row"
                }

                // Determine NAICS level based on code length
                int level = determineNaicsLevel(code)

                // Create and save the NAICS code entity
                // todo -- fix hard-coded year `2022` below (not important, but good housekeeping)
                def naicsCode = new NaicsCode(
                        code: code,
                        title: title,
                        description: description,
                        level: level,
                        yearIntroduced: 2022, // Based on the column header "2022 NAICS US Code"
                        isActive: true
                )
                codesMap[code] = naicsCode
                log.info("\t\tloaded code: $naicsCode")

            } catch (Exception e) {
                log.error "Error loading NAICS code at row ${rowNum + 1}: ${e.message}", e
            }
        }

        log.info("Process (${codesMap.size()}) NAICS codes...")
        codesMap.each { code, naicsCode ->
            try {
                log.info("\t\tprocessing code: $naicsCode")
                // Set hierarchical relationships based on code length
                setHierarchicalRelationships(naicsCode, codesMap)

                // Temporarily commented out to bypass pgvector issues
                // Generate and set the embedding vector
                List<Document> chunks = generateAndSetEmbedding(naicsCode)

                // Save to database
                naicsCodeRepository.save(naicsCode)
                count++

                if (count % 100 == 0) {
                    log.info "Processed ${count} NAICS codes -- current:(${naicsCode.code})"
                }
            } catch (Exception e) {
                log.error "Error updating NAICS code ($naicsCode): ${e.message}"
            }
        }


        log.info("Close the workbook to release resources")
        workbook.close()

        log.info " Imported  ${codesMap.size()} NAICS codes; from Excel; file "
        return count
    }

/**
 * Import NAICS codes from an Excel file with option to clear existing records.
 * @param excelFile The Excel file to import
 * @param clearExisting Whether to clear existing NAICS codes before import
 * @return The number of NAICS codes imported
 */
    @Transactional
    int importFromExcel(File excelFile, boolean clearExisting) {
        log.info "Importing NAICS codes from Excel file: ${excelFile.absolutePath} (clearExisting: ${clearExisting})"

        if (clearExisting) {
            clearAllNaicsCodes()
        }

        return importFromExcel(excelFile)
    }

    private String cleanTextValue(String value) {
        if (value.trim()) {
            if (value.endsWith('T')) {
                log.debug("Removing trailing 'T' from value: ${value}")
                return value.substring(0, value.length() - 1)
            } else {
                log.debug("No trailing 'T' found in value: ${value}")
            }
        } else {
            log.debug("Value is null or empty: ${value}")
        }
        return value
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
    private void setHierarchicalRelationships(NaicsCode naicsCode, Map<String, NaicsCode> codesMap) {
        if (!naicsCode.code) return

        String code = naicsCode.code.replaceAll("\\D", "") // Remove non-digits

        // Set sector (2-digit) information
        if (code.length() == 2) {
            log.info("Sector parent: $naicsCode")
            naicsCode.sectorTitle = naicsCode.title
        } else if (code.length() > 2) {
            String sectorCode = code.substring(0, 2)
            naicsCode.sectorCode = sectorCode

            // Try to get the sector title from the database
//            def sector = naicsCodeRepository.findById(sectorCode).orElse(null)
            def sector = codesMap.get(sectorCode)
            if (sector) {
                naicsCode.sectorTitle = sector.title
            } else {
                log.warn "No sector parent (title) found for code:${naicsCode.code}: ${naicsCode.title}"
            }


            // Set subsector (3-digit) information
            if (code.length() >= 3) {
                String subsectorCode = code.substring(0, 3)
                naicsCode.subsectorCode = subsectorCode

                // Try to get the subsector title from the database
                def subsector = codesMap.get(subsectorCode)
                if (subsector) {
                    naicsCode.subsectorTitle = subsector.title
                }
            }

            // Set industry group (4-digit) information
            if (code.length() >= 4) {
                String industryGroupCode = code.substring(0, 4)
                naicsCode.industryGroupCode = industryGroupCode

                // Try to get the industry group title from the database
                def industryGroup = codesMap.get(industryGroupCode)
                if (industryGroup) {
                    naicsCode.industryGroupTitle = industryGroup.title
                }
            }

            // Set NAICS industry (5-digit) information
            if (code.length() >= 5) {
                String naicsIndustryCode = code.substring(0, 5)
                naicsCode.naicsIndustryCode = naicsIndustryCode

                // Try to get the NAICS industry title from the database
                def naicsIndustry = codesMap.get(naicsIndustryCode)
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
                    log.warn("This code ($naicsCode) seems broken...: $naicsCode")
                }
            }
        } else {
            log.warn("Code:($code) is level 1(???): [${naicsCode}], this should not happen?? ")
//            naicsCode.sectorTitle = naicsCode.title
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
 * Clear all NAICS codes from the database.
 */
    @Transactional
    void clearAllNaicsCodes() {
        log.warn "Clearing all NAICS codes from the database"
        naicsCodeRepository.truncateAllNaicsCodes()
        log.info "All NAICS codes have been deleted"
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
