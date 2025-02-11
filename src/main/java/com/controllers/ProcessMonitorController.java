package com.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.entities.ProcessMonitor;
import com.repository.ProcessMonitorRepository;
import java.util.List;

@RestController
@RequestMapping("/api/process-monitors")
public class ProcessMonitorController {
    private final ProcessMonitorRepository processMonitorRepository;

    public ProcessMonitorController(ProcessMonitorRepository processMonitorRepository) {
        this.processMonitorRepository = processMonitorRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProcessMonitor>> getAllProcessMonitors() {
        return ResponseEntity.ok(processMonitorRepository.findAll());
    }

    @GetMapping("/find")
    public ResponseEntity<ProcessMonitor> getProcessMonitor(@RequestParam String process, @RequestParam String tenant) {
        return processMonitorRepository.findByProcessAndTenant(process, tenant).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
