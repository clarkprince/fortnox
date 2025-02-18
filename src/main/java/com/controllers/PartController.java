package com.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.constants.Constants;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.services.TenantService;
import com.services.fortnox.Articles;
import com.utils.Utils;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private Articles articles;

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getPartsList(@RequestHeader("tenant") String tenantDomain,
            @RequestParam(defaultValue = Constants.DEFAULT_START_DATE) String from, @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "0") int offset) {

        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        String parts = articles.requestArticles(tenant, from, size, offset);
        return parts != null ? ResponseEntity.ok(Utils.prettyPrint(parts)) : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getPartDetails(@PathVariable String id, @RequestHeader("tenant") String tenantDomain) {
        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        JsonNode partDetails = articles.doGetPartDetails(id, tenant);
        return partDetails != null ? ResponseEntity.ok(partDetails.toPrettyString()) : ResponseEntity.notFound().build();
    }
}
