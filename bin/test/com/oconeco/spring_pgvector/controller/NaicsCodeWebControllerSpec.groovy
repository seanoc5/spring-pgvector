package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.service.NaicsCodeService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.ui.Model
import spock.lang.Specification

class NaicsCodeWebControllerSpec extends Specification {

    NaicsCodeService naicsCodeService
    NaicsCodeWebController controller
    Model model

    def setup() {
        naicsCodeService = Mock(NaicsCodeService)
        controller = new NaicsCodeWebController()
        controller.naicsCodeService = naicsCodeService
        model = Mock(Model)
    }

    def "listNaicsCodes should display all NAICS codes when no filters applied"() {
        given:
        def pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "code"))
        def naicsCodes = [new NaicsCode(code: "11", title: "Agriculture"), new NaicsCode(code: "21", title: "Mining")]
        def page = new PageImpl<>(naicsCodes, pageable, naicsCodes.size())

        when:
        def viewName = controller.listNaicsCodes(model, null, null, 0, 20, "code", "asc")

        then:
        1 * naicsCodeService.getAllNaicsCodes(pageable) >> page
        1 * model.addAttribute("naicsCodes", page) >> model
        1 * model.addAttribute("activeLevel", null) >> model
        1 * model.addAttribute("currentPage", 0) >> model
        1 * model.addAttribute("totalPages", page.totalPages) >> model
        1 * model.addAttribute("totalItems", page.totalElements) >> model
        1 * model.addAttribute("sortBy", "code") >> model
        1 * model.addAttribute("sortDir", "asc") >> model
        1 * model.addAttribute("reverseSortDir", "desc") >> model
        viewName == "naics-codes/list"
    }

    def "listNaicsCodes should filter by level when level parameter is provided"() {
        given:
        def level = 2
        def pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "code"))
        def naicsCodes = [new NaicsCode(code: "11", title: "Agriculture", level: level), new NaicsCode(code: "21", title: "Mining", level: level)]
        def page = new PageImpl<>(naicsCodes, pageable, naicsCodes.size())

        when:
        def viewName = controller.listNaicsCodes(model, null, level, 0, 20, "code", "asc")

        then:
        1 * naicsCodeService.findByLevel(level, pageable) >> page
        1 * model.addAttribute("naicsCodes", page) >> model
        1 * model.addAttribute("activeLevel", level) >> model
        1 * model.addAttribute("currentPage", 0) >> model
        1 * model.addAttribute("totalPages", page.totalPages) >> model
        1 * model.addAttribute("totalItems", page.totalElements) >> model
        1 * model.addAttribute("sortBy", "code") >> model
        1 * model.addAttribute("sortDir", "asc") >> model
        1 * model.addAttribute("reverseSortDir", "desc") >> model
        viewName == "naics-codes/list"
    }

    def "listNaicsCodes should search when query parameter is provided"() {
        given:
        def query = "agriculture"
        def pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "code"))
        def naicsCode = new NaicsCode(code: "11", title: "Agriculture")
        def searchResults = [
            [naicsCode: naicsCode, rank: 0.8f, highlightedTitle: "<b>Agriculture</b>", highlightedDescription: "Description of <b>agriculture</b>"]
        ]
        def searchPage = new PageImpl<>(searchResults, pageable, searchResults.size())
        def naicsCodesPage = new PageImpl<>([naicsCode], pageable, 1)

        when:
        def viewName = controller.listNaicsCodes(model, query, null, 0, 20, "code", "asc")

        then:
        1 * naicsCodeService.searchNaicsCodes(query, pageable) >> searchPage
        1 * model.addAttribute("naicsCodes", _) >> { args ->
            Page<NaicsCode> page = args[1]
            assert page.content.size() == 1
            assert page.content[0].code == "11"
            return model
        }
        1 * model.addAttribute("activeLevel", null) >> model
        1 * model.addAttribute("query", query) >> model
        1 * model.addAttribute("searchResults", searchPage) >> model
        1 * model.addAttribute("currentPage", 0) >> model
        1 * model.addAttribute("totalPages", _) >> model
        1 * model.addAttribute("totalItems", _) >> model
        1 * model.addAttribute("sortBy", "code") >> model
        1 * model.addAttribute("sortDir", "asc") >> model
        1 * model.addAttribute("reverseSortDir", "desc") >> model
        viewName == "naics-codes/list"
    }

    def "viewNaicsCode should display a specific NAICS code"() {
        given:
        def code = "11"
        def naicsCode = new NaicsCode(code: code, title: "Agriculture")

        when:
        def viewName = controller.viewNaicsCode(code, model)

        then:
        1 * naicsCodeService.getNaicsCodeByCode(code) >> naicsCode
        1 * model.addAttribute("naicsCode", naicsCode) >> model
        viewName == "naics-codes/view"
    }

    def "viewNaicsCode should redirect when code not found"() {
        given:
        def code = "999999"

        when:
        def viewName = controller.viewNaicsCode(code, model)

        then:
        1 * naicsCodeService.getNaicsCodeByCode(code) >> null
        0 * model.addAttribute(_, _)
        viewName == "redirect:/naics-codes"
    }

    def "searchNaicsCodes should perform search and return results fragment"() {
        given:
        def query = "agriculture"
        def pageable = PageRequest.of(0, 10)
        def naicsCode = new NaicsCode(code: "11", title: "Agriculture")
        def searchResults = [
            [naicsCode: naicsCode, rank: 0.8f, highlightedTitle: "<b>Agriculture</b>", highlightedDescription: "Description of <b>agriculture</b>"]
        ]
        def page = new PageImpl<>(searchResults, pageable, searchResults.size())

        when:
        def viewName = controller.searchNaicsCodes(query, null, 0, 10, model)

        then:
        1 * naicsCodeService.searchNaicsCodes(query, pageable) >> page
        1 * model.addAttribute("query", query) >> model
        1 * model.addAttribute("searchResults", page) >> model
        1 * model.addAttribute("activeLevel", null) >> model
        1 * model.addAttribute("currentPage", 0) >> model
        1 * model.addAttribute("totalPages", page.totalPages) >> model
        1 * model.addAttribute("totalItems", page.totalElements) >> model
        viewName == "naics-codes/fragments/search-results :: results"
    }

    def "searchNaicsCodes should handle errors gracefully"() {
        given:
        def query = "agriculture"
        def pageable = PageRequest.of(0, 10)
        def exception = new RuntimeException("Search error")

        when:
        def viewName = controller.searchNaicsCodes(query, null, 0, 10, model)

        then:
        1 * naicsCodeService.searchNaicsCodes(query, pageable) >> { throw exception }
        1 * model.addAttribute("error", "An error occurred while searching: Search error") >> model
        viewName == "naics-codes/fragments/search-results :: error"
    }

    def "filterByLevel should filter results by level"() {
        given:
        def level = 2
        def pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "code"))
        def naicsCodes = [new NaicsCode(code: "11", title: "Agriculture", level: level), new NaicsCode(code: "21", title: "Mining", level: level)]
        def page = new PageImpl<>(naicsCodes, pageable, naicsCodes.size())

        when:
        def viewName = controller.filterByLevel(level, null, 0, 20, "code", "asc", model)

        then:
        1 * naicsCodeService.findByLevel(level, pageable) >> page
        1 * model.addAttribute("naicsCodes", page) >> model
        1 * model.addAttribute("activeLevel", level) >> model
        1 * model.addAttribute("currentPage", 0) >> model
        1 * model.addAttribute("totalPages", page.totalPages) >> model
        1 * model.addAttribute("totalItems", page.totalElements) >> model
        1 * model.addAttribute("sortBy", "code") >> model
        1 * model.addAttribute("sortDir", "asc") >> model
        1 * model.addAttribute("reverseSortDir", "desc") >> model
        viewName == "naics-codes/fragments/naics-table :: table"
    }
}
