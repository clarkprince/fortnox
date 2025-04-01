package com.webhooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.controllers.ActivityController;
import com.dto.JobPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utils.EmailHelper;
import com.utils.HTMLToTextExtractor;
import com.utils.PDFHelper;

import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/webhooks")
public class JobListener {

    private final Logger log = LoggerFactory.getLogger(ActivityController.class);

    @Autowired
    private EmailHelper emailHelper;

    @PostMapping("/job")
    public void handleJobWebhook(@RequestBody String payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JobPayload jobPayload = mapper.readValue(payload, JobPayload.class);

            String pdfUrl = jobPayload.getPublicLink() + "/PDF";
            String jobDescription = jobPayload.getDescription();
            String emailSubject = "Workwit Team - Job: "
                    + (jobDescription != null && jobDescription.length() > 20 ? jobDescription.substring(0, 20) : jobDescription);

            // Process the PDF and get both PDF bytes and HTML content
            PDFHelper.PDFProcessResult result = PDFHelper.processPdfFromUrlWithBytes(pdfUrl);

            if (result != null) {

                // Convert HTML to formatted text for email body
                String formattedHTML = HTMLToTextExtractor.extractTextWithFormatting(result.getHtmlContent());
                String emailBody = "Good Day,<br><br>See report below:<br><br>" + formattedHTML;

                // Write HTML content to file for testing
                try {
                    Files.writeString(Paths.get("C:\\Code\\java\\output4.html"), formattedHTML);
                } catch (Exception e) {
                    log.error("Failed to write HTML content to file: " + e.getMessage());
                }

                // Send email with PDF attachment
                // boolean emailSent =
                // emailHelper.sendEmailWithPdfAttachment("clark.prince15@gmail.com",
                // emailSubject, emailBody, result.getPdfBytes(),
                // jobPayload.getId() + ".pdf");

                // if (emailSent) {
                // log.info("Email sent successfully with PDF attachment.");
                // } else {
                // log.error("Failed to send email.");
                // }
            } else {
                log.error("Failed to process the PDF.");
            }
        } catch (Exception e) {
            log.error("Error processing webhook payload: " + e.getMessage(), e);
        }
    }
}