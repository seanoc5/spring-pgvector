package com.oconeco.spring_pgvector.config

import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification
import spock.mock.DetachedMockFactory

@ActiveProfiles("test")
class SolrConfigurationSpec extends Specification {

    def "should mock Solr client for testing"() {
        given:
        def mockFactory = new DetachedMockFactory()
        def mockSolrClient = mockFactory.Mock(HttpSolrClient)

        expect: "mock can be created for testing"
        mockSolrClient != null
    }

    @TestConfiguration
    static class TestSolrConfig {
        @Bean
        @Primary
        SolrClient mockSolrClient(@Value('${solr.host}') String solrHost) {
            def mockFactory = new DetachedMockFactory()
            return mockFactory.Mock(HttpSolrClient)
        }
    }
}
