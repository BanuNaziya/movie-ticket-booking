package com.moviebooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // WHY disable CSRF?
            // CSRF protection is needed when the browser automatically sends cookies
            // with every request (session-based auth). Since we use JWT in the
            // Authorization header (not cookies), the browser never sends it
            // automatically — so CSRF attacks are not possible here.
            .csrf(csrf -> csrf.disable())

            // WHY STATELESS session?
            // In a traditional app, Spring Security stores the authenticated user
            // in the HTTP session. We don't want that — JWT carries all identity
            // information in the token itself. STATELESS tells Spring to never
            // create or use an HTTP session, making the API truly stateless and
            // horizontally scalable (any server can handle any request).
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // /api/auth/** is public — anyone can register or login without a token
                .requestMatchers("/api/auth/**").permitAll()
                // All other endpoints require a valid JWT
                .anyRequest().authenticated()
            )

            // WHY addFilterBefore?
            // Our JwtAuthFilter must run BEFORE Spring's built-in
            // UsernamePasswordAuthenticationFilter so that by the time Spring
            // checks "is this request authenticated?", we've already extracted
            // the user identity from the JWT and placed it in the SecurityContext.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // WHY BCrypt?
        // BCrypt is a slow hashing algorithm by design — it's computationally
        // expensive to compute, which makes brute-force attacks impractical.
        // Unlike MD5/SHA which are fast, BCrypt adds a "work factor" (cost parameter)
        // and a random salt per password, so two identical passwords produce
        // different hashes and rainbow table attacks are not possible.
        return new BCryptPasswordEncoder();
    }
}
