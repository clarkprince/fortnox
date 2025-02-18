package com.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.entities.ProcessMonitor;
import com.repository.ProcessMonitorRepository;

@RestController
@RequestMapping("/api/process-monitors")
public class ProcessMonitorController {

    @Autowired
    private ProcessMonitorRepository processMonitorRepository;

    @GetMapping("/tenant/{tenant}/latest")
    public ResponseEntity<ProcessMonitor> getLatestProcessStatus(@PathVariable String tenant) {
        return processMonitorRepository.findLatestByTenant(tenant).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
