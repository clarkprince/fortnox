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
import com.services.synchroteam.SynchroInvoices;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private SynchroInvoices synchroInvoicesService;

    @Autowired
    private TenantRepository tenantRepository;

    @GetMapping("/list")
    public ResponseEntity<?> getInvoiceList(@RequestParam String fromTime, @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "1") int page, @RequestHeader("tenant") String tenantDomain) {
        Optional<Tenant> tenant = tenantRepository.findBySynchroteamDomain(tenantDomain);
        return tenant.isPresent() ? ResponseEntity.ok(synchroInvoicesService.requestInvoices(tenant.get(), fromTime, pageSize, page))
                : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> getInvoiceDetails(@PathVariable String id, @RequestHeader("tenant") String tenantDomain) {
        Optional<Tenant> tenant = tenantRepository.findBySynchroteamDomain(tenantDomain);
        return tenant.isPresent() ? ResponseEntity.ok(synchroInvoicesService.retrieveInvoice(id, tenant.get())) : ResponseEntity.notFound().build();
    }
}
