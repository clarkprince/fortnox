package com.webhooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.controllers.ActivityController;
import com.dto.JobPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.JobProcessingService;

@RestController
@RequestMapping("/api/webhooks")
public class JobListener {

    private final Logger log = LoggerFactory.getLogger(ActivityController.class);

    @Autowired
    private JobProcessingService jobProcessingService;

    @PostMapping("/job")
    public ResponseEntity<String> handleJobWebhook(@RequestBody String payload, @RequestHeader(value = "emails", required = true) String emails) {
        try {
            String[] emailAddresses = emails.split(",");
            if (emailAddresses.length == 0) {
                return ResponseEntity.badRequest().body("No email addresses provided");
            }

            ObjectMapper mapper = new ObjectMapper();
            JobPayload jobPayload = mapper.readValue(payload, JobPayload.class);

            // Start async processing
            jobProcessingService.processJobAndSendEmails(jobPayload, emailAddresses);

            return ResponseEntity.accepted().body("Job processing started");
        } catch (Exception e) {
            log.error("Error processing webhook payload: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing request");
        }
    }
}