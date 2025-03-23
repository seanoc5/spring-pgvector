package com.oconeco.spring_pgvector.service

import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.repository.NaicsCodeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification
import spock.lang.Unroll

class NaicsCodeServiceTest extends Specification {

    NaicsCodeRepository naicsCodeRepository
    NaicsCodeService naicsCodeService

    def setup() {
        naicsCodeRepository = Mock(NaicsCodeRepository)
        naicsCodeService = new NaicsCodeService()
        naicsCodeService.naicsCodeRepository = naicsCodeRepository
    }

    def "should throw exception when search query is empty"() {
        when:
        naicsCodeService.searchNaicsCodes("", PageRequest.of(0, 10))

        then:
        thrown(IllegalArgumentException)
    }

    def "should throw exception when search query is null"() {
        when:
        naicsCodeService.searchNaicsCodes(null, PageRequest.of(0, 10))

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "should convert single term search query '#query' to tsquery '#expectedTsQuery'"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def emptyPage = new PageImpl<Object[]>([], pageable, 0)
        
        when:
        naicsCodeService.searchNaicsCodes(query, pageable)

        then:
        1 * naicsCodeRepository.countByFullTextSearch(expectedTsQuery) >> 1
        1 * naicsCodeRepository.searchByFullText(expectedTsQuery, pageable) >> emptyPage

        where:
        query      | expectedTsQuery
        "soybean"  | "soybean:*"
        "SOYBEAN"  | "SOYBEAN:*"
    }
    
    @Unroll
    def "should convert multiple term search query '#query' to OR-based tsquery '#expectedTsQuery'"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def emptyPage = new PageImpl<Object[]>([], pageable, 0)
        
        when:
        naicsCodeService.searchNaicsCodes(query, pageable)

        then:
        1 * naicsCodeRepository.countByFullTextSearch(expectedTsQuery) >> 1
        1 * naicsCodeRepository.searchByFullText(expectedTsQuery, pageable) >> emptyPage

        where:
        query                | expectedTsQuery
        "soybean farming"    | "soybean:* | farming:*"
        "  soybean  farming  " | "soybean:* | farming:*"
        "soy bean"           | "soy:* | bean:*"
    }
    
    def "should try stemming when no results found with prefix search"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def emptyPage = new PageImpl<Object[]>([], pageable, 0)
        def query = "soybean farming"
        def orTsQuery = "soybean:* | farming:*"
        def stemmedQuery = "soybean | farming"
        
        when:
        naicsCodeService.searchNaicsCodes(query, pageable)

        then:
        1 * naicsCodeRepository.countByFullTextSearch(orTsQuery) >> 0
        1 * naicsCodeRepository.countByFullTextSearch(stemmedQuery) >> 1
        1 * naicsCodeRepository.searchByFullText(stemmedQuery, pageable) >> emptyPage
    }
    
    def "should fall back to LIKE search when no results found with full-text search"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def query = "soybean farming"
        def orTsQuery = "soybean:* | farming:*"
        def stemmedQuery = "soybean | farming"
        def naicsCode = new NaicsCode(
            code: "111110", 
            title: "Soybean Farming", 
            description: "This industry comprises establishments primarily engaged in growing soybeans."
        )
        
        // Create a mock implementation for the LIKE search
        naicsCodeRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable) >> 
            new PageImpl<>([naicsCode], pageable, 1)
        
        // Set up the full-text search mocks
        naicsCodeRepository.countByFullTextSearch(orTsQuery) >> 0
        naicsCodeRepository.countByFullTextSearch(stemmedQuery) >> 0
        
        when:
        def result = naicsCodeService.searchNaicsCodes(query, pageable)

        then:
        result.totalElements == 1
        result.content.size() == 1
        result.content[0].naicsCode == naicsCode
        // Skip the highlighting check for now
    }

    def "should correctly transform search results"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def naicsCode = new NaicsCode(code: "111110", title: "Soybean Farming", description: "This industry comprises establishments primarily engaged in growing soybeans.")
        def searchResults = [
            [naicsCode, 0.8f, "<b>Soybean</b> Farming", "This industry comprises establishments primarily engaged in growing <b>soybeans</b>."] as Object[]
        ]
        def expectedTransformedResults = [
            [
                naicsCode: naicsCode,
                rank: 0.8f,
                highlightedTitle: "<b>Soybean</b> Farming",
                highlightedDescription: "This industry comprises establishments primarily engaged in growing <b>soybeans</b>."
            ]
        ]

        naicsCodeRepository.countByFullTextSearch("soybean:*") >> 1
        naicsCodeRepository.searchByFullText("soybean:*", pageable) >> new PageImpl<>(searchResults, pageable, 1)

        when:
        def result = naicsCodeService.searchNaicsCodes("soybean", pageable)

        then:
        result.totalElements == 1
        result.content.size() == 1
        result.content[0].naicsCode == naicsCode
        result.content[0].rank == 0.8f
        result.content[0].highlightedTitle == "<b>Soybean</b> Farming"
        result.content[0].highlightedDescription == "This industry comprises establishments primarily engaged in growing <b>soybeans</b>."
    }

    def "should handle string code in search results"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def naicsCode = new NaicsCode(code: "111110", title: "Soybean Farming", description: "This industry comprises establishments primarily engaged in growing soybeans.")
        def searchResults = [
            ["111110", 0.8f, "<b>Soybean</b> Farming", "This industry comprises establishments primarily engaged in growing <b>soybeans</b>."] as Object[]
        ]

        naicsCodeRepository.countByFullTextSearch("soybean:*") >> 1
        naicsCodeRepository.searchByFullText("soybean:*", pageable) >> new PageImpl<>(searchResults, pageable, 1)
        naicsCodeRepository.findById("111110") >> Optional.of(naicsCode)

        when:
        def result = naicsCodeService.searchNaicsCodes("soybean", pageable)

        then:
        result.totalElements == 1
        result.content.size() == 1
        result.content[0].naicsCode == naicsCode
    }

    def "should handle unexpected type in search results"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def searchResults = [
            [123, 0.8f, "<b>Soybean</b> Farming", "This industry comprises establishments primarily engaged in growing <b>soybeans</b>."] as Object[]
        ]

        naicsCodeRepository.countByFullTextSearch("soybean:*") >> 1
        naicsCodeRepository.searchByFullText("soybean:*", pageable) >> new PageImpl<>(searchResults, pageable, 1)

        when:
        def result = naicsCodeService.searchNaicsCodes("soybean", pageable)

        then:
        result.totalElements == 1
        result.content.size() == 1
        result.content[0].naicsCode == null
    }

    def "should handle missing fields in search results"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def naicsCode = new NaicsCode(code: "111110", title: "Soybean Farming", description: "This industry comprises establishments primarily engaged in growing soybeans.")
        def searchResults = [
            [naicsCode] as Object[]
        ]

        naicsCodeRepository.countByFullTextSearch("soybean:*") >> 1
        naicsCodeRepository.searchByFullText("soybean:*", pageable) >> new PageImpl<>(searchResults, pageable, 1)

        when:
        def result = naicsCodeService.searchNaicsCodes("soybean", pageable)

        then:
        result.totalElements == 1
        result.content.size() == 1
        result.content[0].naicsCode == naicsCode
        result.content[0].rank == 0.0f
        result.content[0].highlightedTitle == ""
        result.content[0].highlightedDescription == ""
    }
}
