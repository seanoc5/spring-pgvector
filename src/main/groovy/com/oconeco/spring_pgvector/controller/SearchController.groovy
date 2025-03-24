package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.service.SearchService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Controller for the unified search functionality
 * Provides web pages and HTMX endpoints for searching using different methods:
 * - Solr search
 * - PostgreSQL full-text search
 * - Spring-AI vector store search
 */
@Controller
@RequestMapping("/search")
@Slf4j
class SearchController {

    @Autowired
    private SearchService searchService

    /**
     * Main search page
     */
    @GetMapping(["", "/", "/index"])
    String searchPage(
            Model model,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "method", required = false, defaultValue = "all") String method,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "rank") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))

        // Add search parameters to model for form
        model.addAttribute("query", query)
        model.addAttribute("method", method)
        model.addAttribute("currentPage", page)
        model.addAttribute("pageSize", size)
        model.addAttribute("sortBy", sortBy)
        model.addAttribute("sortDir", sortDir)
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc")

        // If there's a query, perform search based on selected method
        if (query && !query.trim().isEmpty()) {
            try {
                switch (method.toLowerCase()) {
                    case "postgres":
                        def results = searchService.searchWithPostgres(query, pageRequest)
                        model.addAttribute("postgresResults", results)
                        break
                    case "solr":
                        def results = searchService.searchWithSolr(query, pageRequest)
                        model.addAttribute("solrResults", results)
                        break
                    case "vector":
                        def results = searchService.searchWithVectorStore(query, pageRequest)
                        model.addAttribute("vectorResults", results)
                        break
                    case "all":
                    default:
                        def allResults = searchService.searchWithAllMethods(query, pageRequest)
                        model.addAttribute("postgresResults", allResults.postgres)
                        model.addAttribute("solrResults", allResults.solr)
                        model.addAttribute("vectorResults", allResults.vector)
                        break
                }
            } catch (Exception e) {
                log.error("Error performing search: ${e.message}", e)
                model.addAttribute("error", "An error occurred while searching: ${e.message}")
            }
        }

        return "search/index"
    }

    /**
     * HTMX endpoint for PostgreSQL search results
     */
    @GetMapping("/postgres")
    String searchWithPostgres(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "rank") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            Model model) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))
            
            def results = searchService.searchWithPostgres(query, pageRequest)
            
            model.addAttribute("query", query)
            model.addAttribute("searchResults", results)
            model.addAttribute("currentPage", page)
            model.addAttribute("pageSize", size)
            model.addAttribute("totalPages", results.totalPages)
            model.addAttribute("totalItems", results.totalElements)
            model.addAttribute("sortBy", sortBy)
            model.addAttribute("sortDir", sortDir)
            model.addAttribute("searchMethod", "postgres")
            
            return "search/fragments/search-results :: results"
        } catch (Exception e) {
            log.error("Error searching with PostgreSQL: ${e.message}", e)
            model.addAttribute("error", "An error occurred while searching with PostgreSQL: ${e.message}")
            return "search/fragments/search-results :: error"
        }
    }

    /**
     * HTMX endpoint for Solr search results
     */
    @GetMapping("/solr")
    String searchWithSolr(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "rank") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            Model model) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))
            
            def results = searchService.searchWithSolr(query, pageRequest)
            
            model.addAttribute("query", query)
            model.addAttribute("searchResults", results)
            model.addAttribute("currentPage", page)
            model.addAttribute("pageSize", size)
            model.addAttribute("totalPages", results.totalPages)
            model.addAttribute("totalItems", results.totalElements)
            model.addAttribute("sortBy", sortBy)
            model.addAttribute("sortDir", sortDir)
            model.addAttribute("searchMethod", "solr")
            
            return "search/fragments/search-results :: results"
        } catch (Exception e) {
            log.error("Error searching with Solr: ${e.message}", e)
            model.addAttribute("error", "An error occurred while searching with Solr: ${e.message}")
            return "search/fragments/search-results :: error"
        }
    }

    /**
     * HTMX endpoint for Vector search results
     */
    @GetMapping("/vector")
    String searchWithVector(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "rank") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            Model model) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))
            
            def results = searchService.searchWithVectorStore(query, pageRequest)
            
            model.addAttribute("query", query)
            model.addAttribute("searchResults", results)
            model.addAttribute("currentPage", page)
            model.addAttribute("pageSize", size)
            model.addAttribute("totalPages", results.totalPages)
            model.addAttribute("totalItems", results.totalElements)
            model.addAttribute("sortBy", sortBy)
            model.addAttribute("sortDir", sortDir)
            model.addAttribute("searchMethod", "vector")
            
            return "search/fragments/search-results :: results"
        } catch (Exception e) {
            log.error("Error searching with Vector store: ${e.message}", e)
            model.addAttribute("error", "An error occurred while searching with Vector store: ${e.message}")
            return "search/fragments/search-results :: error"
        }
    }

    /**
     * HTMX endpoint for combined search results
     */
    @GetMapping("/all")
    String searchWithAllMethods(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortBy", defaultValue = "rank") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "desc") String sortDir,
            Model model) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))
            
            def allResults = searchService.searchWithAllMethods(query, pageRequest)
            
            model.addAttribute("query", query)
            model.addAttribute("postgresResults", allResults.postgres)
            model.addAttribute("solrResults", allResults.solr)
            model.addAttribute("vectorResults", allResults.vector)
            model.addAttribute("currentPage", page)
            model.addAttribute("pageSize", size)
            model.addAttribute("sortBy", sortBy)
            model.addAttribute("sortDir", sortDir)
            
            return "search/fragments/all-results :: results"
        } catch (Exception e) {
            log.error("Error searching with all methods: ${e.message}", e)
            model.addAttribute("error", "An error occurred while searching: ${e.message}")
            return "search/fragments/all-results :: error"
        }
    }
}
