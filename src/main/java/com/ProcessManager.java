package com;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.entities.Tenant;
import com.repository.TenantRepository;
import com.services.fortnox.Articles;
import com.services.fortnox.FnCustomers;
import com.services.fortnox.FortnoxAuth;
import com.services.synchroteam.Jobs;
import com.services.synchroteam.SynchroInvoices;

@Service
@Configuration
@EnableScheduling
public class ProcessManager implements CommandLineRunner {

    private final Logger log = LoggerFactory.getLogger(ProcessManager.class);

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private Jobs jobsService;

    @Autowired
    private SynchroInvoices synchroInvoicesService;

    @Autowired
    private FnCustomers fnCustomersService;

    @Autowired
    private Articles articlesService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting processes for all tenants...");
        while (true) {
            runProcessesByTenant();
            // wait for 5 minutes
            // Thread.sleep(300000);
            Thread.sleep(60000);
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    public void processMonitor() {
        // log.info("Starting process monitor...");
    }

    public void runProcessesByTenant() {
        String fromTime = LocalDateTime.now().minusMinutes(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        fromTime = "2025-02-03 00:00:00";
        List<Tenant> tenants = authorisedTenants();
        for (Tenant tenant : tenants) {
            try {
                if (!validTenant(tenant))
                    continue;

                // Run all processes
                // fnCustomersService.getCustomers(tenant, fromTime, 500);
                // articlesService.getParts(tenant, fromTime, 500);
                jobsService.checkingValidatedJobs(tenant, fromTime, 100);
                // synchroInvoicesService.invoiceList(tenant, fromTime, 100);

            } catch (Exception e) {
                log.error("Error during process execution: ", e);
            }
        }

    }

    private List<Tenant> authorisedTenants() {
        List<Tenant> tenants = new ArrayList<>();
        List<Tenant> aTenants = tenantRepository.findAll();
        for (Tenant tenant : aTenants) {
            try {
                if (tenant.getSynchroteamDomain().equalsIgnoreCase("workwitsolution")) { // ************

                    Tenant aTenant = doFortnoxAuth(tenant);
                    if (aTenant != null) {
                        tenants.add(aTenant);
                    }
                }
            } catch (Exception e) {
                log.error("Error: ", e);
                e.printStackTrace();
            }
        }
        return tenants;
    }

    private Tenant doFortnoxAuth(Tenant tenant) {
        try {
            Map<String, String> auth = FortnoxAuth.doAuth(tenant.getFortNoxRefreshToken(), true);
            tenant.setFortnoxToken(auth.get("access_token"));
            tenant.setFortNoxRefreshToken(auth.get("refresh_token"));
            tenantRepository.save(tenant);
        } catch (IOException e) {
            log.error("Error: ", e);
            e.printStackTrace();
        }
        return tenant;
    }

    private boolean validTenant(Tenant tenant) {
        if (tenant.getFortnoxToken() == null) {
            log.error("Token not found");
            return false;
        }
        return true;
    }

    public Tenant authorisedTenantByDomain(String domain) {
        Optional<Tenant> optionalTenant = tenantRepository.findBySynchroteamDomain(domain);
        if (optionalTenant.isPresent()) {
            Tenant tenant = optionalTenant.get();
            try {
                return doFortnoxAuth(tenant);
            } catch (Exception e) {
                log.error("Error: ", e);
                e.printStackTrace();
            }
        }
        return null;
    }
}
