package com.example.customermanagement.controller;

import com.example.customermanagement.dto.CustomerDTO;
import com.example.customermanagement.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    // -------------------------------------------------------
    // POST /api/customers
    // Create a new customer
    // @Valid triggers validation on CustomerDTO fields
    // -------------------------------------------------------
    @PostMapping
    public ResponseEntity<CustomerDTO> createCustomer(
            @Valid @RequestBody CustomerDTO dto) {
        log.info("POST /api/customers — NIC: {}", dto.getNicNumber());
        CustomerDTO created = customerService.createCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // -------------------------------------------------------
    // PUT /api/customers/{id}
    // Update an existing customer by ID
    // -------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDTO dto) {
        log.info("PUT /api/customers/{}", id);
        CustomerDTO updated = customerService.updateCustomer(id, dto);
        return ResponseEntity.ok(updated);
    }

    // -------------------------------------------------------
    // GET /api/customers/{id}
    // Get full detail of one customer including all relations
    // -------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Long id) {
        log.info("GET /api/customers/{}", id);
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    // -------------------------------------------------------
    // GET /api/customers?search=&page=0&size=20
    // Paginated list for table view with optional search
    // -------------------------------------------------------
    @GetMapping
    public ResponseEntity<Page<CustomerDTO>> getAllCustomers(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/customers — search='{}' page={} size={}", search, page, size);
        return ResponseEntity.ok(customerService.getAllCustomers(search, page, size));
    }

    // -------------------------------------------------------
    // DELETE /api/customers/{id}
    // Delete a customer and all their related data (cascade)
    // -------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.info("DELETE /api/customers/{}", id);
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}