package com.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "process_monitor")
@Getter
@Setter
public class ProcessMonitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;

    @Column(name = "process", nullable = false)
    @JsonIgnore
    private String process;

    @Column(name = "successful", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean successful;

    @Column(name = "tenant")
    @JsonIgnore
    private String tenant;

    @Transient
    @JsonIgnore
    private List<Activity> activities;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "run_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime runAt;

    public ProcessMonitor() {
    }

    public ProcessMonitor(String process, String tenant) {
        this.process = process;
        this.tenant = tenant;
        this.activities = new ArrayList<>();
    }

    @PrePersist
    @PreUpdate
    protected void onChange() {
        runAt = LocalDateTime.now();
    }
}
