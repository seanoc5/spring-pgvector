package test

import groovy.util.logging.Slf4j
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

import java.nio.charset.StandardCharsets
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.sql.Types
import java.text.SimpleDateFormat
import java.util.Arrays

/**
 * A utility class to import SAM.gov contract opportunities from a CSV file into a PostgreSQL database.
 * This script reads the CSV file, creates or updates the opportunities table schema,
 * and imports the data into the database.
 */
@Slf4j
class SamGovCsvImporter {

    // PostgreSQL DB connection parameters
    static final String DB_URL = "jdbc:postgresql://localhost:5436/spring-pgvector"
    static final String DB_USER = "postgres"
    static final String DB_PASSWORD = "pass1234"
    public static final String VECTOR_DIMENSION = 768          // todo -- match this to the actual embedding model!!1!

    // CSV file path
    static final String CSV_FILE_PATH = "/home/sean/Downloads/ContractOpportunities-20250316-065253.csv"

    // Date format for parsing dates from the CSV
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy hh:mm a z")

    static void main(String[] args) {
        try {
            // Check if we're in search mode
            if (args.length > 0 && args[0] == "--search") {
                // Remove the --search flag and pass remaining args to demonstrateSearch
                String[] searchArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]
                log.info "Search Args: $searchArgs"
                demonstrateSearch(searchArgs)
                return
            }

            log.info "Connecting to PostgreSQL..."
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)

            // Ensure the opportunities table exists with the correct schema
            ensureOpportunitiesTableExists(connection)

            // Import data from the CSV file
            importCsvData(connection)

