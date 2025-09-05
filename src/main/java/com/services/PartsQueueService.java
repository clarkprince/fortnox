package com.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dto.PartDTO;
import com.entities.Activity;
import com.entities.PartsQueue;
import com.entities.QueuePartDetails;
import com.entities.Tenant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.PartsQueueRepository;
import com.repository.QueuePartDetailsRepository;
import com.repository.TenantRepository;
import com.services.synchroteam.Parts;

@Service
public class PartsQueueService {
    @Autowired
    private PartsQueueRepository queueRepository;

    @Autowired
    private QueuePartDetailsRepository detailsRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private final Logger log = LoggerFactory.getLogger(PartsQueueService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void queueParts(List<PartDTO> parts, String tenantDomain, Long queueId) {
        PartsQueue queue = queueRepository.findById(queueId).orElseThrow(() -> new RuntimeException("Queue not found: " + queueId));

        List<QueuePartDetails> details = parts.stream().map(part -> {
            try {
                QueuePartDetails detail = new QueuePartDetails();
                detail.setQueue(queue);
                detail.setPartData(objectMapper.writeValueAsString(part));
                detail.setStatus("PENDING");
                return detail;
            } catch (Exception e) {
                throw new RuntimeException("Failed to queue part", e);
            }
        }).collect(Collectors.toList());

        queue.setDetails(details);
        queue.setTotalParts(parts.size());
        queue.setStatus("PENDING"); // Set initial status
        queueRepository.save(queue);
    }

    @Async
    @Scheduled(fixedRate = 60000)
    public void processQueue() {
        try {
            List<QueuePartDetails> pendingDetails = detailsRepository.findPendingDetails(1000);
            if (pendingDetails.isEmpty())
                return;

            List<Tenant> tenants = tenantRepository.findAllOptimised();
            Map<String, Tenant> tenantMap = tenants.stream()
                    .filter(t -> t.getSynchroteamQuotaRemaining() == null || t.getSynchroteamQuotaRemaining() > 0)
                    .collect(Collectors.toMap(Tenant::getSynchroteamDomain, t -> t));

            for (QueuePartDetails detail : pendingDetails) {
                Tenant tenant = tenantMap.get(detail.getQueue().getTenantDomain());
                if (tenant != null) {
                    processQueueDetail(detail, tenant);
                }
            }
        } catch (Exception e) {
            log.error("Error processing queue: " + e.getMessage());
        }
    }

    private void processQueueDetail(QueuePartDetails detail, Tenant tenant) {
        try {
            PartDTO part = objectMapper.readValue(detail.getPartData(), PartDTO.class);
            Activity result = Parts.updatePart(part, tenant);

            detail.setStatus(result.isSuccessful() ? "COMPLETED" : "FAILED");
            detail.setErrorDetails(result.isSuccessful() ? null : result.getActivity2());
            detail.setProcessedAt(LocalDateTime.now());

            PartsQueue queue = detail.getQueue();
            queue.setProcessedParts(queue.getProcessedParts() + 1);
            if (!result.isSuccessful()) {
                queue.setFailedParts(queue.getFailedParts() + 1);
            }

            // Update queue status if all parts are processed
            if (queue.getProcessedParts().equals(queue.getTotalParts())) {
                queue.setStatus("COMPLETED");
                queue.setProcessedAt(LocalDateTime.now());
            }

            detailsRepository.save(detail);
            queueRepository.save(queue);
        } catch (Exception e) {
            detail.setStatus("FAILED");
            detail.setErrorDetails(e.getMessage());
            detail.setProcessedAt(LocalDateTime.now());

            PartsQueue queue = detail.getQueue();
            queue.setFailedParts(queue.getFailedParts() + 1);
            queue.setProcessedParts(queue.getProcessedParts() + 1);

            // Update queue status if all parts are processed
            if (queue.getProcessedParts().equals(queue.getTotalParts())) {
                queue.setStatus("COMPLETED");
                queue.setProcessedAt(LocalDateTime.now());
            }

            detailsRepository.save(detail);
            queueRepository.save(queue);
        }
    }
}
