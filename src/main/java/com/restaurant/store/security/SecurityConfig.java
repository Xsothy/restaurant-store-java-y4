package com.restaurant.store.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF for API endpoints, but might keep it for web if you use sessions
                .csrf(csrf -> csrf.disable()) // Quick fix, but consider enabling for web forms
                .cors(cors -> cors.configure(http))

                // 2. Define Authorization Rules
                .authorizeHttpRequests(auth -> auth
                        // Web/Static paths allowed for everyone
                        .requestMatchers("/", "/login", "/register", "/menu", "/products/**", "/product-details", "/cart").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/*.html", "/static/**").permitAll()
                        // API paths allowed for everyone (e.g., login/register)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/products/**", "/api/categories/**").permitAll()
                        // Internal API and webhooks (for Admin backend and Stripe)
                        .requestMatchers("/api/internal/**", "/api/webhooks/**", "/api/sync/**").permitAll()
                        // WebSocket endpoint
                        .requestMatchers("/ws/**").permitAll()

                        // SECURE WEB PATHS: Require authentication
                        .requestMatchers("/orders", "/profile").authenticated() // Now accessible via session/form login
                        // SECURE API PATHS: Require authentication
                        .requestMatchers("/api/orders/**", "/api/deliveries/**", "/api/customers/**").authenticated()

                        // Default rule
                        .anyRequest().authenticated()
                )

                // 3. Configure Form-Based Login (For Web UI)
                .formLogin(form -> form
                        .loginPage("/login")        // Your custom login page
                        .loginProcessingUrl("/login") // Endpoint where form POSTs data
                        .defaultSuccessUrl("/orders", true) // Redirect after successful login
                        .permitAll()
                )

                // 4. Re-enable Session Management (For Web UI)
                // Since you have web paths that need session-based login, you should remove the STATELESS policy
                // or configure it to be conditionally STATELESS/ALWAYS. A simpler fix is removing the STATELESS policy
                // or setting it to IF_REQUIRED if you use the same chain for both.

                // --- RECOMMENDED CHANGE ---
                // If you must keep the JWT for API, separate your chains (complex)
                // OR try setting policy to IF_REQUIRED and remove the stateless setting for simplicity.

                // If you absolutely need a single chain and want form login for /orders:
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // Allows sessions when needed
                )
                // -------------------------

                // 5. JWT Filter Placement (Handles /api/* requests)
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // Still runs for all requests

        return http.build();
    }
}
