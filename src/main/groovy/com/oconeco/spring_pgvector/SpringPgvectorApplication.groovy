package com.oconeco.spring_pgvector

import groovy.util.logging.Slf4j
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.BaseHttpSolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation
import org.apache.solr.client.solrj.response.schema.SchemaResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@Slf4j
@SpringBootApplication
class SpringPgvectorApplication {

    @Value('${solr.host:http://localhost:8983/solr}')
    String solrUrl

    @Value('${solr.collection:replaceMeDefault}')
    String collectionName

    static void main(String[] args) {
        SpringApplication.run(SpringPgvectorApplication, args)
    }

//    @Bean
//    public SolrClient solrClient() {
//        return new HttpSolrClient.Builder(solrUrl).build()
//    }

    @Bean
    public CommandLineRunner runner(SolrClient solrClient) {
        return { args ->
            log.info("Checking and initializing Solr collection and schema...")

            try {
                // Check if collection exists
                List<String> collections = CollectionAdminRequest.listCollections(solrClient)
                if (!collections.contains(collectionName)) {
                    log.info("Collection '${collectionName}' does not exist. Creating it...")
                    // Create collection
                    def createRequest = CollectionAdminRequest.createCollection(collectionName, 1, 1)
                    def createResponse = createRequest.process(solrClient)

                    if (createResponse.isSuccess()) {
                        log.info("Successfully created '${collectionName}' collection")
                        // Wait for collection to be ready
                        Thread.sleep(2000)
                        // Add field types
                        addFieldTypes(solrClient, collectionName)

                    } else {
                        log.error("Failed to create '${collectionName}' collection: ${createResponse}")
                    }
                } else {
                    log.info("Collection '${collectionName}' already exists")
                    // Check if field types exist and add if they don't
                    def fieldTypesExist = checkFieldTypesExist(solrClient, collectionName)
                    if (!fieldTypesExist) {
                        addFieldTypes(solrClient, collectionName)
                    }
                }
            } catch (Exception e) {
                log.error("Error initializing Solr collection and schema", e)
            }
            // No need to close the solrClient here as Spring manages its lifecycle
        }
    }

    boolean checkFieldTypesExist(SolrClient solrClient, String collectionName) {
        try {
            def request = new SchemaRequest.FieldTypes()
            request.setPath("/schema/fieldtypes")
            def response = request.process(solrClient, collectionName)

            def fieldTypes = response.getFieldTypes()
            def fieldTypeNames = fieldTypes.collect { FieldTypeRepresentation ftr ->
                (String) ftr.getAttributes().get("name");
            }

            return fieldTypeNames.contains("text_shingle") && fieldTypeNames.contains("text_sayt")
        } catch (Exception e) {
            log.error("Error checking field types", e)
            return false
        }
    }

    def addFieldTypes(SolrClient solrClient, String collectionName) {
        log.info("Adding field types 'text_shingle' and 'text_sayt'...")

        try {
            // Add text_shingle field type
            // Define attributes for the new field type
            Map<String, Object> shingleFieldTypeAttr = new HashMap<>();
            shingleFieldTypeAttr.put("name", "text_shingle");
            shingleFieldTypeAttr.put("class", "solr.TextField");
            shingleFieldTypeAttr.put("positionIncrementGap", "100");

            // Define an analyzer for the field type
            Map<String, Object> shingleAnalyzer = new HashMap<>();
            shingleAnalyzer.put("tokenizer", Map.of("class", "solr.StandardTokenizerFactory"));
            shingleAnalyzer.put("filters", List.of(
                    Map.of("class", "solr.LowerCaseFilterFactory"),
                    Map.of("class", "solr.ShingleFilterFactory", "minShingleSize", "2", "maxShingleSize", "5", "outputUnigrams", "true")
            ));
            shingleFieldTypeAttr.put("analyzer", shingleAnalyzer);

            FieldTypeDefinition shindleFieldType = new FieldTypeDefinition()
            shindleFieldType.setAttributes(shingleFieldTypeAttr)

            // Create and process the request to add the field type
            SchemaRequest.AddFieldType addFieldTypeRequest = new SchemaRequest.AddFieldType(shindleFieldType);
            SchemaResponse.UpdateResponse response = addFieldTypeRequest.process(solrClient, collectionName);
            if (response.getStatus() == 0) {
                log.info("Successfully added 'text_shingle' field type")
            } else {
                log.error("Failed to add 'text_shingle' field type: ${response}")
            }
        } catch (BaseHttpSolrClient.RemoteExecutionException ree) {
            log.warn("Field type 'text_shingle' already exists?? $ree")

        } catch (Exception e) {
            log.error("Error adding 'text_shingle' field type", e)
        }


        // Add text_sayt field type (Search As You Type)
        try {
            Map<String, Object> saytAnalyzer = [tokenizer: [class: 'solr.StandardTokenizerFactory'],
                                                filters  : [[class: 'solr.LowerCaseFilterFactory'],
                                                            [class: 'solr.EdgeNGramFilterFactory', minGramSize: '3', maxGramSize: '10']]]

            Map<String, Object> saytFieldTypeAttr = [name: 'text_sayt', class: 'solr.TextField', positionIncrementGap: 100, analyzer: saytAnalyzer]
            // Define an analyzer for the field type
//            saytFieldTypeAttr.put("analyzer", saytAnalyzer)
            FieldTypeDefinition saytFieldType = new FieldTypeDefinition()
            saytFieldType.setAttributes(saytFieldTypeAttr)

            // Create and process the request to add the field type
            SchemaRequest.AddFieldType addFieldTypeRequest = new SchemaRequest.AddFieldType(saytFieldType);
            SchemaResponse.UpdateResponse response = addFieldTypeRequest.process(solrClient, collectionName);
            if (response.getStatus() == 0) {
                log.info("Successfully added 'text_sayt' field type")
            } else {
                log.error("Failed to add 'text_sayt' field type: ${response}")
            }

            // Add text_shingle field type
        } catch (Exception e) {
            log.error("Error adding field types", e)
        }


    }
}

/*
    curl -X POST -H 'Content-type:application/json' http://localhost:8983/solr/contracting/schema -d '

    String solrUrl = "http://localhost:8983/solr/your_core";
            try (SolrClient solrClient = new HttpSolrClient.Builder(solrUrl).build()) {

                // Define attributes for the new field type
                Map<String, Object> fieldTypeAttributes = new HashMap<>();
                fieldTypeAttributes.put("name", "text_custom");
                fieldTypeAttributes.put("class", "solr.TextField");
                fieldTypeAttributes.put("positionIncrementGap", "100");

                // Define an analyzer for the field type
                Map<String, Object> analyzer = new HashMap<>();
                analyzer.put("tokenizer", Map.of("class", "solr.StandardTokenizerFactory"));
                analyzer.put("filters", List.of(
                        Map.of("class", "solr.LowerCaseFilterFactory"),
                        Map.of("class", "solr.StopFilterFactory", "words", "stopwords.txt", "ignoreCase", "true")
                ));

                fieldTypeAttributes.put("analyzer", analyzer);

                // Create and process the request to add the field type
                SchemaRequest.AddFieldType addFieldTypeRequest = new SchemaRequest.AddFieldType(fieldTypeAttributes);
                SchemaResponse.UpdateResponse response = addFieldTypeRequest.process(solrClient);

                // Print the response from Solr
                System.out.println("Response: " + response.getResponse());

 */
