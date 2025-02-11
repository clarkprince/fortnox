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

import com.constants.Processes;
import com.entities.Activity;
import com.entities.ProcessMonitor;
import com.entities.Tenant;
import com.repository.ActivityRepository;
import com.repository.TenantRepository;
import com.services.ProcessMonitorService;
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
    private ProcessMonitorService processMonitorService;

    @Autowired
    private ActivityRepository activityRepository;

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
            Thread.sleep(300000);
            // Thread.sleep(3000000);
        }
    }

    @Scheduled(cron = "0 * * * * ?")
    public void processMonitor() {
        // log.info("Starting process monitor...");
    }

    public void runProcessesByTenant() {
        String fromTime = LocalDateTime.now().minusMinutes(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<Tenant> tenants = authorisedTenants();
        for (Tenant tenant : tenants) {
            try {
                if (!validTenant(tenant))
                    continue;

                // Run all processes
                runCustomerProcess(tenant, fromTime);
                runPartsProcess(tenant, fromTime);
                runJobsProcess(tenant, fromTime);
                runInvoiceProcess(tenant, fromTime);
            } catch (Exception e) {
                log.error("Error during process execution: ", e);
            }
        }

    }

    private void runCustomerProcess(Tenant tenant, String fromTime) {
        try {
            ProcessMonitor processMonitor = new ProcessMonitor(Processes.CUSTOMERS, tenant.getSynchroteamDomain());
            processMonitor = fnCustomersService.getCustomers(tenant, fromTime, 500, processMonitor);
            processMonitorService.saveOrUpdate(processMonitor);
            saveActivities(tenant.getSynchroteamDomain(), processMonitor.getActivities(), Processes.CUSTOMERS);
        } catch (Exception e) {
            log.error("Error during process execution: ", e);
        }
    }

    private void runPartsProcess(Tenant tenant, String fromTime) {
        try {
            ProcessMonitor processMonitor = new ProcessMonitor(Processes.PARTS, tenant.getSynchroteamDomain());
            processMonitor = articlesService.getParts(tenant, fromTime, 500, processMonitor);
            processMonitorService.saveOrUpdate(processMonitor);
            saveActivities(tenant.getSynchroteamDomain(), processMonitor.getActivities(), Processes.PARTS);
        } catch (Exception e) {
            log.error("Error during process execution: ", e);
        }
    }

    private void runJobsProcess(Tenant tenant, String fromTime) {
        try {
            ProcessMonitor processMonitor = new ProcessMonitor(Processes.JOBS, tenant.getSynchroteamDomain());
            processMonitor = jobsService.checkingValidatedJobs(tenant, fromTime, 100, processMonitor);
            processMonitorService.saveOrUpdate(processMonitor);
            saveActivities(tenant.getSynchroteamDomain(), processMonitor.getActivities(), Processes.JOBS);
        } catch (Exception e) {
            log.error("Error during process execution: ", e);
        }
    }

    private void runInvoiceProcess(Tenant tenant, String fromTime) {
        try {
            ProcessMonitor processMonitor = new ProcessMonitor(Processes.INVOICES, tenant.getSynchroteamDomain());
            processMonitor = synchroInvoicesService.invoiceList(tenant, fromTime, 100, processMonitor);
            processMonitorService.saveOrUpdate(processMonitor);
            saveActivities(tenant.getSynchroteamDomain(), processMonitor.getActivities(), Processes.INVOICES);
        } catch (Exception e) {
            log.error("Error during process execution: ", e);
        }
    }

    private void saveActivities(String tenantDomain, List<Activity> activities, String process) {
        for (Activity activity : activities) {
            activity.setTenant(tenantDomain);
            activity.setProcess(process);
            activityRepository.save(activity);
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
