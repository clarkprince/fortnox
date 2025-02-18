package com.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.entities.ProcessMonitor;

@Repository
public interface ProcessMonitorRepository extends JpaRepository<ProcessMonitor, Long> {
    @Query("SELECT pm FROM ProcessMonitor pm WHERE pm.process = :process AND pm.tenant = :tenant")
    Optional<ProcessMonitor> findByProcessAndTenant(String process, String tenant);

    @Query("SELECT pm FROM ProcessMonitor pm WHERE pm.tenant = :tenant AND pm.runAt = ("
            + "SELECT MAX(p.runAt) FROM ProcessMonitor p WHERE p.tenant = :tenant)")
    Optional<ProcessMonitor> findLatestByTenant(@Param("tenant") String tenant);
}
