package com.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.entities.Settings;
import com.repository.SettingsRepository;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsRepository settingsRepository;

    public SettingsController(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @GetMapping
    public ResponseEntity<List<Settings>> getAllSettings() {
        return ResponseEntity.ok(settingsRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Settings> getSetting(@PathVariable int id) {
        return settingsRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tenant/{tenant}")
    public ResponseEntity<List<Settings>> getSettingsByTenant(@PathVariable String tenant) {
        return ResponseEntity.ok(settingsRepository.findByTenant(tenant));
    }

    @GetMapping("/tenant/{tenant}/section/{section}")
    public ResponseEntity<List<Settings>> getSettingsByTenantAndSection(@PathVariable String tenant, @PathVariable String section) {
        return ResponseEntity.ok(settingsRepository.findByTenantAndSection(tenant, section));
    }

    @PostMapping
    public ResponseEntity<Settings> createSetting(@RequestBody Settings settings) {
        Settings savedSettings = settingsRepository.save(settings);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSettings);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Settings> updateSetting(@PathVariable int id, @RequestBody Settings settings) {
        return settingsRepository.findById(id).map(existingSettings -> {
            settings.setId(id);
            return ResponseEntity.ok(settingsRepository.save(settings));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSetting(@PathVariable int id) {
        return settingsRepository.findById(id).map(settings -> {
            settingsRepository.deleteById(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
