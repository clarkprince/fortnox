package com.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.constants.Constants;
import com.entities.Tenant;
import com.services.TenantService;
import com.services.synchroteam.Jobs;
import com.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private Jobs jobsService;

    @Autowired
    private TenantService tenantService;

    @GetMapping("/validated")
    public ResponseEntity<?> getValidatedJobs(@RequestHeader("tenant") String tenantDomain,
            @RequestParam(defaultValue = Constants.DEFAULT_START_DATE) String from, @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "1") int page) {

        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        String jobs = jobsService.requestJobs(tenant, from, size, page);
        return jobs != null ? ResponseEntity.ok(Utils.prettyPrint(jobs)) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getJobDetails(@PathVariable String id, @RequestHeader("tenant") String tenantDomain) {

        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        JsonNode job = jobsService.retrieveJob(id, tenant);
        return job != null ? ResponseEntity.ok(job.toPrettyString()) : ResponseEntity.notFound().build();
    }
}
