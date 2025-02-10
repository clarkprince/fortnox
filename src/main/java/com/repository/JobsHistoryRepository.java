package com.repository;

import com.entities.JobsHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobsHistoryRepository extends JpaRepository<JobsHistory, String> {
}
