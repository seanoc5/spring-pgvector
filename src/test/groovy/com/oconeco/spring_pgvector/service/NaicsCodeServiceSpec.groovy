package com.oconeco.spring_pgvector.service

import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.repository.NaicsCodeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import spock.lang.Specification

class NaicsCodeServiceSpec extends Specification {

    NaicsCodeRepository naicsCodeRepository
    NaicsCodeService naicsCodeService

    def setup() {
        naicsCodeRepository = Mock(NaicsCodeRepository)
        naicsCodeService = new NaicsCodeService()
        naicsCodeService.naicsCodeRepository = naicsCodeRepository
    }

    def "getAllNaicsCodes should return all NAICS codes with pagination"() {
        given:
        def pageable = PageRequest.of(0, 10)
        def naicsCodes = [new NaicsCode(code: "11", title: "Agriculture"), new NaicsCode(code: "21", title: "Mining")]
        def page = new PageImpl<>(naicsCodes, pageable, naicsCodes.size())

        when:
        def result = naicsCodeService.getAllNaicsCodes(pageable)

        then:
        1 * naicsCodeRepository.findAll(pageable) >> page
        result.content.size() == 2
        result.content[0].code == "11"
        result.content[1].code == "21"
    }

    def "getNaicsCodeByCode should return a NAICS code by its code"() {
        given:
        def code = "11"
        def naicsCode = new NaicsCode(code: code, title: "Agriculture")

        when:
        def result = naicsCodeService.getNaicsCodeByCode(code)

        then:
        1 * naicsCodeRepository.findById(code) >> Optional.of(naicsCode)
        result.code == code
        result.title == "Agriculture"
    }

    def "getNaicsCodeByCode should return null when code not found"() {
        given:
        def code = "999999"

        when:
        def result = naicsCodeService.getNaicsCodeByCode(code)

        then:
        1 * naicsCodeRepository.findById(code) >> Optional.empty()
        result == null
    }

    def "createNaicsCode should create a new NAICS code"() {
        given:
        def naicsCode = new NaicsCode(code: "11", title: "Agriculture")

        when:
        def result = naicsCodeService.createNaicsCode(naicsCode)

        then:
        1 * naicsCodeRepository.existsById("11") >> false
        1 * naicsCodeRepository.save(naicsCode) >> naicsCode
        result.code == "11"
        result.title == "Agriculture"
    }

    def "createNaicsCode should throw exception when code already exists"() {
        given:
        def naicsCode = new NaicsCode(code: "11", title: "Agriculture")

        when:
        naicsCodeService.createNaicsCode(naicsCode)

        then:
        1 * naicsCodeRepository.existsById("11") >> true
        thrown(IllegalArgumentException)
    }

    def "createNaicsCode should throw exception when code is empty"() {
        given:
        def naicsCode = new NaicsCode(code: "", title: "Agriculture")

        when:
        naicsCodeService.createNaicsCode(naicsCode)

        then:
        0 * naicsCodeRepository.existsById(_)
        thrown(IllegalArgumentException)
    }

    def "updateNaicsCode should update an existing NAICS code"() {
        given:
        def code = "11"
        def naicsCode = new NaicsCode(code: code, title: "Updated Agriculture")

        when:
        def result = naicsCodeService.updateNaicsCode(code, naicsCode)

        then:
        1 * naicsCodeRepository.existsById(code) >> true
        1 * naicsCodeRepository.save(naicsCode) >> naicsCode
        result.code == code
        result.title == "Updated Agriculture"
    }

    def "updateNaicsCode should throw exception when code not found"() {
        given:
        def code = "999999"
        def naicsCode = new NaicsCode(code: code, title: "Not Found")

        when:
        naicsCodeService.updateNaicsCode(code, naicsCode)

        then:
        1 * naicsCodeRepository.existsById(code) >> false
        thrown(IllegalArgumentException)
    }

    def "deleteNaicsCode should delete a NAICS code"() {
        given:
        def code = "11"

        when:
        naicsCodeService.deleteNaicsCode(code)

        then:
        1 * naicsCodeRepository.existsById(code) >> true
        1 * naicsCodeRepository.deleteById(code)
    }

    def "deleteNaicsCode should throw exception when code not found"() {
        given:
        def code = "999999"

        when:
        naicsCodeService.deleteNaicsCode(code)

        then:
        1 * naicsCodeRepository.existsById(code) >> false
        thrown(IllegalArgumentException)
    }

    def "findByLevel should return NAICS codes by level"() {
        given:
        def level = 2
        def pageable = PageRequest.of(0, 10)
        def naicsCodes = [new NaicsCode(code: "11", title: "Agriculture", level: level), new NaicsCode(code: "21", title: "Mining", level: level)]
        def page = new PageImpl<>(naicsCodes, pageable, naicsCodes.size())

        when:
        def result = naicsCodeService.findByLevel(level, pageable)

        then:
        1 * naicsCodeRepository.findByLevel(level, pageable) >> page
        result.content.size() == 2
        result.content.every { it.level == level }
    }

    def "findByLevel should throw exception when level is invalid"() {
        given:
        def level = invalidLevel
        def pageable = PageRequest.of(0, 10)

        when:
        naicsCodeService.findByLevel(level, pageable)

        then:
        thrown(IllegalArgumentException)

        where:
        invalidLevel << [null, 0, 7]
    }

    def "searchNaicsCodes should perform full-text search"() {
        given:
        def query = "agriculture"
        def pageable = PageRequest.of(0, 10)
        def naicsCode = new NaicsCode(code: "11", title: "Agriculture")
        def searchResults = [
            [naicsCode, 0.8f, "<b>Agriculture</b>", "Description of <b>agriculture</b>"] as Object[]
        ]
        def page = new PageImpl<>(searchResults, pageable, searchResults.size())

        when:
        def result = naicsCodeService.searchNaicsCodes(query, pageable)

        then:
        1 * naicsCodeRepository.countByFullTextSearch("agriculture:*") >> 1L
        1 * naicsCodeRepository.searchByFullText("agriculture:*", pageable) >> page
        result.content.size() == 1
        result.content[0].naicsCode == naicsCode
        result.content[0].rank == 0.8f
        result.content[0].highlightedTitle == "<b>Agriculture</b>"
    }

    def "searchNaicsCodes should throw exception when query is empty"() {
        given:
        def query = emptyQuery
        def pageable = PageRequest.of(0, 10)

        when:
        naicsCodeService.searchNaicsCodes(query, pageable)

        then:
        thrown(IllegalArgumentException)

        where:
        emptyQuery << [null, "", "  "]
    }
}
