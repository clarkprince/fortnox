package com.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.constants.Constants;
import com.dto.TenantDTO;
import com.entities.Settings;
import com.entities.Tenant;
import com.repository.SettingsRepository;
import com.repository.TenantRepository;
import com.services.TenantService;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private SettingsRepository settingsRepository;

    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAllOptimised();
        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable int id) {
        return tenantRepository.findById(id).map(tenant -> {
            List<Settings> settings = settingsRepository.findByTenant(tenant.getSynchroteamDomain());
            return ResponseEntity.ok(new TenantDTO(tenant, settings));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Tenant> createTenant(@RequestBody TenantDTO tenantDTO) {
        Tenant savedTenant = tenantRepository.save(tenantDTO.getTenant());
        saveSettings(tenantDTO.getSettings(), savedTenant);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTenant);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(@PathVariable int id, @RequestBody TenantDTO tenantDTO) {
        return tenantRepository.findById(id).map(existingTenant -> {
            existingTenant.setSynchroteamAPIKey(tenantDTO.getTenant().getSynchroteamAPIKey());
            existingTenant.setSynchroteamDomain(tenantDTO.getTenant().getSynchroteamDomain());
            Tenant updated = tenantRepository.save(existingTenant);
            saveSettings(tenantDTO.getSettings(), updated);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    private void saveSettings(List<Settings> settings, Tenant tenant) {
        if (settings != null) {
            settings.forEach(setting -> {
                setting.setTenant(tenant.getSynchroteamDomain());
                setting.setSection(Constants.SECTION_GENERAL);
                settingsRepository.findBySettingAndTenant(setting.getSetting(), tenant.getSynchroteamDomain()).ifPresentOrElse(existingSetting -> {
                    existingSetting.setValue(setting.getValue());
                    existingSetting.setSection(setting.getSection());
                    settingsRepository.save(existingSetting);
                }, () -> settingsRepository.save(setting));
            });
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTenant(@PathVariable int id) {
        return tenantRepository.findById(id).map(tenant -> {
            tenantRepository.deleteById(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/activate")
    public ResponseEntity<?> activateCustomer(@RequestParam("code") String code, @RequestParam("domain") String domain,
            @RequestParam("apikey") String apikey) {
        return tenantService.activateTenant(code, domain, apikey) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
