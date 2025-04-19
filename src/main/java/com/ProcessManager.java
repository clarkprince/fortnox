package com;

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
import com.repository.ProcessMonitorRepository;
import com.repository.SettingsRepository;
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

    @Autowired
    private SettingsRepository settingsRepository;

    @Autowired
    private ProcessMonitorRepository processMonitorRepository;

    private boolean isAuthProcessRunning = false;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting processes for all tenants...");
        while (true) {
            runProcessesByTenant();
            int schedule = settingsRepository.findBySetting("schedule").map(s -> Integer.parseInt(s.getValue())).orElse(300000);
            Thread.sleep(schedule * 1000);
        }
    }

    @Scheduled(cron = "0 */15 * * * ?")
    public void processMonitor() {
        log.info("Starting token refresh...");
        try {
            isAuthProcessRunning = true;
            authorisedTenants();
        } catch (Exception e) {
            log.error("Error during token refresh: ", e);
        } finally {
            isAuthProcessRunning = false;
        }
        log.info("Token refresh completed for all tenants.");
    }

    public void runProcessesByTenant() {
        if (isAuthProcessRunning) {
            try {
                log.info("Auth process is running, waiting for it to finish...");
                Thread.sleep(60 * 1000); // Wait for the auth process to finish
            } catch (Exception e) {
                log.error("Error while waiting for auth process to finish: ", e);
            }
        }
        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            try {
                if (!validTenant(tenant))
                    continue;

                // Run all processes
                runCustomerProcess(tenant);
                runPartsProcess(tenant);
                runJobsProcess(tenant);
                runInvoiceProcess(tenant);
            } catch (Exception e) {
                log.error("Error during process execution: ", e);
            }
        }
    }

    private void runCustomerProcess(Tenant tenant) {
        try {
            String fromTime = processMonitorRepository.findByProcessAndTenant(Processes.CUSTOMERS, tenant.getSynchroteamDomain())
                    .map(processMonitor -> processMonitor.getRunAt().minusMinutes(10)).orElse(LocalDateTime.now().minusMinutes(10))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            ProcessMonitor processMonitor = new ProcessMonitor(Processes.CUSTOMERS, tenant.getSynchroteamDomain());
            processMonitor = fnCustomersService.getCustomers(tenant, fromTime, 500, processMonitor);
            processMonitorService.saveOrUpdate(processMonitor);
            saveActivities(tenant.getSynchroteamDomain(), processMonitor.getActivities(), Processes.CUSTOMERS);
        } catch (Exception e) {
            log.error("Error during process execution: ", e);
        }
    }

    private void runPartsProcess(Tenant tenant) {
        try {
            String fromTime = processMonitorRepository.findByProcessAndTenant(Processes.PARTS, tenant.getSynchroteamDomain())
                    .map(processMonitor -> processMonitor.getRunAt().minusMinutes(10)).orElse(LocalDateTime.now().minusMinutes(10))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            ProcessMonitor processMonitor = new ProcessMonitor(Processes.PARTS, tenant.getSynchroteamDomain());
            processMonitor = articlesService.getParts(tenant, fromTime, 500, processMonitor);
            processMonitorService.saveOrUpdate(processMonitor);
            saveActivities(tenant.getSynchroteamDomain(), processMonitor.getActivities(), Processes.PARTS);
        } catch (Exception e) {
            log.error("Error during process execution: ", e);
        }
    }

    private void runJobsProcess(Tenant tenant) {
        try {
            String fromTime = processMonitorRepository.findByProcessAndTenant(Processes.JOBS, tenant.getSynchroteamDomain())
                    .map(processMonitor -> processMonitor.getRunAt().minusMinutes(10)).orElse(LocalDateTime.now().minusMinutes(10))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            ProcessMonitor processMonitor = new ProcessMonitor(Processes.JOBS, tenant.getSynchroteamDomain());
            processMonitor = jobsService.checkingValidatedJobs(tenant, fromTime, 100, processMonitor);
            processMonitorService.saveOrUpdate(processMonitor);
            saveActivities(tenant.getSynchroteamDomain(), processMonitor.getActivities(), Processes.JOBS);
        } catch (Exception e) {
            log.error("Error during process execution: ", e);
        }
    }

    private void runInvoiceProcess(Tenant tenant) {
        try {
            String fromTime = processMonitorRepository.findByProcessAndTenant(Processes.INVOICES, tenant.getSynchroteamDomain())
                    .map(processMonitor -> processMonitor.getRunAt().minusMinutes(10)).orElse(LocalDateTime.now().minusMinutes(10))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

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
            if (!activityRepository.existsByActivity1(activity.getActivity1())) {
                activity.setTenant(tenantDomain);
                activity.setProcess(process);
                activityRepository.save(activity);
            }
        }
    }

    private List<Tenant> authorisedTenants() {
        List<Tenant> tenants = new ArrayList<>();
        List<Tenant> aTenants = tenantRepository.findAll();
        for (Tenant tenant : aTenants) {
            try {
                if (validTenant(tenant)) {
                    Tenant aTenant = doFortnoxAuth(tenant);
                    if (aTenant != null && aTenant.isTenantActive()) {
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
            log.info("Authorising tenant: " + tenant.getSynchroteamDomain());
            Map<String, String> auth = FortnoxAuth.doAuth(tenant.getFortNoxRefreshToken(), true);
            tenant.setFortnoxToken(auth.get("access_token"));
            tenant.setFortNoxRefreshToken(auth.get("refresh_token"));
            log.info("Successfully authorised tenant: " + tenant.getSynchroteamDomain());
        } catch (Exception e) {
            tenant.setTenantActive(false);
            log.error("Error authorising tenant: " + tenant.getSynchroteamDomain(), e);
            e.printStackTrace();
        }
        tenantRepository.save(tenant);
        return tenant;
    }

    private boolean validTenant(Tenant tenant) {
        if (tenant.getFortnoxToken() == null || tenant.getFortnoxToken().isEmpty() || tenant.getFortNoxRefreshToken() == null
                || tenant.getFortNoxRefreshToken().isEmpty()) {
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
