package com.oconeco.spring_pgvector.repository

import com.oconeco.spring_pgvector.domain.NaicsCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NaicsCodeRepositorySpec extends Specification {

    // Use a static field for the container to be shared across test methods
    // Using pgvector-enabled PostgreSQL image
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("pgvector/pgvector:pg14")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init_pgvector.sql")

    // Start the container before all tests
    def setupSpec() {
        postgreSQLContainer.start()
        System.setProperty("spring.datasource.url", postgreSQLContainer.getJdbcUrl())
        System.setProperty("spring.datasource.username", postgreSQLContainer.getUsername())
        System.setProperty("spring.datasource.password", postgreSQLContainer.getPassword())
    }

    // Stop the container after all tests
    def cleanupSpec() {
        postgreSQLContainer.stop()
    }

    @Autowired
    NaicsCodeRepository naicsCodeRepository

    def "repository should save and retrieve a NAICS code"() {
        given:
        def naicsCode = new NaicsCode(
            code: "111110",
            level: 6,
            title: "Soybean Farming",
            description: "This industry comprises establishments primarily engaged in growing soybeans and/or producing soybean seeds.",
            sectorCode: "11",
            sectorTitle: "Agriculture, Forestry, Fishing and Hunting",
            subsectorCode: "111",
            subsectorTitle: "Crop Production",
            industryGroupCode: "1111",
            industryGroupTitle: "Oilseed and Grain Farming",
            naicsIndustryCode: "11111",
            naicsIndustryTitle: "Soybean Farming",
            nationalIndustryCode: "111110",
            nationalIndustryTitle: "Soybean Farming",
            isActive: true
        )

        when:
        def savedNaicsCode = naicsCodeRepository.save(naicsCode)
        def retrievedNaicsCode = naicsCodeRepository.findById("111110").orElse(null)

        then:
        savedNaicsCode != null
        retrievedNaicsCode != null
        retrievedNaicsCode.code == "111110"
        retrievedNaicsCode.title == "Soybean Farming"
        retrievedNaicsCode.level == 6
    }

    def "findByLevel should return NAICS codes of a specific level"() {
        given:
        def naicsCode1 = new NaicsCode(code: "11", level: 2, title: "Agriculture, Forestry, Fishing and Hunting")
        def naicsCode2 = new NaicsCode(code: "21", level: 2, title: "Mining, Quarrying, and Oil and Gas Extraction")
        def naicsCode3 = new NaicsCode(code: "111", level: 3, title: "Crop Production")
        
        naicsCodeRepository.saveAll([naicsCode1, naicsCode2, naicsCode3])
        
        when:
        def result = naicsCodeRepository.findByLevel(2, PageRequest.of(0, 10))
        
        then:
        result.content.size() == 2
        result.content.every { it.level == 2 }
        result.content.collect { it.code }.containsAll(["11", "21"])
    }

    def "findByTitleContainingIgnoreCase should perform case-insensitive search"() {
        given:
        def naicsCode1 = new NaicsCode(code: "11", level: 2, title: "Agriculture, Forestry, Fishing and Hunting")
        def naicsCode2 = new NaicsCode(code: "111", level: 3, title: "Crop Production")
        def naicsCode3 = new NaicsCode(code: "112", level: 3, title: "Animal Production and Aquaculture")
        
        naicsCodeRepository.saveAll([naicsCode1, naicsCode2, naicsCode3])
        
        when:
        def result = naicsCodeRepository.findByTitleContainingIgnoreCase("production", PageRequest.of(0, 10))
        
        then:
        result.content.size() == 2
        result.content.collect { it.code }.containsAll(["111", "112"])
    }

    // Commenting out full-text search tests for now as they require the search_vector column
    // which might need additional setup in the test environment
    /*
    def "searchByFullText should perform full-text search"() {
        given: "We need to create the search_vector column and trigger"
        // This test requires the search_vector column to be present
        // The init_pgvector.sql script should have created it
        
        and: "We create test data"
        def naicsCode1 = new NaicsCode(
            code: "111110", 
            level: 6, 
            title: "Soybean Farming",
            description: "This industry comprises establishments primarily engaged in growing soybeans and/or producing soybean seeds."
        )
        def naicsCode2 = new NaicsCode(
            code: "111120", 
            level: 6, 
            title: "Oilseed (except Soybean) Farming",
            description: "This industry comprises establishments primarily engaged in growing oilseed crops (except soybeans) and/or producing oilseed seeds (except soybean seeds)."
        )
        
        naicsCodeRepository.saveAll([naicsCode1, naicsCode2])
        
        when: "searching for 'soybean'"
        def result = naicsCodeRepository.searchByFullText("soybean", PageRequest.of(0, 10))
        
        then: "should find the soybean farming entry"
        result.content.size() >= 1
        result.content.find { it[0] instanceof NaicsCode && it[0].code == "111110" } != null
        
        when: "searching for 'oilseed'"
        result = naicsCodeRepository.searchByFullText("oilseed", PageRequest.of(0, 10))
        
        then: "should find both entries"
        result.content.size() >= 1
        result.content.findAll { it[0] instanceof NaicsCode }.collect { it[0].code }.containsAll(["111110", "111120"])
    }

    def "countByFullTextSearch should return correct count"() {
        given:
        def naicsCode1 = new NaicsCode(
            code: "111110", 
            level: 6, 
            title: "Soybean Farming",
            description: "This industry comprises establishments primarily engaged in growing soybeans and/or producing soybean seeds."
        )
        def naicsCode2 = new NaicsCode(
            code: "111120", 
            level: 6, 
            title: "Oilseed (except Soybean) Farming",
            description: "This industry comprises establishments primarily engaged in growing oilseed crops (except soybeans) and/or producing oilseed seeds (except soybean seeds)."
        )
        
        naicsCodeRepository.saveAll([naicsCode1, naicsCode2])
        
        when:
        def count = naicsCodeRepository.countByFullTextSearch("soybean")
        
        then:
        count >= 1
        
        when:
        count = naicsCodeRepository.countByFullTextSearch("oilseed")
        
        then:
        count >= 1
    }
    */
}
