package com.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.entities.Part;
import com.repository.PartRepository;
import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {
    private final PartRepository partRepository;

    public PartController(PartRepository partRepository) {
        this.partRepository = partRepository;
    }

    @GetMapping
    public ResponseEntity<List<Part>> getAllParts() {
        return ResponseEntity.ok(partRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Part> getPartById(@PathVariable Long id) {
        return partRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/find")
    public ResponseEntity<List<Part>> getPartsBySourceAndTenant(@RequestParam String source, @RequestParam String tenantDomain) {
        return ResponseEntity.ok(partRepository.findBySourceAndTenantDomain(source, tenantDomain));
    }
}
