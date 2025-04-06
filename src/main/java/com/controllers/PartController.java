package com.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.constants.Constants;
import com.dto.PartDTO;
import com.dto.PartPriceUpdateDTO;
import com.entities.Activity;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.services.TenantService;
import com.services.fortnox.Articles;
import com.services.synchroteam.Parts;
import com.services.synchroteam.PartsBulkUpdate;
import com.utils.Utils;

import java.util.List;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private Articles articles;

    @Autowired
    private PartsBulkUpdate partsBulkUpdate;

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

    @GetMapping(value = "/synchroteam/list", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getSynchroteamParts(@RequestHeader("tenant") String tenantDomain, @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "1") int page) {
        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        String parts = Parts.getParts(tenant, size, page);
        return parts != null ? ResponseEntity.ok(Utils.prettyPrint(parts)) : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/synchroteam/{id}", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> getSynchroteamPart(@PathVariable String id, @RequestHeader("tenant") String tenantDomain) {
        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        String part = Parts.getPart(tenant, id, false);
        return part != null ? ResponseEntity.ok(Utils.prettyPrint(part)) : ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/synchroteam/search", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> findSynchroteamPart(@RequestParam String reference, @RequestHeader("tenant") String tenantDomain) {
        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        String part = Parts.getPart(tenant, reference, true);
        return part != null ? ResponseEntity.ok(Utils.prettyPrint(part)) : ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/synchroteam/prices", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateSynchroteamPartPrices(@RequestBody List<PartPriceUpdateDTO> prices,
            @RequestHeader("tenant") String tenantDomain) {
        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        Activity activity = new Activity();
        activity = Parts.updatePartPrices(prices, tenant, activity);
        return activity.isSuccessful() ? ResponseEntity.ok(activity.getActivity2()) : ResponseEntity.badRequest().build();
    }

    @PostMapping(value = "/synchroteam/prices/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadPriceFile(@RequestParam("file") MultipartFile file, @RequestHeader("tenant") String tenantDomain) {
        try {
            Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
            List<PartPriceUpdateDTO> prices = partsBulkUpdate.processFile(file);
            Activity activity = new Activity();
            activity = Parts.updatePartPrices(prices, tenant, activity);
            return activity.isSuccessful() ? ResponseEntity.ok(activity.getActivity2()) : ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }

    @PostMapping(value = "/synchroteam/parts/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadPartsFile(@RequestParam("file") MultipartFile file, @RequestHeader("tenant") String tenantDomain) {
        try {
            Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
            List<PartDTO> parts = partsBulkUpdate.processPartsFile(file);
            Activity activity = new Activity();
            activity = Parts.updateParts(parts, tenant, activity);
            return activity.isSuccessful() ? ResponseEntity.ok(activity.getActivity2()) : ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }
}
