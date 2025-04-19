package com.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "queue_parts")
@Getter
@Setter
public class PartsQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Column(name = "tenant_domain")
    private String tenantDomain;

    @Column(name = "user_id")
    private Integer userId;

    private Integer totalParts = 0;
    private Integer processedParts = 0;
    private Integer failedParts = 0;
    private String status = "PENDING";

    @OneToMany(mappedBy = "queue", cascade = CascadeType.ALL)
    private List<QueuePartDetails> details;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public double getProgress() {
        return totalParts == 0 ? 0 : (processedParts * 100.0) / totalParts;
    }
}
