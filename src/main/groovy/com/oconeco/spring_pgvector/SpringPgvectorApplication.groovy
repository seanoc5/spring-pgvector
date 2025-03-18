package com.oconeco.spring_pgvector

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
//@RestController
class SpringPgvectorApplication {

	static void main(String[] args) {
		SpringApplication.run(SpringPgvectorApplication, args)
	}

    @Bean
      public CommandLineRunner runner() {

    }

}
