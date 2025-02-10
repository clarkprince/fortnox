package com.repository;

import com.entities.Settings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettingsRepository extends JpaRepository<Settings, Integer> {
    List<Settings> findByTenant(String tenant);

    List<Settings> findByTenantAndSection(String tenant, String section);

    Optional<Settings> findByTenantAndSetting(String tenant, String setting);
}
