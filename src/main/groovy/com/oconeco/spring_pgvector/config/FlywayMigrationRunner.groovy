package com.oconeco.spring_pgvector.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

import javax.sql.DataSource

/**
 * Configuration to run Flyway migrations manually with a clean option
 */
@Configuration
class FlywayMigrationRunner {

    @Autowired
    DataSource dataSource

    /**
     * Bean to clean and migrate the database when the 'flyway-clean' profile is active
     */
    @Bean
    @Profile("flyway-clean")
    CommandLineRunner flywayCleanMigrate() {
        return { args ->
            println "Running Flyway clean and migrate..."
            
            // Create a new Flyway instance
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .cleanDisabled(false) // Ensure clean is enabled
                .load()
                
            try {
                // Clean the database first (removes all tables)
                flyway.clean()
                println "Database cleaned successfully"
                
                // Run migrations
                int migrationsApplied = flyway.migrate()
                println "Applied $migrationsApplied migrations successfully"
            } catch (Exception e) {
                println "Error during Flyway operation: ${e.message}"
                e.printStackTrace()
            }
        }
    }
}
