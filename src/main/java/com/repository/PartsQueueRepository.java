package com.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.entities.PartsQueue;
import java.util.List;

public interface PartsQueueRepository extends JpaRepository<PartsQueue, Long> {
    List<PartsQueue> findByTenantDomainOrderByCreatedAtDesc(String tenantDomain);

    List<PartsQueue> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<PartsQueue> findByTenantDomainAndUserIdOrderByCreatedAtDesc(String tenantDomain, Integer userId);

    Page<PartsQueue> findByTenantDomainOrderByCreatedAtDesc(String tenantDomain, Pageable pageable);
}
