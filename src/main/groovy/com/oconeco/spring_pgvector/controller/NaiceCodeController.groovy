package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.domain.NaiceCode
import com.oconeco.spring_pgvector.service.NaiceCodeService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * REST controller for managing US Census NAICS codes.
 */
@RestController
@RequestMapping("/api/naice-codes")
@Slf4j
class NaiceCodeController {

    @Autowired
    private NaiceCodeService naiceCodeService

    /**
     * Get all NAICS codes with pagination.
     */
    @GetMapping
    ResponseEntity<Page<NaiceCode>> getAllNaiceCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))

        Page<NaiceCode> naiceCodes = naiceCodeService.getAllNaiceCodes(pageRequest)
        return ResponseEntity.ok(naiceCodes)
    }

    /**
     * Get a specific NAICS code by its code.
     */
    @GetMapping("/{code}")
    ResponseEntity<NaiceCode> getNaiceCodeByCode(@PathVariable String code) {
        NaiceCode naiceCode = naiceCodeService.getNaiceCodeByCode(code)

        if (naiceCode) {
            return ResponseEntity.ok(naiceCode)
        } else {
            return ResponseEntity.notFound().build()
        }
    }

    /**
     * Create a new NAICS code.
     */
    @PostMapping
    ResponseEntity<Object> createNaiceCode(@RequestBody NaiceCode naiceCode) {
        try {
            NaiceCode createdNaiceCode = naiceCodeService.createNaiceCode(naiceCode)
            return ResponseEntity.status(HttpStatus.CREATED).body(createdNaiceCode)
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body([
                status: "error",
                message: e.message
            ])
        } catch (Exception e) {
            log.error "Error creating NAICS code: ${e.message}", e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body([
                status: "error",
                message: "Error creating NAICS code: ${e.message}"
            ])
        }
    }

    /**
     * Update an existing NAICS code.
     */
    @PutMapping("/{code}")
    ResponseEntity<Object> updateNaiceCode(@PathVariable String code, @RequestBody NaiceCode naiceCode) {
        try {
            NaiceCode updatedNaiceCode = naiceCodeService.updateNaiceCode(code, naiceCode)
            return ResponseEntity.ok(updatedNaiceCode)
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body([
                status: "error",
                message: e.message
            ])
        } catch (Exception e) {
            log.error "Error updating NAICS code: ${e.message}", e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body([
                status: "error",
                message: "Error updating NAICS code: ${e.message}"
            ])
        }
    }

    /**
     * Delete a NAICS code.
     */
    @DeleteMapping("/{code}")
    ResponseEntity<Object> deleteNaiceCode(@PathVariable String code) {
        try {
            naiceCodeService.deleteNaiceCode(code)
            return ResponseEntity.noContent().build()
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body([
                status: "error",
                message: e.message
            ])
        } catch (Exception e) {
            log.error "Error deleting NAICS code: ${e.message}", e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body([
                status: "error",
                message: "Error deleting NAICS code: ${e.message}"
            ])
        }
    }

    /**
     * Search NAICS codes by title.
     */
    @GetMapping("/search/title")
    ResponseEntity<Page<NaiceCode>> searchByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size)
        Page<NaiceCode> results = naiceCodeService.searchByTitle(title, pageRequest)
        return ResponseEntity.ok(results)
    }

    /**
     * Search NAICS codes by description.
     */
    @GetMapping("/search/description")
    ResponseEntity<Page<NaiceCode>> searchByDescription(
            @RequestParam String description,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size)
        Page<NaiceCode> results = naiceCodeService.searchByDescription(description, pageRequest)
        return ResponseEntity.ok(results)
    }

    /**
     * Search NAICS codes by sector code.
     */
    @GetMapping("/search/sector")
    ResponseEntity<Page<NaiceCode>> searchBySectorCode(
            @RequestParam String sectorCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size)
        Page<NaiceCode> results = naiceCodeService.searchBySectorCode(sectorCode, pageRequest)
        return ResponseEntity.ok(results)
    }

    /**
     * Search NAICS codes by active status.
     */
    @GetMapping("/search/active")
    ResponseEntity<Page<NaiceCode>> searchByActiveStatus(
            @RequestParam Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size)
        Page<NaiceCode> results = naiceCodeService.searchByActiveStatus(isActive, pageRequest)
        return ResponseEntity.ok(results)
    }

    /**
     * Full-text search for NAICS codes.
     */
    @GetMapping("/search")
    ResponseEntity<Page<Map<String, Object>>> searchNaiceCodes(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            PageRequest pageRequest = PageRequest.of(page, size)
            Page<Map<String, Object>> results = naiceCodeService.searchNaiceCodes(query, pageRequest)
            return ResponseEntity.ok(results)
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null)
        } catch (Exception e) {
            log.error "Error searching NAICS codes: ${e.message}", e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }

    /**
     * Import NAICS codes from a CSV file.
     */
    @PostMapping("/import")
    ResponseEntity<Map<String, Object>> importFromCsv(@RequestParam("file") MultipartFile file) {
        try {
            // Create a temporary file
            File tempFile = File.createTempFile("naice_codes", ".csv")
            file.transferTo(tempFile)

            // Import the data
            int count = naiceCodeService.importFromCsv(tempFile)

            // Clean up
            tempFile.delete()

            return ResponseEntity.ok([
                status: "success",
                message: "Successfully imported ${count} NAICS codes",
                count: count
            ])
        } catch (Exception e) {
            log.error "Error importing CSV: ${e.message}", e
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body([
                status: "error",
                message: "Error importing CSV: ${e.message}"
            ])
        }
    }
}
