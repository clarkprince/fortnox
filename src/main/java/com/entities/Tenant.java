package com.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tenants")
@Getter
@Setter
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "fortnox_refresh_token")
    private String fortNoxRefreshToken;

    @Column(name = "synchroteam_domain")
    private String synchroteamDomain;

    @Column(name = "synchroteam_api_key")
    private String synchroteamAPIKey;

    @Transient
    private String fortnoxToken;
}
