package com.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.dto.JobPayload;
import com.utils.EmailHelper;
import com.utils.HTMLFomatter;
import com.utils.PDFHelper;

@Service
public class JobProcessingService {
    private final Logger log = LoggerFactory.getLogger(JobProcessingService.class);

    @Autowired
    private EmailHelper emailHelper;

    @Async()
    public void processJobAndSendEmails(JobPayload jobPayload, String[] emailAddresses) {
        try {
            String pdfUrl = jobPayload.getPublicLink() + "/PDF";
            String jobDescription = jobPayload.getDescription();
            String emailSubject = "Workwit Team - Job: "
                    + (jobDescription != null && jobDescription.length() > 20 ? jobDescription.substring(0, 20) : jobDescription);

            PDFHelper.PDFProcessResult result = PDFHelper.processPdfFromUrlWithBytes(pdfUrl);

            if (result != null) {
                String formattedHTML = HTMLFomatter.extractTextWithFormatting(result.getHtmlContent());
                String emailBody = "Good Day,<br><br>See report below:<br><br>" + formattedHTML;

                boolean allEmailsSent = true;
                for (String email : emailAddresses) {
                    boolean emailSent = emailHelper.sendEmailWithPdfAttachment(email.trim(), emailSubject, emailBody, result.getPdfBytes(),
                            jobPayload.getId() + ".pdf");
                    if (!emailSent) {
                        allEmailsSent = false;
                        log.error("Failed to send email to: " + email);
                    }
                }

                if (allEmailsSent) {
                    log.info("Emails sent successfully with PDF attachment.");
                } else {
                    log.error("Failed to send some emails.");
                }
            } else {
                log.error("Failed to process the PDF.");
            }
        } catch (Exception e) {
            log.error("Error processing job: " + e.getMessage(), e);
        }
    }
}
