package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.service.NaicsCodeService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes

/**
 * Web controller for NAICS codes.
 * Provides web pages for viewing and searching NAICS codes.
 */
@Controller
@RequestMapping("/naics-codes")
@Slf4j
class NaicsCodeWebController {

    @Autowired
    private NaicsCodeService naicsCodeService

    /**
     * Display the NAICS codes list page.
     */
    @GetMapping(["", "/", '/index', '/list'])
    String listNaicsCodes(
            Model model,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "level", required = false) Integer level,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "code") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))

        Page<NaicsCode> naicsCodes

        // If level filter is applied
        if (level != null) {
            naicsCodes = naicsCodeService.findByLevel(level, pageRequest)
            model.addAttribute("activeLevel", level)
        } else {
            model.addAttribute("activeLevel", null)
        }

        // If search query is applied
        if (query && !query.trim().isEmpty()) {
            log.info("Listing NAICS codes for query: {}", query)
            try {
                Page<Map<String, Object>> searchResults = naicsCodeService.searchNaicsCodes(query, pageRequest)

                // Extract just the NaicsCode objects for display
                List<NaicsCode> naicsCodesList = searchResults.content.collect { it.naicsCode }
                naicsCodes = new org.springframework.data.domain.PageImpl<>(
                    naicsCodesList,
                    pageRequest,
                    searchResults.totalElements
                )

                model.addAttribute("query", query)
                model.addAttribute("searchResults", searchResults)
            } catch (Exception e) {
                log.error "Error searching NAICS codes: ${e.message}", e
                model.addAttribute("error", "An error occurred while searching: ${e.message}")
                naicsCodes = naicsCodeService.getAllNaicsCodes(pageRequest)
            }
        } else if (level == null) {
            // If no filters applied
            log.info("No filters applied, listing all NAICS codes")
            naicsCodes = naicsCodeService.getAllNaicsCodes(pageRequest)
        }

        model.addAttribute("naicsCodes", naicsCodes)
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", naicsCodes.totalPages)
        model.addAttribute("totalItems", naicsCodes.totalElements)
        model.addAttribute("sortBy", sortBy)
        model.addAttribute("sortDir", sortDir)
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc")

        return "naics-codes/list"
    }

    /**
     * Display a specific NAICS code.
     */
    @GetMapping("/{code}")
    String viewNaicsCode(@PathVariable(name="code") String code, Model model) {
        NaicsCode naicsCode = naicsCodeService.getNaicsCodeByCode(code)

        if (naicsCode) {
            model.addAttribute("naicsCode", naicsCode)
            return "naics-codes/view"
        } else {
            return "redirect:/naics-codes"
        }
    }

    /**
     * Handle HTMX search requests for search-as-you-type functionality.
     */
    @GetMapping("/search")
    String searchNaicsCodes(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "level", required = false) Integer level,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Model model) {

        try {
            PageRequest pageRequest = PageRequest.of(page, size)
            Page<Map<String, Object>> results

            // Apply level filter if provided
            if (level != null) {
                // First get results by search query
                results = naicsCodeService.searchNaicsCodes(query, pageRequest)

                // Then filter by level
                List<Map<String, Object>> filteredResults = results.content.findAll {
                    it.naicsCode.level == level
                }

                // Create a new page with filtered results
                results = new org.springframework.data.domain.PageImpl<>(filteredResults, pageRequest, filteredResults.size())

                model.addAttribute("activeLevel", level)

            } else {
                results = naicsCodeService.searchNaicsCodes(query, pageRequest)
                model.addAttribute("activeLevel", null)
            }

            model.addAttribute("query", query)
            model.addAttribute("searchResults", results)
            model.addAttribute("currentPage", page)
            model.addAttribute("totalPages", results.totalPages)
            model.addAttribute("totalItems", results.totalElements)

            return "naics-codes/fragments/search-results :: results"
        } catch (Exception e) {
            log.error "Error searching NAICS codes: ${e.message}", e
            model.addAttribute("error", "An error occurred while searching: ${e.message}")
            return "naics-codes/fragments/search-results :: error"
        }
    }

    /**
     * Handle HTMX level filter requests.
     */
    @GetMapping("/filter")
    String filterByLevel(
            @RequestParam(name = "level", required = false) Integer level,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "code") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir,
            Model model) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))

        Page<NaicsCode> naicsCodes

        // Apply both filters if both are present
        if (query && !query.trim().isEmpty() && level != null) {
            try {
                Page<Map<String, Object>> searchResults = naicsCodeService.searchNaicsCodes(query, pageRequest)

                // Filter by level
                List<Map<String, Object>> filteredResults = searchResults.content.findAll {
                    it.naicsCode.level == level
                }

                // Extract just the NaicsCode objects for display
                List<NaicsCode> naicsCodesList = filteredResults.collect { it.naicsCode }
                naicsCodes = new org.springframework.data.domain.PageImpl<>(
                    naicsCodesList,
                    pageRequest,
                    filteredResults.size()
                )

                model.addAttribute("query", query)
                model.addAttribute("searchResults", new org.springframework.data.domain.PageImpl<>(
                    filteredResults,
                    pageRequest,
                    filteredResults.size()
                ))
            } catch (Exception e) {
                log.error "Error searching NAICS codes: ${e.message}", e
                model.addAttribute("error", "An error occurred while searching: ${e.message}")
                naicsCodes = level != null ? naicsCodeService.findByLevel(level, pageRequest) : naicsCodeService.getAllNaicsCodes(pageRequest)
            }
        } else if (level != null) {
            // Only level filter
            naicsCodes = naicsCodeService.findByLevel(level, pageRequest)
        } else if (query && !query.trim().isEmpty()) {
            // Only search query
            try {
                Page<Map<String, Object>> searchResults = naicsCodeService.searchNaicsCodes(query, pageRequest)

                // Extract just the NaicsCode objects for display
                List<NaicsCode> naicsCodesList = searchResults.content.collect { it.naicsCode }
                naicsCodes = new org.springframework.data.domain.PageImpl<>(
                    naicsCodesList,
                    pageRequest,
                    searchResults.totalElements
                )

                model.addAttribute("query", query)
                model.addAttribute("searchResults", searchResults)
            } catch (Exception e) {
                log.error "Error searching NAICS codes: ${e.message}", e
                model.addAttribute("error", "An error occurred while searching: ${e.message}")
                naicsCodes = naicsCodeService.getAllNaicsCodes(pageRequest)
            }
        } else {
            // No filters
            naicsCodes = naicsCodeService.getAllNaicsCodes(pageRequest)
        }

        model.addAttribute("activeLevel", level)
        model.addAttribute("naicsCodes", naicsCodes)
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", naicsCodes.totalPages)
        model.addAttribute("totalItems", naicsCodes.totalElements)
        model.addAttribute("sortBy", sortBy)
        model.addAttribute("sortDir", sortDir)
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc")

        return "naics-codes/fragments/naics-table :: table"
    }

    /**
     * Display the file upload page for importing NAICS codes.
     */
    @GetMapping("/upload")
    String showUploadForm(Model model) {
        return "naics-codes/upload"
    }

    /**
     * Handle file upload for importing NAICS codes.
     */
    @PostMapping("/upload")
    String handleFileUpload(
            @RequestParam(name="file") MultipartFile file,
            @RequestParam(name = "clearExisting", required = false, defaultValue = "false") boolean clearExisting,
            // @RequestParam(name = "fileType", required = false) String fileType,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload")
            return "redirect:/naics-codes/upload"
        }

        try {
            // Create a temporary file to store the uploaded content
            String originalFilename = file.getOriginalFilename()
            File tempFile = File.createTempFile("naics-import-", getFileExtension(originalFilename))
            tempFile.deleteOnExit()
            file.transferTo(tempFile)

            int importedCount = 0

            // Check file type and process accordingly
            if (isExcelFile(originalFilename)) {
                importedCount = naicsCodeService.importFromExcel(tempFile, clearExisting)
            } else if (isCsvFile(originalFilename)) {
                importedCount = naicsCodeService.importFromCsv(tempFile, clearExisting)
            } else {
                redirectAttributes.addFlashAttribute("error", "Unsupported file format. Please upload an Excel (.xlsx) or CSV file.")
                return "redirect:/naics-codes/upload"
            }

            redirectAttributes.addFlashAttribute("success",
                "Successfully imported ${importedCount} NAICS codes" +
                (clearExisting ? " after clearing existing records." : "."))

            return "redirect:/naics-codes"

        } catch (Exception e) {
            log.error "Error importing NAICS codes: ${e.message}", e
            redirectAttributes.addFlashAttribute("error", "Failed to import NAICS codes: ${e.message}")
            return "redirect:/naics-codes/upload"
        }
    }

    /**
     * Check if the file is an Excel file based on its extension.
     */
    private boolean isExcelFile(String filename) {
        if (!filename) return false
        return filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")
    }

    /**
     * Check if the file is a CSV file based on its extension.
     */
    private boolean isCsvFile(String filename) {
        if (!filename) return false
        return filename.toLowerCase().endsWith(".csv")
    }

    /**
     * Get the file extension including the dot.
     */
    private String getFileExtension(String filename) {
        if (!filename) return ""
        int lastDotIndex = filename.lastIndexOf(".")
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : ""
    }
}
