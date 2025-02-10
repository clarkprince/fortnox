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
import com.services.synchroteam.Jobs;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private Jobs jobsService;

    @Autowired
    private TenantRepository tenantRepository;

    @GetMapping("/validated")
    public ResponseEntity<?> getValidatedJobs(@RequestParam String fromTime, @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "1") int page, @RequestHeader("tenant") String tenantDomain) {
        Optional<Tenant> tenant = tenantRepository.findBySynchroteamDomain(tenantDomain);
        return tenant.isPresent() ? ResponseEntity.ok(jobsService.requestJobs(tenant.get(), fromTime, pageSize, page))
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> getJobDetails(@PathVariable String id, @RequestHeader("tenant") String tenantDomain) {
        Optional<Tenant> tenant = tenantRepository.findBySynchroteamDomain(tenantDomain);
        return tenant.isPresent() ? ResponseEntity.ok(jobsService.retrieveJob(id, tenant.get())) : ResponseEntity.notFound().build();
    }
}
