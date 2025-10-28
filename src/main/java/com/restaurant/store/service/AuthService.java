package com.restaurant.store.service;

import com.restaurant.store.dto.request.CustomerRegisterRequest;
import com.restaurant.store.dto.request.LoginRequest;
import com.restaurant.store.dto.response.AuthResponse;
import com.restaurant.store.dto.response.CustomerResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(CustomerRegisterRequest request) {
        // Check if email already exists
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Create new customer
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Save customer
        customer = customerRepository.save(customer);

        // Generate JWT token
        String token = jwtUtil.generateToken(customer.getEmail(), customer.getId());

        // Create customer response
        CustomerResponse customerResponse = mapToCustomerResponse(customer);

        // Expire token after 30 minutes
        Long expiresIn = (long) (30 * 60);

        return new AuthResponse(token, expiresIn, customerResponse);
    }

    public AuthResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Get customer details
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Customer not found"));

        // Generate JWT token
        String token = jwtUtil.generateToken(customer.getEmail(), customer.getId());

        // Create customer response
        CustomerResponse customerResponse = mapToCustomerResponse(customer);

        // Expire token after 30 minutes
        Long expiresIn = (long) (30 * 60);

        return new AuthResponse(token, expiresIn, customerResponse);
    }

    private CustomerResponse mapToCustomerResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setAddress(customer.getAddress());
        response.setCreatedAt(customer.getCreatedAt());
        return response;
    }
}
