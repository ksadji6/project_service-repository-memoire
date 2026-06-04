package com.esmt.projet.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor // <--- CORRIGÉ : Nécessaire pour injecter automatiquement le filtre
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter; // <--- CORRIGÉ : Injection du filtre

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
                        // Documentation swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/projects/v3/api-docs").permitAll()
                        // Seul le chef projet ou l'admin crée un projet
                        .requestMatchers("/api/projects/create").hasAnyRole("CHEF_PROJET", "ADMIN")
                        // Les autres routes concernant le projet
                        .requestMatchers("/api/projects/**").hasAnyRole("ADMIN", "PRESALES", "CHEF_PROJET", "INGENIEUR", "SUPERVISEUR")
                        // Les autres routes concernant les taches
                        .requestMatchers("/api/projects/tasks/**").hasAnyRole("ADMIN", "CHEF_PROJET", "INGENIEUR")
                        //.requestMatchers("/api/projects/tasks/**").hasAnyRole("ADMIN", "CHEF_PROJET", "INGENIEUR","SUPERVISEUR")
                        .requestMatchers("/api/projects/tasks/ingenieur/**").hasAnyRole( "ADMIN", "INGENIEUR")

                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}