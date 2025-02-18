package com.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.entities.Tenant;
import com.repository.TenantRepository;
import com.services.TenantService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private List<Tenant> cache = new ArrayList<>();

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantService tenantService;

    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAllOptimised();
        cache.addAll(tenants);
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenant(@PathVariable int id) {
        return tenantRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        Tenant savedTenant = tenantRepository.save(tenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTenant);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable int id, @RequestBody Tenant tenant) {
        return tenantRepository.findById(id).map(existingTenant -> {
            tenant.setId(id);
            Tenant updated = tenantRepository.save(tenant);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable int id) {
        return tenantRepository.findById(id).map(tenant -> {
            tenantRepository.deleteById(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/activate")
    public ResponseEntity<?> activateCustomer(@RequestParam("code") String code, @RequestParam("domain") String domain,
            @RequestParam("apikey") String apikey) {
        return tenantService.activateTenant(code, domain, apikey) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
