package com.restaurant.store.service;

import com.restaurant.store.dto.request.CustomerUpdateRequest;
import com.restaurant.store.dto.response.CustomerResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.mapper.CustomerMapper;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.security.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AuthHelper authHelper;

    @Transactional
    public CustomerResponse updateCustomer(Long customerId, CustomerUpdateRequest request) {
        Customer currentCustomer = authHelper.user();

        if (!currentCustomer.getId().equals(customerId)) {
            throw new BadRequestException("You can only update your own profile");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (!customer.getEmail().equalsIgnoreCase(normalizedEmail) &&
                customerRepository.findByEmail(normalizedEmail)
                        .filter(existing -> !existing.getId().equals(customerId))
                        .isPresent()) {
            throw new BadRequestException("Email is already in use");
        }

        customer.setName(request.getName().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        customer.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);

        Customer saved = customerRepository.save(customer);
        log.info("Customer {} updated profile information", customerId);

        return customerMapper.toResponse(saved);
    }
}
