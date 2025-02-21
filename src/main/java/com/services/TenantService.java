package com.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.entities.Tenant;
import com.exceptions.TenantNotFoundException;
import com.repository.TenantRepository;
import com.services.fortnox.FortnoxAuth;

@Service
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    public Tenant getTenantByDomain(String domain) {
        return tenantRepository.findBySynchroteamDomain(domain).orElseThrow(() -> new TenantNotFoundException(domain));
    }

    public boolean activateTenant(String code, String domain, String apikey) {
        Tenant tenant = tenantRepository.findBySynchroteamDomain(domain.toLowerCase()).orElse(null);
        if (tenant == null) {
            tenant = new Tenant();
        }
        try {
            Map<String, String> auth = FortnoxAuth.doAuth(code, false);
            tenant.setFortnoxToken(auth.get("access_token"));
            tenant.setFortNoxRefreshToken(auth.get("refresh_token"));
            tenant.setSynchroteamDomain(domain.toLowerCase());
            tenant.setSynchroteamAPIKey(apikey);
            tenant.setTenantActive(true);
            tenantRepository.save(tenant);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
