package com.oconeco.spring_pgvector.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import javax.sql.DataSource

@Configuration
@EnableWebSecurity
class SecurityConfig {
    private final DataSource dataSource

    SecurityConfig(DataSource dataSource) {
        this.dataSource = dataSource
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder()
    }

    @Bean
    PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl()
        tokenRepository.setDataSource(dataSource)
        tokenRepository.setCreateTableOnStartup(true)  // This will create the remember-me table
        return tokenRepository
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // Disable CSRF for simplicity in development
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/embedding/**").permitAll()  // Allow all /embedding/** endpoints
                .requestMatchers("/auth/login", "/css/**", "/js/**").permitAll()  // Allow login page and static resources
                .anyRequest().authenticated()  // Require auth for other endpoints
            )
            .formLogin()
                .loginPage("/auth/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            .and()
            .logout()
                .logoutSuccessUrl("/auth/login?logout")
                .permitAll()
            .and()
            .rememberMe()
                .tokenRepository(persistentTokenRepository())
                .tokenValiditySeconds(86400) // 24 hours
                .key("spring-pgvector-remember-me-key")

        return http.build()
    }
}
