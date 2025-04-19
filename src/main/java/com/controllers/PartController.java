package com.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.entities.PartsQueue;
import com.entities.Tenant;
import com.entities.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.PartsQueueRepository;
import com.repository.UserRepository;
import com.services.PartsQueueService;
import com.services.TenantService;
import com.services.fortnox.Articles;
import com.services.synchroteam.Parts;
import com.services.synchroteam.PartsBulkUpdate;
import com.utils.Utils;

@RestController
@RequestMapping("/api/parts")
public class PartController {

    private static final Logger log = LoggerFactory.getLogger(PartController.class);

    @Autowired
    private TenantService tenantService;

    @Autowired
    private Articles articles;

    @Autowired
    private PartsBulkUpdate partsBulkUpdate;

    @Autowired
    private PartsQueueService partsQueueService;

    @Autowired
    private PartsQueueRepository partsQueueRepository;

    @Autowired
    private UserRepository userRepository;

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
    public ResponseEntity<JsonNode> getSynchroteamParts(@RequestHeader("tenant") String tenantDomain, @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "1") int page) {
        try {
            Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
            String partsResponse = Parts.getParts(tenant, size, page);
            return ResponseEntity.ok(new ObjectMapper().readTree(partsResponse));
        } catch (Exception e) {
            log.error("Error retrieving parts: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = "/synchroteam/queued", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ResponseEntity<Map<String, Object>> getQueuedParts(@RequestHeader("tenant") String tenantDomain,
            @RequestParam(defaultValue = "100") int size, @RequestParam(defaultValue = "1") int page) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Page<PartsQueue> queuedPartsPage = partsQueueRepository.findByTenantDomainOrderByCreatedAtDesc(tenantDomain,
                    PageRequest.of(page - 1, size));

            List<Map<String, Object>> queuedParts = queuedPartsPage.getContent().stream().map(queue -> {
                Map<String, Object> queueMap = new HashMap<>();
                queueMap.put("queueId", queue.getId());
                queueMap.put("createdAt", queue.getCreatedAt());
                queueMap.put("status", queue.getStatus());
                queueMap.put("fileName", queue.getFileName());
                queueMap.put("totalParts", queue.getTotalParts());
                queueMap.put("processedParts", queue.getProcessedParts());
                queueMap.put("failedParts", queue.getFailedParts());
                queueMap.put("progress", queue.getProgress());

                // // Only include pending and failed parts in details
                // List<Map<String, Object>> details = queue.getDetails().stream().filter(detail
                // -> !detail.getStatus().equals("COMPLETED"))
                // .map(detail -> {
                // Map<String, Object> detailMap = new HashMap<>();
                // try {
                // PartDTO part = mapper.readValue(detail.getPartData(), PartDTO.class);
                // detailMap.put("part", part);
                // detailMap.put("status", detail.getStatus());
                // detailMap.put("errorDetails", detail.getErrorDetails());
                // detailMap.put("processedAt", detail.getProcessedAt());
                // } catch (Exception e) {
                // log.error("Error parsing part data: " + e.getMessage());
                // }
                // return detailMap;
                // }).collect(Collectors.toList());

                queueMap.put("details", new ArrayList());
                return queueMap;
            }).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("content", queuedParts);
            response.put("totalElements", queuedPartsPage.getTotalElements());
            response.put("totalPages", queuedPartsPage.getTotalPages());
            response.put("size", queuedPartsPage.getSize());
            response.put("number", queuedPartsPage.getNumber());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving queued parts: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
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
    public ResponseEntity<String> uploadPartsFile(@RequestParam("file") MultipartFile file, @RequestHeader("tenant") String tenantDomain,
            @RequestHeader("email") String email) {
        try {
            List<PartDTO> parts = partsBulkUpdate.processPartsFile(file);
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found: " + email));

            // Create queue entry
            PartsQueue queue = new PartsQueue();
            queue.setFileName(file.getOriginalFilename());
            queue.setTenantDomain(tenantDomain);
            queue.setUserId(user.getId());
            queue.setTotalParts(parts.size());
            queue = partsQueueRepository.save(queue);

            // Queue the parts for processing
            partsQueueService.queueParts(parts, tenantDomain, queue.getId());

            return ResponseEntity.ok("Parts queued for processing. Queue ID: " + queue.getId() + ". Total parts: " + parts.size());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing file: " + e.getMessage());
        }
    }
}
