package com.oconeco.spring_pgvector.service

import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.repository.NaicsCodeRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Integration test for NaicsCodeService that uses the actual database.
 * This test is particularly useful for diagnosing issues with PostgreSQL full-text search.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NaicsCodeServiceIntegrationTest extends Specification {

    @Autowired
    NaicsCodeService naicsCodeService

    @Autowired
    NaicsCodeRepository naicsCodeRepository

    def setup() {
        // Clear any existing data
        naicsCodeRepository.deleteAll()
        
        // Insert test data
        naicsCodeRepository.saveAll([
            new NaicsCode(
                code: "111110", 
                title: "Soybean Farming", 
                description: "This industry comprises establishments primarily engaged in growing soybeans and producing soybean seeds.",
                level: 5,
                sectorCode: "11",
                sectorTitle: "Agriculture, Forestry, Fishing and Hunting",
                subsectorCode: "111",
                subsectorTitle: "Crop Production",
                industryGroupCode: "1111",
                industryGroupTitle: "Oilseed and Grain Farming",
                naicsIndustryCode: "11111",
                naicsIndustryTitle: "Soybean Farming",
                isActive: true
            ),
            new NaicsCode(
                code: "311224", 
                title: "Soybean and Other Oilseed Processing", 
                description: "This industry comprises establishments primarily engaged in crushing soybeans and other oilseeds, such as cottonseeds and sunflower seeds, and extracting oils.",
                level: 5,
                sectorCode: "31",
                sectorTitle: "Manufacturing",
                subsectorCode: "311",
                subsectorTitle: "Food Manufacturing",
                industryGroupCode: "3112",
                industryGroupTitle: "Grain and Oilseed Milling",
                naicsIndustryCode: "31122",
                naicsIndustryTitle: "Starch and Vegetable Fats and Oils Manufacturing",
                isActive: true
            ),
            new NaicsCode(
                code: "325311", 
                title: "Nitrogenous Fertilizer Manufacturing", 
                description: "This industry comprises establishments primarily engaged in manufacturing nitrogenous fertilizer materials or mixed fertilizers from nitrogenous materials produced at the same establishment.",
                level: 5,
                sectorCode: "32",
                sectorTitle: "Manufacturing",
                subsectorCode: "325",
                subsectorTitle: "Chemical Manufacturing",
                industryGroupCode: "3253",
                industryGroupTitle: "Pesticide, Fertilizer, and Other Agricultural Chemical Manufacturing",
                naicsIndustryCode: "32531",
                naicsIndustryTitle: "Fertilizer Manufacturing",
                isActive: true
            )
        ])
        
        // Force a flush to ensure the database triggers run
        naicsCodeRepository.flush()
    }

    @Unroll
    def "should find NAICS codes containing '#searchTerm'"() {
        given:
        def pageable = PageRequest.of(0, 10)

        when:
        def results = naicsCodeService.searchNaicsCodes(searchTerm, pageable)
        
        then:
        results.totalElements >= expectedMinResults
        
        and: "Debug output to help diagnose search issues"
        println "Search term: $searchTerm"
        println "Total results: ${results.totalElements}"
        results.content.each { result ->
            println "- Code: ${result.naicsCode.code}, Title: ${result.naicsCode.title}"
            println "  Rank: ${result.rank}"
            println "  Highlighted Title: ${result.highlightedTitle}"
            println "  Highlighted Description: ${result.highlightedDescription}"
        }

        where:
        searchTerm                  | expectedMinResults
        "soybean"                   | 2
        "soybeans"                  | 2
        "soy"                       | 2
        "farming"                   | 1
        "fertilizer"                | 1
        "manufacturing"             | 2
        "agriculture"               | 1
        "oilseed"                   | 2
        "soybean farming"           | 1
        "soybean processing"        | 1
    }

    def "should perform case-insensitive search"() {
        given:
        def pageable = PageRequest.of(0, 10)

        when:
        def results = naicsCodeService.searchNaicsCodes("SOYBEAN", pageable)
        
        then:
        results.totalElements >= 2
    }

    def "should perform direct database query to diagnose search issues"() {
        given:
        def searchTerm = "soybean"
        def tsQuery = searchTerm + ":*"
        
        when:
        def count = naicsCodeRepository.countByFullTextSearch(tsQuery)
        def results = naicsCodeRepository.searchByFullText(tsQuery, PageRequest.of(0, 10))
        
        then:
        count >= 2
        results.totalElements >= 2
        
        and: "Debug output of raw database results"
        println "Direct query with tsQuery: $tsQuery"
        println "Total results from direct query: $count"
        results.content.each { row ->
            println "- Row: ${row}"
        }
    }

    def "should examine search_vector contents"() {
        when:
        // Use a native query to directly examine the search_vector column
        def vectors = naicsCodeRepository.findAll()
        
        then:
        vectors.size() >= 3
        
        and: "Debug output of search_vector contents"
        println "Examining search_vector contents:"
        vectors.each { naicsCode ->
            println "- Code: ${naicsCode.code}, Title: ${naicsCode.title}"
            // Note: We can't directly access the search_vector as it's not exposed in the entity
            // This is just to show that the records exist
        }
    }
}
