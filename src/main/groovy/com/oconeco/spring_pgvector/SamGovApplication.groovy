package com.oconeco.spring_pgvector

import groovy.util.logging.Slf4j
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties

/**
 * Main application class for the SAM.gov Contract Opportunities application.
 * This application provides functionality to import, search, and view SAM.gov contract opportunities.
 */
@SpringBootApplication
@EnableConfigurationProperties
@Slf4j
class SamGovApplication {

    static void main(String[] args) {
        SpringApplication.run(SamGovApplication, args)
        log.info "SAM.gov Application started successfully"
    }
}
