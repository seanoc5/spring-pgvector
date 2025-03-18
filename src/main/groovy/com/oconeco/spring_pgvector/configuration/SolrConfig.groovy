package com.oconeco.spring_pgvector.configuration

import groovy.util.logging.Slf4j
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SolrConfig {

    @Bean
    public SolrClient solrClient(SolrProperties solrProperties) {
        String collection = 'solr_system'
        String baseUrl = "http://${solrProperties.host}:${solrProperties.port}/solr/${solrProperties.collection}"
        SolrClient client = new HttpSolrClient.Builder(baseUrl).build()
        log.info("Built Solr Client: $client")
        return client

        // problem with http2 client, jetty something...?
        // For a standalone Solr instance
        // return new HttpSolrClient.Builder(solrProperties.getHost() + "/" + solrProperties.getCollection()).build();
    }
}
