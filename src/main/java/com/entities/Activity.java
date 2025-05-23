package com.entities;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "activities", indexes = { @Index(name = "idx_activities_tenant", columnList = "tenant"),
        @Index(name = "idx_activities_created_at", columnList = "created_at"), @Index(name = "idx_activities_process", columnList = "process"),
        @Index(name = "idx_activities_tenant_created_at", columnList = "tenant,created_at"),
        @Index(name = "idx_activities_tenant_process_created_at", columnList = "tenant,process,created_at") })
@Getter
@Setter
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity1", columnDefinition = "TEXT")
    private String activity1;

    @Column(name = "activity2", columnDefinition = "TEXT")
    private String activity2;

    @Column(name = "process", columnDefinition = "varchar(255) DEFAULT NULL")
    private String process;

    @Column(name = "successful", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean successful;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "tenant")
    private String tenant;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
