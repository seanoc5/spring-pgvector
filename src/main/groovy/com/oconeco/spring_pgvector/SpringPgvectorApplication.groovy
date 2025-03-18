package com.oconeco.spring_pgvector

import groovy.util.logging.Slf4j
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation
import org.apache.solr.common.util.NamedList
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Slf4j
@SpringBootApplication
class SpringPgvectorApplication {

    @Value('${solr.host:http://localhost:8983/solr}')
    String solrUrl

    static void main(String[] args) {
        SpringApplication.run(SpringPgvectorApplication, args)
    }

    @Bean
    public CommandLineRunner runner() {
        return { args ->
            log.info("Checking and initializing Solr collection and schema...")

            // Create SolrClient
            SolrClient solrClient = new HttpSolrClient.Builder(solrUrl).build()

            try {
                // Check if collection exists
                List<String> collections = CollectionAdminRequest.listCollections(solrClient)
//                def listResponse = listRequest.process(solrClient)
//                def collections = listResponse.getCollectionNames()

                if (!collections.contains("contracting")) {
                    log.info("Collection 'contracting' does not exist. Creating it...")

                    // Create collection
                    def createRequest = CollectionAdminRequest.createCollection("contracting", 1, 1)
                    def createResponse = createRequest.process(solrClient)

                    if (createResponse.isSuccess()) {
                        log.info("Successfully created 'contracting' collection")

                        // Wait for collection to be ready
                        Thread.sleep(2000)

                        // Add field types
                        addFieldTypes(solrClient)
                    } else {
                        log.error("Failed to create 'contracting' collection: ${createResponse}")
                    }
                } else {
                    log.info("Collection 'contracting' already exists")

                    // Check if field types exist and add if they don't
                    def fieldTypesExist = checkFieldTypesExist(solrClient)
                    if (!fieldTypesExist) {
                        addFieldTypes(solrClient)
                    }
                }
            } catch (Exception e) {
                log.error("Error initializing Solr collection and schema", e)
            } finally {
                solrClient.close()
            }
        }
    }

    boolean checkFieldTypesExist(SolrClient solrClient) {
        try {
            def request = new SchemaRequest.FieldTypes()
            request.setPath("/schema/fieldtypes")
            def response = request.process(solrClient, "contracting")

            def fieldTypes = response.getFieldTypes()
            def fieldTypeNames = fieldTypes.collect {FieldTypeRepresentation ftr ->
                (String) ftr.getAttributes().get("name");
            }

            return fieldTypeNames.contains("text_shingle") && fieldTypeNames.contains("text_sayt")
        } catch (Exception e) {
            log.error("Error checking field types", e)
            return false
        }
    }

    def addFieldTypes(SolrClient solrClient) {
        log.info("Adding field types 'text_shingle' and 'text_sayt'...")

        // Add text_shingle field type
        def shingleFieldType = [
            "add-field-type": [
                "name": "text_shingle",
                "class": "solr.TextField",
                "positionIncrementGap": "100",
                "analyzer": [
                    "tokenizer": [
                        "class": "solr.StandardTokenizerFactory"
                    ],
                    "filters": [
                        ["class": "solr.LowerCaseFilterFactory"],
                        ["class": "solr.ShingleFilterFactory", "minShingleSize": "2", "maxShingleSize": "3", "outputUnigrams": "true"]
                    ]
                ]
            ]
        ]

        // Add text_sayt field type (Search As You Type)
        def saytFieldType = [
            "add-field-type": [
                "name": "text_sayt",
                "class": "solr.TextField",
                "positionIncrementGap": "100",
                "analyzer": [
                    "tokenizer": [
                        "class": "solr.StandardTokenizerFactory"
                    ],
                    "filters": [
                        ["class": "solr.LowerCaseFilterFactory"],
                        ["class": "solr.EdgeNGramFilterFactory", "minGramSize": "1", "maxGramSize": "20"]
                    ]
                ]
            ]
        ]

        try {
            // Add text_shingle field type
            def shingleRequest = new SchemaRequest(shingleFieldType)
            def shingleResponse = shingleRequest.process(solrClient, "contracting")

            if (shingleResponse.getStatus() == 0) {
                log.info("Successfully added 'text_shingle' field type")
            } else {
                log.error("Failed to add 'text_shingle' field type: ${shingleResponse}")
            }

            // Add text_sayt field type
            def saytRequest = new SchemaRequest(saytFieldType)
            def saytResponse = saytRequest.process(solrClient, "contracting")

            if (saytResponse.getStatus() == 0) {
                log.info("Successfully added 'text_sayt' field type")
            } else {
                log.error("Failed to add 'text_sayt' field type: ${saytResponse}")
            }
        } catch (Exception e) {
            log.error("Error adding field types", e)
        }
    }
}
