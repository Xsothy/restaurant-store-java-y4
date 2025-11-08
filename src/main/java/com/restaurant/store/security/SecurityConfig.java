package com.restaurant.store.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Still needed for type reference

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    // NOTE: The JwtAuthenticationFilter field should be removed if it's no longer used.
    // If you keep it, it will be unused in the filter chain below.

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
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Enable CSRF (Good practice for session-based web apps)
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**").disable()) // Disable only if you need to access H2 console/APIs
                .cors(cors -> cors.configure(http))

                // 3. Define authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public access for common resources and pages
                        .requestMatchers("/", "/register", "/menu", "/products/**", "cart", "/css/**", "/js/**", "/images/**", "/*.html", "/static/**").permitAll()
                        .requestMatchers("/auth/login", "/auth/register").permitAll()

                        // Protected pages require authentication
                        .requestMatchers("/orders/**", "/profile").authenticated()

                        // Fallback: All other requests should be authenticated by default
                        .anyRequest().authenticated()
                )

                // 4. Enable standard FORM LOGIN
                .formLogin(form -> form
                        .loginPage("/auth/login") // Custom login page URL
                        .loginProcessingUrl("/auth/login") // Default POST endpoint for login
                        .defaultSuccessUrl("/menu", true) // Redirect after successful login
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )

                // 5. Enable standard LOGOUT
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/menu")
                        .invalidateHttpSession(true) // Invalidate the HTTP session
                        .deleteCookies("JSESSIONID") // Delete the session cookie
                        .permitAll()
                )

                // 6. Set custom authentication provider
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

//    @Bean
//    @Order(2)
//    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
//        http
//            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**").disable()) // Disable only if you need to access H2 console/APIs
//            .cors(cors -> cors.configure(http))
//            .authorizeHttpRequests(auth -> auth
//                .requestMatchers("/api/auth/**").permitAll()
//                .requestMatchers("/api/categories/**", "/api/products/**").permitAll()
//                .requestMatchers("/api/orders/**").authenticated()
//                .requestMatchers("/api/deliveries/**").authenticated()
//                .requestMatchers("/api/customers/**").authenticated()
//                .requestMatchers("/api/**").authenticated()
//            )
//            .sessionManagement(session -> session
//                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
//            )
//            .authenticationProvider(authenticationProvider())
//            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }
}