            connection.close()
            log.info "Import completed successfully."
        } catch (Exception e) {
            log.error "Error during import: ${e.message}", e
            e.printStackTrace()
        }
    }

    /**
     * Checks if the opportunities table exists and creates it if it doesn't.
     * The table schema is based on the CSV headers.
     */
    static void ensureOpportunitiesTableExists(Connection connection) {
        log.info "Checking if opportunities table exists..."

        // Check if the table exists
        boolean tableExists = false
        ResultSet tables = connection.metaData.getTables(null, null, "opportunities", null)
        tableExists = tables.next()
        tables.close()

        if (!tableExists) {
            log.info "Opportunities table does not exist. Creating it now..."

            // Create the table with all fields from the CSV
            Statement stmt = connection.createStatement()
            String createTableSQL = """
                CREATE TABLE opportunities (
                    notice_id VARCHAR(255) PRIMARY KEY,
                    title TEXT,
                    description TEXT,
                    current_response_date TIMESTAMP,
                    last_modified_date TIMESTAMP,
                    last_published_date TIMESTAMP,
                    contract_opportunity_type VARCHAR(255),
                    poc_information TEXT,
                    active_inactive VARCHAR(50),
                    awardee TEXT,
                    contract_award_number VARCHAR(255),
                    contract_award_date TIMESTAMP,
                    naics TEXT,
                    psc TEXT,
                    modification_number VARCHAR(255),
                    set_aside TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    vector_embedding VECTOR(768),
                    search_vector TSVECTOR
                )
            """
            stmt.execute(createTableSQL)

            // Create indexes for better query performance
            stmt.execute("CREATE INDEX idx_opportunities_last_published_date ON opportunities(last_published_date)")
            stmt.execute("CREATE INDEX idx_opportunities_naics ON opportunities(naics)")
            stmt.execute("CREATE INDEX idx_opportunities_active_inactive ON opportunities(active_inactive)")

            // Create function to generate search vectors
            String createFunctionSQL = '''
                CREATE OR REPLACE FUNCTION opportunities_search_vector_update() RETURNS TRIGGER AS $$
                BEGIN
                    NEW.search_vector = 
                        setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
                        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql;
            '''
            stmt.execute(createFunctionSQL)

            // Create trigger to automatically update search vectors
            String createTriggerSQL = """
                CREATE TRIGGER opportunities_search_vector_update_trigger
                BEFORE INSERT OR UPDATE ON opportunities
                FOR EACH ROW
                EXECUTE FUNCTION opportunities_search_vector_update();
            """
            stmt.execute(createTriggerSQL)

            // Create GIN index for fast full-text search
            stmt.execute("CREATE INDEX idx_opportunities_search_vector ON opportunities USING GIN(search_vector)")

            stmt.close()
            log.info "Opportunities table created successfully."
        } else {
            log.info "Opportunities table already exists."

            // Check if we need to add any missing columns
            // This is a simplified approach - in production, you might want to check each column
            try {
                Statement stmt = connection.createStatement()

                // Try to add vector_embedding column if it doesn't exist
                try {
                    stmt.execute("ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS vector_embedding VECTOR(VECTOR_DIMENSION)")
                    log.info "Added vector_embedding column"
                } catch (Exception e) {
                    log.warn "Could not add vector_embedding column: ${e.message}"
                }

                // Add search_vector column if it doesn't exist
                try {
                    stmt.execute("ALTER TABLE opportunities ADD COLUMN IF NOT EXISTS search_vector TSVECTOR")
                    log.info "Added search_vector column"

                    // Create function to generate search vectors if it doesn't exist
                    String createFunctionSQL = """
                        CREATE OR REPLACE FUNCTION opportunities_search_vector_update() RETURNS TRIGGER AS \$\$
                        BEGIN
                            NEW.search_vector = 
                                setweight(to_tsvector('english', COALESCE(NEW.title, '')), 'A') ||
                                setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'B');
                            RETURN NEW;
                        END;
                        \$\$ LANGUAGE plpgsql;
                    """
                    stmt.execute(createFunctionSQL)

                    // Check if trigger exists
                    ResultSet triggers = connection.metaData.getTables(null, null, "opportunities_search_vector_update_trigger", null)
                    boolean triggerExists = triggers.next()
                    triggers.close()

                    if (!triggerExists) {
                        // Create trigger to automatically update search vectors
                        String createTriggerSQL = """
                            CREATE TRIGGER opportunities_search_vector_update_trigger
                            BEFORE INSERT OR UPDATE ON opportunities
                            FOR EACH ROW
                            EXECUTE FUNCTION opportunities_search_vector_update();
                        """
                        stmt.execute(createTriggerSQL)
                        log.info "Created search vector update trigger"
                    }

                    // Create GIN index if it doesn't exist
                    try {
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_opportunities_search_vector ON opportunities USING GIN(search_vector)")
                        log.info "Created search_vector GIN index"
                    } catch (Exception e) {
                        log.warn "Could not create search_vector GIN index: ${e.message}"
                    }

                    // Update existing records to populate the search_vector
                    stmt.execute("UPDATE opportunities SET search_vector = setweight(to_tsvector('english', COALESCE(title, '')), 'A') || setweight(to_tsvector('english', COALESCE(description, '')), 'B')")
                    log.info "Updated existing records with search vectors"

                } catch (Exception e) {
                    log.warn "Could not add search_vector column or related objects: ${e.message}"
                }

                stmt.close()
            } catch (Exception e) {
                log.warn "Error checking/updating table schema: ${e.message}"
            }
        }
    }

    /**
     * Imports data from the CSV file into the opportunities table.
     */
    static void importCsvData(Connection connection) {
        log.info "Importing data from CSV file: ${CSV_FILE_PATH}"

        File csvFile = new File(CSV_FILE_PATH)
        if (csvFile.exists()) {
            log.debug("Found source file: $csvFile")
        } else {
            throw new FileNotFoundException("CSV file not found: ${CSV_FILE_PATH}")
        }

        // Parse the CSV file
        BufferedReader csvReader = csvFile.newReader('UTF-8')

        Iterable<CSVRecord> records = CSVFormat.RFC4180.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(csvReader);

        // Prepare the insert statement
        String insertSQL = """
            INSERT INTO opportunities (
                notice_id, title, description, current_response_date, last_modified_date, 
                last_published_date, contract_opportunity_type, poc_information, 
                active_inactive, awardee, contract_award_number, contract_award_date, 
                naics, psc, modification_number, set_aside
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (notice_id) 
            DO UPDATE SET 
                title = EXCLUDED.title,
                description = EXCLUDED.description,
                current_response_date = EXCLUDED.current_response_date,
                last_modified_date = EXCLUDED.last_modified_date,
                last_published_date = EXCLUDED.last_published_date,
                contract_opportunity_type = EXCLUDED.contract_opportunity_type,
                poc_information = EXCLUDED.poc_information,
                active_inactive = EXCLUDED.active_inactive,
                awardee = EXCLUDED.awardee,
                contract_award_number = EXCLUDED.contract_award_number,
                contract_award_date = EXCLUDED.contract_award_date,
                naics = EXCLUDED.naics,
                psc = EXCLUDED.psc,
                modification_number = EXCLUDED.modification_number,
                set_aside = EXCLUDED.set_aside,
                updated_at = CURRENT_TIMESTAMP
        """

        PreparedStatement pstmt = connection.prepareStatement(insertSQL)

        int count = 0
        int batchSize = 100

        // Process each record
        records.each { CSVRecord record ->
            try {
                // Map CSV fields to database columns
                pstmt.setString(1, record.get("Notice ID"))
                pstmt.setString(2, record.get("Title"))
                pstmt.setString(3, record.get("Description"))

                // Parse dates - handle nulls properly
                Timestamp currentResponseDate = parseDate(record.get("Current Response Date"))
                if (currentResponseDate != null) {
                    pstmt.setTimestamp(4, currentResponseDate)
                } else {
                    pstmt.setNull(4, Types.TIMESTAMP)
                }

                Timestamp lastModifiedDate = parseDate(record.get("Last Modified Date"))
                if (lastModifiedDate != null) {
                    pstmt.setTimestamp(5, lastModifiedDate)
                } else {
                    pstmt.setNull(5, Types.TIMESTAMP)
                }

                Timestamp lastPublishedDate = parseDate(record.get("Last Published Date"))
                if (lastPublishedDate != null) {
                    pstmt.setTimestamp(6, lastPublishedDate)
                } else {
                    pstmt.setNull(6, Types.TIMESTAMP)
                }

                pstmt.setString(7, record.get("Contract Opportunity Type"))
                pstmt.setString(8, record.get("POC Information"))
                pstmt.setString(9, record.get("Active/Inactive"))
                pstmt.setString(10, record.get("Awardee"))
                pstmt.setString(11, record.get("Contract Award Number"))

                Timestamp contractAwardDate = parseDate(record.get("Contract Award Date"))
                if (contractAwardDate != null) {
                    pstmt.setTimestamp(12, contractAwardDate)
                } else {
                    pstmt.setNull(12, Types.TIMESTAMP)
                }

                pstmt.setString(13, record.get("NAICS"))
                pstmt.setString(14, record.get("PSC"))
                pstmt.setString(15, record.get("Modification Number"))
                pstmt.setString(16, record.get("Set Aside"))

                pstmt.addBatch()
                count++

                // Execute batch insert
                if (count % batchSize == 0) {
                    pstmt.executeBatch()
                    log.info "Processed ${count} records"
                }
            } catch (Exception e) {
                log.error "Error processing record: ${e.message}"
            }
        }

        // Execute any remaining records
        if (count % batchSize != 0) {
            pstmt.executeBatch()
        }

        pstmt.close()

        log.info "Imported ${count} records from CSV file"
    }

    /**
     * Parses a date string into a Timestamp.
     * Returns null if the date string is empty or invalid.
     */
    static Timestamp parseDate(String dateStr) {
        if (!dateStr || dateStr.trim() == "N/A") {
            return null
        }

        try {
            // Handle different date formats
            if (dateStr.contains("CET") || dateStr.contains("EDT") || dateStr.contains("EST")) {
                // Format like "Mar 18, 2025 12:00 PM CET"
                Date date = DATE_FORMAT.parse(dateStr)
                return new Timestamp(date.time)
            } else if (dateStr.contains(",")) {
                // Format like "Mar 27, 2025" without time
                SimpleDateFormat shortFormat = new SimpleDateFormat("MMM dd, yyyy")
                Date date = shortFormat.parse(dateStr)
                return new Timestamp(date.time)
            } else if (dateStr.contains("-") && dateStr.length() >= 10) {
                // Try to parse as ISO format
                return Timestamp.valueOf(dateStr.replace('T', ' ').substring(0, Math.min(19, dateStr.length())))
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
     * Searches opportunities using full text search on title and description fields.
     *
     * @param connection The database connection
     * @param query The search query string
     * @param limit Maximum number of results to return (default 100)
     * @param offset Pagination offset (default 0)
     * @return List of maps containing the search results
     */
    static List<Map> searchOpportunities(Connection connection, String query, int limit = 100, int offset = 0) {
        log.info "Searching opportunities with query: '${query}'"

        if (!query || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty")
        }

        // Convert the query to a tsquery expression
        String tsQuery = query.trim()
                .replaceAll(/\s+/, " ")  // Normalize whitespace
                .split(" ")
                .collect { word -> word + ":*" }  // Add prefix matching
                .join(" & ")             // AND operator between words

        // Prepare the SQL query with highlighting
        String sql = """
            SELECT 
                notice_id, 
                title,
                description,
                current_response_date,
                last_published_date,
                contract_opportunity_type,
                active_inactive,
                naics,
                ts_rank(search_vector, to_tsquery('english', ?)) AS rank,
                ts_headline('english', title, to_tsquery('english', ?), 'StartSel=<b>, StopSel=</b>, MaxWords=50, MinWords=10, MaxFragments=3') AS highlighted_title,
                ts_headline('english', description, to_tsquery('english', ?), 'StartSel=<b>, StopSel=</b>, MaxWords=100, MinWords=20, MaxFragments=3') AS highlighted_description
            FROM opportunities
            WHERE search_vector @@ to_tsquery('english', ?)
            ORDER BY rank DESC
            LIMIT ? OFFSET ?
        """

        List<Map> results = []

        try {
            PreparedStatement pstmt = connection.prepareStatement(sql)
            pstmt.setString(1, tsQuery)
            pstmt.setString(2, tsQuery)
            pstmt.setString(3, tsQuery)
            pstmt.setString(4, tsQuery)
            pstmt.setInt(5, limit)
            pstmt.setInt(6, offset)

            ResultSet rs = pstmt.executeQuery()

            while (rs.next()) {
                Map<String, Object> result = [
                    noticeId: rs.getString("notice_id"),
                    title: rs.getString("title"),
                    description: rs.getString("description"),
                    currentResponseDate: rs.getTimestamp("current_response_date"),
                    lastPublishedDate: rs.getTimestamp("last_published_date"),
                    contractOpportunityType: rs.getString("contract_opportunity_type"),
                    activeInactive: rs.getString("active_inactive"),
                    naics: rs.getString("naics"),
                    rank: rs.getFloat("rank"),
                    highlightedTitle: rs.getString("highlighted_title"),
                    highlightedDescription: rs.getString("highlighted_description")
                ]
                results.add(result)
            }

            rs.close()
            pstmt.close()

            log.info "Found ${results.size()} matching opportunities"

        } catch (Exception e) {
            log.error "Error searching opportunities: ${e.message}", e
            throw e
        }

        return results
    }

    /**
     * Counts the total number of opportunities matching a search query.
     *
     * @param connection The database connection
     * @param query The search query string
     * @return The count of matching records
     */
    static int countSearchResults(Connection connection, String query) {
        if (!query || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty")
        }

        // Convert the query to a tsquery expression
        String tsQuery = query.trim()
                .replaceAll(/\s+/, " ")  // Normalize whitespace
                .split(" ")
                .collect { word -> word + ":*" }  // Add prefix matching
                .join(" & ")             // AND operator between words

        String sql = "SELECT COUNT(*) FROM opportunities WHERE search_vector @@ to_tsquery('english', ?)"

        try {
            PreparedStatement pstmt = connection.prepareStatement(sql)
            pstmt.setString(1, tsQuery)

            ResultSet rs = pstmt.executeQuery()
            int count = 0

            if (rs.next()) {
                count = rs.getInt(1)
            }

            rs.close()
            pstmt.close()

            return count

        } catch (Exception e) {
            log.error "Error counting search results: ${e.message}", e
            throw e
        }
    }

    /**
     * Demonstrates how to use the full text search functionality.
     * This method can be called from main() to test the search capabilities.
     *
     * @param args Command line arguments (optional search query)
     */
    static void demonstrateSearch(String[] args) {
        String searchQuery = args.length > 0 ? args[0] : "software development"

        log.info "Demonstrating full text search with query: '${searchQuery}'"

        try {
            log.info "Connecting to PostgreSQL..."
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)

            // Count matching records
            int totalCount = countSearchResults(connection, searchQuery)
            log.info "Found ${totalCount} total matches for '${searchQuery}'"

            // Get paginated results
            int pageSize = 10
            int page = 1
            int offset = (page - 1) * pageSize

            List<Map> results = searchOpportunities(connection, searchQuery, pageSize, offset)

            // Display results
            log.info "Showing page ${page} (${results.size()} of ${totalCount} results):"
            results.eachWithIndex { result, index ->
                log.info "Result ${index + 1 + offset}:"
                log.info "  Notice ID: ${result.noticeId}"
                log.info "  Title: ${result.title}"
                log.info "  Rank: ${result.rank}"

                // Show highlighted title
                log.info "  Highlighted Title: ${result.highlightedTitle ?: 'No highlights'}"

                // Show highlighted description
                log.info "  Highlighted Description: ${result.highlightedDescription ?: 'No highlights'}"

                log.info "  Published: ${result.lastPublishedDate}"
                log.info "  Type: ${result.contractOpportunityType}"
                log.info "  Status: ${result.activeInactive}"
                log.info "---"
            }

            connection.close()
            log.info "Search demonstration completed."
        } catch (Exception e) {
            log.error "Error during search demonstration: ${e.message}", e
            e.printStackTrace()
        }
    }
}
