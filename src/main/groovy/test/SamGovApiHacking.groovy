package test

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.io.entity.EntityUtils

//import org.apache.http.client.methods.HttpGet
//import org.apache.http.impl.client.CloseableHttpClient
//import org.apache.http.impl.client.HttpClients
//import org.apache.http.util.EntityUtils

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * A simple proof-of-concept class to:
 *  - Query SAM.gov for open contract opportunities since the last crawl
 *  - Insert the opportunity details into a PostgreSQL database table
 *
 * This script is intended to run periodically (e.g. nightly).
 */
@Slf4j
class SamGovApiHacking {


    // Replace with the actual SAM.gov API endpoint and API key if needed.
//    static final String SAM_API_URL = "https://api.sam.gov/prod/opportunities/v1/search"
    static final String SAM_API_URL = "https://api.sam.gov/opportunities/v2/search"
    static final String API_KEY = "ebeUcxzefDVLhIuGHbmIsDejANLpfV12Fw7b2V2I"  // Remove if not required
    static String dataFolder = '/opt/data/data.gov/SAM.GOV'

    // PostgreSQL DB connection parameters. Update with your database details.
    static final String DB_URL = "jdbc:postgresql://localhost:5436/spring-pgvector"
    static final String DB_USER = "postgres"
    static final String DB_PASSWORD = "pass1234"
    static sdfFileNameAppropriate = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
    static sdfQueryAppropriate = new SimpleDateFormat("MM/dd/yyyy")

    static void main(String[] args) {
        try {
            log.info "Connect to PostgreSQL..."
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)

            // Check and create the opportunities table if it doesn't exist
            ensureOpportunitiesTableExists(connection)

            // For PoC, assume last crawl was yesterday; in production, persist this value.
            Date lastCrawlDate = getLastCrawlDate()
//            String formattedDate = '2025-03-01'     // lastCrawlDate.format('yyyy-MM-dd')
            String formattedDate = sdfQueryAppropriate.format(lastCrawlDate)    //  '06/01/2025'
            String fileNameDate = sdfFileNameAppropriate.format(new Date())

            // Build the query URL (adjust parameters as required by the SAM.gov API)
//            String queryUrl = "${SAM_API_URL}?lastUpdated=${formattedDate}&status=OPEN&api_key=${API_KEY}&ptype=o&limit=100"
            String queryUrl = "${SAM_API_URL}?rdlto=${formattedDate}&status=active&api_key=${API_KEY}&ptype=o&limit=1000&postedFrom=01/01/2025&postedTo=12/31/2025"
            log.info "Querying SAM.gov API: ${queryUrl}"

            File dataFolder = new File(dataFolder)

            // Create the HTTP client and prepare the GET request.
            CloseableHttpClient httpClient = HttpClients.createDefault()
            HttpGet httpGet = new HttpGet(queryUrl)

            // Execute the request and parse the JSON response.
            def response = httpClient.execute(httpGet)
            def responseBody = EntityUtils.toString(response.getEntity())
            def json = new JsonSlurper().parseText(responseBody)
            if(json.error){
                log.error("API Request error: ${json.error}")
            } else {
                log.info "Received response with ${json.opportunitiesData?.size() ?: 0} opportunities."
                File cacheFile = new File(dataFolder, "samgov_opportunities_${fileNameDate}.json")
                cacheFile << responseBody

                // Insert opportunities
                json.opportunitiesData?.each { opportunity ->
                    insertOpportunity(connection, opportunity)
                }
            }
            connection.close()
            httpClient.close()

            // Update last crawl date as needed (for PoC, we simply print it)
            updateLastCrawlDate(new Date())
        }
        catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if the opportunities table exists in the database and creates it if it doesn't.
     * The table includes the required fields: id, title, description, and published_date.
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

            // Create the table with the required fields
            Statement stmt = connection.createStatement()
            String createTableSQL = """
                CREATE TABLE opportunities (
                    id VARCHAR(255) PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT,
                    published_date TIMESTAMP NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """
            stmt.execute(createTableSQL)

            // Create an index on published_date for better query performance
            String createIndexSQL = "CREATE INDEX idx_opportunities_published_date ON opportunities(published_date)"
            stmt.execute(createIndexSQL)

            stmt.close()
            log.info "Opportunities table created successfully."
        } else {
            log.info "Opportunities table already exists."
        }
    }

    /**
     * For the proof-of-concept, we assume the last crawl was 1 day ago.
     * In a production system, this should be read from a persistent store.
     */
    static Date getLastCrawlDate() {
        Calendar cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        return cal.getTime()
    }

    /**
     * Placeholder to update the last crawl date.
     * In production, store this value in a database or file.
     */
    static void updateLastCrawlDate(Date date) {
        log.info "Updating last crawl date to: ${date}"
        // Implement persistent update logic here.
    }

    /**
     * Inserts an opportunity record into the 'opportunities' table.
     * Assumes a table with at least the following columns: id, title, description, published_date.
     *
     * Adjust field names and data types according to your database schema.
     */
    static void insertOpportunity(Connection connection, Map opportunity) {
        // Extract values from the opportunity; adjust keys based on actual API response structure.
        String id = opportunity.id?.toString() ?: ""
        String title = opportunity.title ?: ""
        String description = opportunity.description ?: ""
        // Here we assume the API returns a date string; adjust the conversion as needed.
        Timestamp publishedTimestamp = opportunity.publishedDate ? Timestamp.valueOf(opportunity.publishedDate) : new Timestamp(new Date().time)

        String sql = "INSERT INTO opportunities (id, title, description, published_date) VALUES (?, ?, ?, ?)"
        PreparedStatement stmt = connection.prepareStatement(sql)
        stmt.setString(1, id)
        stmt.setString(2, title)
        stmt.setString(3, description)
        stmt.setTimestamp(4, publishedTimestamp)
        stmt.executeUpdate()
        stmt.close()
        log.info "Inserted opportunity with id: ${id}"
    }
}
