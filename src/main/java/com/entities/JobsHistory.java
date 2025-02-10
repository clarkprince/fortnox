package com.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "jobs_history")
@Getter
@Setter
public class JobsHistory {
    @Id
    private String id;
}
