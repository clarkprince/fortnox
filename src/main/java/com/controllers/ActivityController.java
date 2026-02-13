package com.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.constants.Constants;
import com.dto.PaginatedResponse;
import com.entities.Activity;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.ActivityRepository;
import com.services.TenantService;
import com.services.fortnox.Articles;
import com.services.fortnox.FnCustomers;
import com.services.synchroteam.Jobs;
import com.services.synchroteam.SynchroInvoices;
import com.utils.Utils;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityRepository activityRepository;

    private final Logger log = LoggerFactory.getLogger(ActivityController.class);

    @Autowired
    private Jobs jobsService;

    @Autowired
    private SynchroInvoices synchroInvoicesService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private Articles articlesService;

    @Autowired
    private FnCustomers fnCustomersService;

    public ActivityController(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    @GetMapping("/list")
    public ResponseEntity<PaginatedResponse<Activity>> getActivitiesList(@RequestHeader(value = "tenant", required = false) String tenant,
            @RequestParam(defaultValue = Constants.DEFAULT_START_DATE) String from, @RequestParam(required = false) String to,
            @RequestParam(required = false) String process, @RequestParam(defaultValue = "100") int size, @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String q) {

        LocalDateTime fromDate = Utils.parseDateTime(from);
        LocalDateTime toDate = to != null ? Utils.parseDateTime(to) : LocalDateTime.now();

        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Activity> pageResult;
        if (!Utils.isEmpty(q)) {
            if (!Utils.isEmpty(tenant)) {
                pageResult = activityRepository.findBySearchTermAndTenantAndProcessAndCreatedAtBetween(q, tenant, process, fromDate, toDate,
                        pageRequest);
            } else {
                pageResult = activityRepository.findBySearchTermAndProcessAndCreatedAtBetween(q, process, fromDate, toDate, pageRequest);
            }
        } else {
            if (!Utils.isEmpty(tenant)) {
                if (!Utils.isEmpty(process)) {
                    pageResult = activityRepository.findByTenantAndProcessAndCreatedAtBetween(tenant, process.toLowerCase(), fromDate, toDate,
                            pageRequest);
                } else {
                    pageResult = activityRepository.findByTenantAndCreatedAtBetween(tenant, fromDate, toDate, pageRequest);
                }
            } else {
                if (!Utils.isEmpty(process)) {
                    pageResult = activityRepository.findByProcessAndCreatedAtBetween(process, fromDate, toDate, pageRequest);
                } else {
                    pageResult = activityRepository.findByCreatedAtBetween(fromDate, toDate, pageRequest);
                }
            }
        }

        return ResponseEntity.ok(PaginatedResponse.from(pageResult));
    }

    @PostMapping("/rerun")
    public ResponseEntity<?> rerunActivities(@RequestBody List<Long> activityIds,
            @RequestHeader(value = "tenant", required = true) String tenantDomain) {

        for (Long id : activityIds) {
            Activity activity = activityRepository.findById(id).orElse(null);
            if (activity != null && activity.getActivity1() != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(activity.getActivity1());

                    Tenant tenant = tenantService.getTenantByDomain(tenantDomain);
                    Activity activityToUpdate = new Activity();
                    activityToUpdate.setId(activity.getId());
                    activityToUpdate.setProcess(activity.getProcess());
                    activityToUpdate.setTenant(tenant.getSynchroteamDomain());

                    switch (activity.getProcess()) {
                    case "jobs":
                        activityToUpdate = jobsService.reprocessJob(jsonNode.get("id").asText(), tenant, activity);
                        break;
                    case "invoices":
                        activityToUpdate = synchroInvoicesService.reprocessInvoice(jsonNode.get("id").asText(), tenant, activity);
                        break;
                    case "parts":
                        activityToUpdate = articlesService.reprocessArticle(jsonNode.get("ArticleNumber").asText(), tenant, activity);
                        break;
                    case "customers":
                        activityToUpdate = fnCustomersService.reprocessCustomer(jsonNode.get("CustomerNumber").asText(), tenant, activity);
                        break;
                    default:
                        log.error("Unknown process type: {}", activity.getProcess());
                        continue;
                    }

                    activityRepository.save(activityToUpdate);
                } catch (Exception e) {
                    log.error("Failed to rerun activity {}: {}", id, e.getMessage());
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Activity> getActivityById(@PathVariable Long id) {
        return activityRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
