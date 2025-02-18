package com.controllers;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.constants.Constants;
import com.dto.PaginatedResponse;
import com.entities.Activity;
import com.repository.ActivityRepository;
import com.utils.Utils;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityRepository activityRepository;

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
                    pageResult = activityRepository.findByTenantAndProcessAndCreatedAtBetween(tenant, process, fromDate, toDate, pageRequest);
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

    @GetMapping("/{id}")
    public ResponseEntity<Activity> getActivityById(@PathVariable Long id) {
        return activityRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
