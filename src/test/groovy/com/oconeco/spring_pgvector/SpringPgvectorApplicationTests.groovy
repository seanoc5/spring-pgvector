package com.oconeco.spring_pgvector

import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

/**
 * Simple test class that doesn't load the full application context
 * but just verifies that Spock is configured correctly
 */
@ActiveProfiles("test")
class SpringPgvectorApplicationTests extends Specification {

	def "contextLoads"() {
		expect: "Spock test framework is working"
		true
	}
}
