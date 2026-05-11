package com.example.stockmanagermicroservice.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    public void sendOrderConfirmation(String to, String supplierName, String orderId, String total, String currency) {
        String subject = "Purchase Order Confirmation — " + orderId + " | MedinaFlux";
        
        String content = "<html><body style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #334155; line-height: 1.6; background-color: #f1f5f9; padding: 20px;\">"
                + "<div style=\"max-width: 650px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);\">"
                + "<div style=\"background: linear-gradient(135deg, #10b981 0%, #059669 100%); color: white; padding: 32px 40px; text-align: center;\">"
                + "<h1 style=\"margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 0.5px;\">MedinaFlux</h1>"
                + "<p style=\"margin: 8px 0 0; font-size: 16px; opacity: 0.9; text-transform: uppercase; letter-spacing: 1px;\">Purchase Order Confirmation</p>"
                + "</div>"
                + "<div style=\"padding: 40px;\">"
                + "<p style=\"font-size: 16px; margin-top: 0;\">Dear <strong>" + supplierName + "</strong>,</p>"
                + "<p style=\"font-size: 16px;\">We are pleased to inform you that your quotation has been accepted. This email serves as an official confirmation of our <strong>Purchase Order (" + orderId + ")</strong>.</p>"
                
                + "<div style=\"background: #f0fdf4; border-left: 4px solid #10b981; padding: 20px 24px; border-radius: 8px; margin: 24px 0;\">"
                + "<table style=\"width: 100%; border-collapse: collapse;\">"
                + "<tr><td style=\"padding: 8px 0; color: #64748b; font-size: 14px;\">Order Reference:</td><td style=\"padding: 8px 0; font-weight: 700; color: #0f172a; text-align: right;\">" + orderId + "</td></tr>"
                + "<tr><td style=\"padding: 8px 0; color: #64748b; font-size: 14px;\">Total Amount:</td><td style=\"padding: 8px 0; font-weight: 700; color: #10b981; font-size: 18px; text-align: right;\">" + total + " " + currency + "</td></tr>"
                + "<tr><td style=\"padding: 8px 0; color: #64748b; font-size: 14px;\">Status:</td><td style=\"padding: 8px 0; font-weight: 700; color: #059669; text-align: right;\">CONFIRMED</td></tr>"
                + "</table>"
                + "</div>"

                + "<p style=\"font-size: 15px; color: #475569;\">Please proceed with the delivery and billing according to our established procurement terms. Our logistics team will be in touch if further coordination is needed.</p>"
                
                + "<div style=\"margin-top: 40px; padding-top: 24px; border-top: 1px solid #e2e8f0; text-align: center;\">"
                + "<p style=\"margin: 0; font-size: 14px; color: #94a3b8;\">Thank you for your partnership.</p>"
                + "<p style=\"margin: 4px 0 0; font-weight: 600; color: #475569;\">MedinaFlux Procurement Division</p>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "</body></html>";
                
        sendSimpleMessage(to, subject, content);
    }
}
