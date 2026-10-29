package edu.sjsu.cmpe172.tutoring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        .csrf(csrf -> {csrf.disable();})
        .sessionManagement(session -> {
            session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
        })
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/error", "/login").permitAll()
            .requestMatchers("/api/slots", "/api/auth/**", "/api/customer/**", "/api/provider/**").permitAll()
            .requestMatchers("/slots", "/slots/**", "/book", "/confirmation", "/confirmation/**").permitAll()
            .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
            .anyRequest().authenticated()
        );
        
        return http.build();
    }
}
