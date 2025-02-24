package com.dto;

import java.util.List;
import com.entities.Tenant;
import com.entities.Settings;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantDTO {
    private Tenant tenant;
    private List<Settings> settings;

    public TenantDTO(Tenant tenant, List<Settings> settings) {
        this.tenant = tenant;
        this.settings = settings;
    }
}
