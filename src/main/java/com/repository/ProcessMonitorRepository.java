package com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.entities.ProcessMonitor;
import java.util.Optional;

@Repository
public interface ProcessMonitorRepository extends JpaRepository<ProcessMonitor, Long> {
    Optional<ProcessMonitor> findByProcessAndTenant(String process, String tenant);
}
