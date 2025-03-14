package com.oconeco.spring_pgvector

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import jakarta.persistence.EntityManager

@SpringBootTest
class SpringPgvectorApplicationTests {

	@Autowired
	private EntityManager entityManager

	@Test
	void contextLoads() {
		// Basic test to ensure the application context loads successfully
		assert entityManager != null
	}

}
