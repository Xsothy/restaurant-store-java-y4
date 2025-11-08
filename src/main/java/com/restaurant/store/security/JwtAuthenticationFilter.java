package com.restaurant.store.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.dto.response.ErrorResponse;
import com.restaurant.store.exception.JwtAuthenticationException;
import com.restaurant.store.exception.JwtExpiredException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (ExpiredJwtException e) {
                log.error("JWT token has expired: {}", e.getMessage());
                handleException(response, HttpStatus.UNAUTHORIZED, "JWT token has expired. Please login again.", request.getRequestURI());
                return;
            } catch (MalformedJwtException e) {
                log.error("Invalid JWT token format: {}", e.getMessage());
                handleException(response, HttpStatus.UNAUTHORIZED, "Invalid JWT token format. Please login again.", request.getRequestURI());
                return;
            } catch (SignatureException e) {
                log.error("JWT signature validation failed: {}", e.getMessage());
                handleException(response, HttpStatus.UNAUTHORIZED, "Invalid JWT token signature. Please login again.", request.getRequestURI());
                return;
            } catch (Exception e) {
                log.error("JWT token processing failed: {}", e.getMessage(), e);
                handleException(response, HttpStatus.UNAUTHORIZED, "Invalid JWT token. Please login again.", request.getRequestURI());
                return;
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                } else {
                    log.error("JWT token validation failed for user: {}", username);
                }
            } catch (Exception e) {
                log.error("Error loading user details: {}", e.getMessage(), e);
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private void handleException(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        ErrorResponse errorResponse = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
