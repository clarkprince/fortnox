package com.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.entities.Settings;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Integer> {
    @Query("SELECT s FROM Settings s")
    List<Settings> findAllOptimised();

    @Query("SELECT s FROM Settings s WHERE s.setting = :setting")
    Optional<Settings> findBySetting(String setting);
}
