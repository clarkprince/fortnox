package com.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.entities.Tenant;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Integer> {
    Optional<Tenant> findBySynchroteamDomain(String synchroteamDomain);

    @Query("SELECT t FROM Tenant t ORDER BY t.id ASC")
    List<Tenant> findAllOptimised();
}
