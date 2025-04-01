package com.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import jakarta.mail.util.ByteArrayDataSource;

@Component
public class EmailHelper {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from.address}")
    private String fromAddress;

    @Value("${spring.mail.from.name}")
    private String fromName;

    public boolean sendEmail(String to, String subject, String body) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends an email with attachments (both HTML content and PDF)
     */
    public boolean sendEmailWithAttachment(String to, String subject, String body, String htmlContent, String attachmentName) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            // Add the HTML content as an attachment
            ByteArrayDataSource htmlDataSource = new ByteArrayDataSource(htmlContent, "text/html;charset=UTF-8");
            helper.addAttachment(attachmentName, htmlDataSource);

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends an email with PDF attachment and HTML body
     */
    public boolean sendEmailWithPdfAttachment(String to, String subject, String body, byte[] pdfBytes, String attachmentName) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            // Add the PDF as attachment
            ByteArrayDataSource pdfDataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
            helper.addAttachment(attachmentName, pdfDataSource);

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}