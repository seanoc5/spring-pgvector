package com.oconeco.spring_pgvector.controller


import com.oconeco.spring_pgvector.domain.NaicsCode
import com.oconeco.spring_pgvector.service.NaicsCodeService
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
@RequestMapping("/api/naics-codes")
@Slf4j
class NaicsCodeController {

    @Autowired
    private NaicsCodeService naicsCodeService

    /**
     * Get all NAICS codes with pagination.
     */
    @GetMapping
    ResponseEntity<Page<NaicsCode>> getAllNaicsCodes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "code") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortBy))

        Page<NaicsCode> naicsCodes = naicsCodeService.getAllNaicsCodes(pageRequest)
        return ResponseEntity.ok(naicsCodes)
    }

    /**
     * Get a specific NAICS code by its code.
     */
    @GetMapping("/{code}")
    ResponseEntity<NaicsCode> getNaicsCodeByCode(@PathVariable String code) {
        NaicsCode naicsCode = naicsCodeService.getNaicsCodeByCode(code)

        if (naicsCode) {
            return ResponseEntity.ok(naicsCode)
        } else {
            return ResponseEntity.notFound().build()
        }
    }

    /**
     * Create a new NAICS code.
     */
    @PostMapping
    ResponseEntity<Object> createNaicsCode(@RequestBody NaicsCode naicsCode) {
        try {
            NaicsCode createdNaicsCode = naicsCodeService.createNaicsCode(naicsCode)
            return ResponseEntity.status(HttpStatus.CREATED).body(createdNaicsCode)
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
    ResponseEntity<Object> updateNaicsCode(@PathVariable String code, @RequestBody NaicsCode naicsCode) {
        try {
            NaicsCode updatedNaicsCode = naicsCodeService.updateNaicsCode(code, naicsCode)
            return ResponseEntity.ok(updatedNaicsCode)
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
    ResponseEntity<Object> deleteNaicsCode(@PathVariable String code) {
        try {
            naicsCodeService.deleteNaicsCode(code)
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
    ResponseEntity<Page<NaicsCode>> searchByTitle(
            @RequestParam(name = "title") String title,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size)
        Page<NaicsCode> results = naicsCodeService.searchByTitle(title, pageRequest)
        return ResponseEntity.ok(results)
    }

    /**
     * Search NAICS codes by description.
     */
    @GetMapping("/search/description")
    ResponseEntity<Page<NaicsCode>> searchByDescription(
            @RequestParam(name = "description") String description,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size)
        Page<NaicsCode> results = naicsCodeService.searchByDescription(description, pageRequest)
        return ResponseEntity.ok(results)
    }

    /**
     * Search NAICS codes by sector code.
     */
    @GetMapping("/search/sector")
    ResponseEntity<Page<NaicsCode>> searchBySectorCode(
            @RequestParam(name = "sectorCode") String sectorCode,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size)
        Page<NaicsCode> results = naicsCodeService.searchBySectorCode(sectorCode, pageRequest)
        return ResponseEntity.ok(results)
    }

    /**
     * Search NAICS codes by active status.
     */
    @GetMapping("/search/active")
    ResponseEntity<Page<NaicsCode>> searchByActiveStatus(
            @RequestParam(name = "isActive") Boolean isActive,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size)
        Page<NaicsCode> results = naicsCodeService.searchByActiveStatus(isActive, pageRequest)
        return ResponseEntity.ok(results)
    }

    /**
     * Full-text search for NAICS codes.
     */
    @GetMapping("/search")
    ResponseEntity<Page<Map<String, Object>>> searchNaicsCodes(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        try {
            PageRequest pageRequest = PageRequest.of(page, size)
            Page<Map<String, Object>> results = naicsCodeService.searchNaicsCodes(query, pageRequest)
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
    ResponseEntity<Map<String, Object>> importFromCsv(@RequestParam(name = "file") MultipartFile file) {
        try {
            // Create a temporary file
            File tempFile = File.createTempFile("naics_codes", ".csv")
            file.transferTo(tempFile)

            // Import the data
            int count = naicsCodeService.importFromCsv(tempFile)

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
