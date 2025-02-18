package com.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "settings", indexes = { @Index(name = "idx_tenant", columnList = "tenant"),
        @Index(name = "idx_tenant_section", columnList = "tenant,section"), @Index(name = "idx_tenant_setting", columnList = "tenant,setting") })
@Getter
@Setter
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String setting;

    @Column(nullable = false)
    private String value;

    private String section;

    private String tenant;

    @JsonIgnore
    @Column(name = "last_modified", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime lastModified;

    @PrePersist
    @PreUpdate
    protected void onChange() {
        lastModified = LocalDateTime.now();
    }
}
