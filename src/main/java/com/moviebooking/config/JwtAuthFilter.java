package com.moviebooking.config;

import com.moviebooking.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

// WHY extend OncePerRequestFilter?
// Spring's filter chain can invoke a filter multiple times per request in some
// scenarios (e.g., forward dispatches). OncePerRequestFilter guarantees this
// filter runs exactly once per HTTP request, preventing duplicate authentication.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // WHY check for "Bearer " prefix?
        // The HTTP Authorization header standard for token-based auth uses the
        // format: "Bearer <token>". We verify the prefix to ensure we're handling
        // the right auth scheme (not Basic auth or others).
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Strip the "Bearer " prefix (7 characters) to get the raw JWT string
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                Claims claims = jwtUtil.validateToken(token);
                String email = claims.getSubject();

                // userId was embedded into the token at login time — no DB lookup needed.
                // WHY store userId in the token? So downstream code (controllers)
                // can identify which user made the request without an extra DB query.
                Long userId = claims.get("userId", Long.class);

                // Create an Authentication object and store it in the SecurityContext.
                // WHY SecurityContextHolder? Spring Security checks this context on
                // every request to determine if the user is authenticated. Placing our
                // auth object here tells Spring "this request is authenticated".
                // - principal = email (who the user is)
                // - credentials = userId (used by controllers to scope DB queries)
                // - authorities = empty list (no role-based permissions in this system)
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, userId, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
            // If token is invalid, we simply don't set authentication.
            // Spring Security will then reject the request with 401 Unauthorized
            // at the authorization check stage — no need to explicitly return an error here.
        }

        // Always continue the filter chain — letting Spring Security handle
        // authorization. If no authentication was set above, protected endpoints
        // will return 401 automatically.
        filterChain.doFilter(request, response);
    }
}
