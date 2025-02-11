package com.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.entities.Tenant;
import com.exceptions.TenantNotFoundException;
import com.repository.TenantRepository;

@Service
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    public Tenant getTenantByDomain(String domain) {
        return tenantRepository.findBySynchroteamDomain(domain).orElseThrow(() -> new TenantNotFoundException(domain));
    }
}
