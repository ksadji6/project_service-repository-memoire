package com.esmt.projet.config;

import org.springframework.cloud.util.ConditionalOnBootstrapEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        //seul le chef projet ou l'admin crée un projet
                        .requestMatchers("/api/projects/create").hasAnyRole("CHEF_PROJET", "ADMIN")

                        .requestMatchers("/api/projects/**").hasAnyRole("ADMIN", "PRESALES", "CHEF_PROJET", "INGENIEUR", "SUPERVISEUR")

                        .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "CHEF_PROJET", "INGENIEUR")

                        .anyRequest().authenticated()
                );

        return http.build();
    }


}
