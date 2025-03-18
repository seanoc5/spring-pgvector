package com.oconeco.spring_pgvector.configuration

import groovy.util.logging.Slf4j
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConfigurationProperties(prefix = "solr")
public class SolrProperties {
    String host;
    String collection;
    Integer port
    // getters and setters
}
