package com.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.entities.Part;
import java.util.Optional;
import java.util.List;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByArticleNumberAndTenantDomain(String articleNumber, String tenantDomain);

    List<Part> findBySourceAndTenantDomain(String source, String tenantDomain);

    Page<Part> findBySourceAndTenantDomain(String source, String tenantDomain, Pageable pageable);
}