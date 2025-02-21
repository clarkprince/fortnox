package com.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.entities.Settings;
import com.repository.SettingsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private SettingsRepository settingsRepository;

    @GetMapping
    public ResponseEntity<List<Settings>> getAllSettings() {
        return ResponseEntity.ok(settingsRepository.findAllOptimised());
    }

    @PostMapping
    public ResponseEntity<Void> updateSettings(@RequestBody List<Settings> settings) {
        try {
            for (Settings setting : settings) {
                settingsRepository.findBySetting(setting.getSetting()).ifPresentOrElse(existingSetting -> {
                    existingSetting.setValue(setting.getValue());
                    existingSetting.setSection(setting.getSection());
                    settingsRepository.save(existingSetting);
                }, () -> settingsRepository.save(setting));
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
