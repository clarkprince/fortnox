package com.repository;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.entities.Activity;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
        Page<Activity> findByTenant(String tenant, Pageable pageable);

        Page<Activity> findByCreatedAtGreaterThanEqual(LocalDateTime from, Pageable pageable);

        Page<Activity> findByTenantAndCreatedAtGreaterThanEqual(String tenant, LocalDateTime from, Pageable pageable);

        Page<Activity> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

        Page<Activity> findByProcessAndCreatedAtBetween(String process, LocalDateTime from, LocalDateTime to, Pageable pageable);

        Page<Activity> findByTenantAndCreatedAtBetween(String tenant, LocalDateTime from, LocalDateTime to, Pageable pageable);

        Page<Activity> findByTenantAndProcessAndCreatedAtBetween(String tenant, String process, LocalDateTime from, LocalDateTime to,
                        Pageable pageable);

        @Query("SELECT a FROM Activity a WHERE " + "(LOWER(a.activity1) LIKE LOWER(CONCAT('%',:search,'%')) OR "
                        + "LOWER(a.activity2) LIKE LOWER(CONCAT('%',:search,'%')) OR " + "LOWER(a.message) LIKE LOWER(CONCAT('%',:search,'%'))) "
                        + "AND a.createdAt >= :fromDate")
        Page<Activity> findBySearchTermAndCreatedAtGreaterThanEqual(@Param("search") String search, @Param("fromDate") LocalDateTime fromDate,
                        Pageable pageable);

        @Query("SELECT a FROM Activity a WHERE " + "(LOWER(a.activity1) LIKE LOWER(CONCAT('%',:search,'%')) OR "
                        + "LOWER(a.activity2) LIKE LOWER(CONCAT('%',:search,'%')) OR " + "LOWER(a.message) LIKE LOWER(CONCAT('%',:search,'%'))) "
                        + "AND a.tenant = :tenant " + "AND a.createdAt >= :fromDate")
        Page<Activity> findBySearchTermAndTenantAndCreatedAtGreaterThanEqual(@Param("search") String search, @Param("tenant") String tenant,
                        @Param("fromDate") LocalDateTime fromDate, Pageable pageable);

        @Query("SELECT a FROM Activity a WHERE " + "(LOWER(a.activity1) LIKE LOWER(CONCAT('%',:search,'%')) OR "
                        + "LOWER(a.activity2) LIKE LOWER(CONCAT('%',:search,'%')) OR " + "LOWER(a.message) LIKE LOWER(CONCAT('%',:search,'%'))) "
                        + "AND (:process IS NULL OR a.process = :process) " + "AND a.createdAt BETWEEN :fromDate AND :toDate")
        Page<Activity> findBySearchTermAndProcessAndCreatedAtBetween(@Param("search") String search, @Param("process") String process,
                        @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate, Pageable pageable);

        @Query("SELECT a FROM Activity a WHERE " + "(LOWER(a.activity1) LIKE LOWER(CONCAT('%',:search,'%')) OR "
                        + "LOWER(a.activity2) LIKE LOWER(CONCAT('%',:search,'%')) OR " + "LOWER(a.message) LIKE LOWER(CONCAT('%',:search,'%'))) "
                        + "AND a.tenant = :tenant " + "AND (:process IS NULL OR a.process = :process) "
                        + "AND a.createdAt BETWEEN :fromDate AND :toDate")
        Page<Activity> findBySearchTermAndTenantAndProcessAndCreatedAtBetween(@Param("search") String search, @Param("tenant") String tenant,
                        @Param("process") String process, @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate,
                        Pageable pageable);
}
