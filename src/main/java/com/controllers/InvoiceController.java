package com.controllers;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.services.synchroteam.SynchroInvoices;
import com.utils.Utils;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private SynchroInvoices synchroInvoicesService;

    @Autowired
    private TenantService tenantService;

    @GetMapping("/list")
    public ResponseEntity<?> getInvoiceList(@RequestHeader("tenant") String tenantDomain,
            @RequestParam(defaultValue = Constants.DEFAULT_START_DATE) String from, @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "1") int page) {

        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        String invoices = synchroInvoicesService.requestInvoices(tenant, from, size, page);
        return invoices != null ? ResponseEntity.ok(Utils.prettyPrint(invoices)) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getInvoiceDetails(@PathVariable String id, @RequestHeader("tenant") String tenantDomain) {

        Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
        JsonNode invoice = synchroInvoicesService.retrieveInvoice(id, tenant);
        return invoice != null ? ResponseEntity.ok(invoice.toPrettyString()) : ResponseEntity.notFound().build();
    }
}
