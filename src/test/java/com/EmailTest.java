package com;

import com.utils.EmailHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EmailTest {

    @Autowired
    private EmailHelper emailHelper;

    public static void main(String[] args) {
        SpringApplication.run(EmailTest.class, args);
    }

    @Bean
    public CommandLineRunner testEmail() {
        return args -> {
            System.out.println("Sending test email...");

            String to = "clark.prince15@gmail.com";
            String subject = "Test Email - SMTP Configuration";
            String body = "<html><body>" + "<h2>SMTP Test Email</h2>"
                    + "<p>This is a test email to verify the email configuration is working correctly.</p>" + "<p><strong>Date:</strong> "
                    + java.time.LocalDateTime.now() + "</p>" + "<p>If you received this email, the SMTP settings are configured properly!</p>"
                    + "</body></html>";

            boolean success = emailHelper.sendEmail(to, subject, body);

            if (success) {
                System.out.println("✓ Email sent successfully to " + to);
            } else {
                System.out.println("✗ Failed to send email");
            }

            System.exit(0);
        };
    }
}
