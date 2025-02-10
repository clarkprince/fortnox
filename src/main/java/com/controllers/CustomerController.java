package com.controllers;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.repository.TenantRepository;
import com.services.fortnox.FnCustomers;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private FnCustomers fnCustomers;

    @GetMapping("/list")
    public ResponseEntity<String> getCustomerList(@RequestHeader("tenant") String tenantDomain, @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "0") int offset) {
        Optional<Tenant> tenant = tenantRepository.findBySynchroteamDomain(tenantDomain);
        String customers = fnCustomers.requestCustomers(tenant.get(), "2010-01-01 00:00:00 ", size, offset);
        return customers != null ? ResponseEntity.ok(customers) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> getCustomerDetails(@PathVariable String id, @RequestHeader("tenant") String tenantDomain) {
        Optional<Tenant> tenant = tenantRepository.findBySynchroteamDomain(tenantDomain);
        return tenant.isPresent() ? ResponseEntity.ok(fnCustomers.doGetSingleCustomer(tenant.get(), id)) : ResponseEntity.notFound().build();
    }
}
