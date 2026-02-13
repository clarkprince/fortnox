package com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.entities.QueuePartDetails;
import java.util.List;

public interface QueuePartDetailsRepository extends JpaRepository<QueuePartDetails, Long> {

    List<QueuePartDetails> findByQueueIdAndStatus(Long queueId, String status);

    @Query("SELECT COUNT(d) FROM QueuePartDetails d WHERE d.status = ?1")
    int countByStatus(String status);

    @Query("SELECT d FROM QueuePartDetails d WHERE d.status = 'PENDING' ORDER BY d.createdAt ASC")
    List<QueuePartDetails> findPendingDetails(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT d FROM QueuePartDetails d WHERE d.queue.tenantDomain = ?1 AND d.status = ?2 ORDER BY d.createdAt ASC")
    List<QueuePartDetails> findByTenantDomainAndStatusOrderByCreatedAt(String tenantDomain, String status);

    List<QueuePartDetails> findByStatus(String status);
}
