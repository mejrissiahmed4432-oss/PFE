package com.example.usermicroservice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetPasswordEmail(String to, String resetLink) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("Medina.tn");
        helper.setTo(to);
        helper.setSubject("Password Reset Request - ITManage");

        String htmlContent = "<div style='font-family: Arial, sans-serif; padding: 20px; color: #333;'>"
                + "<h2>Password Reset Request</h2>"
                + "<p>Hello,</p>"
                + "<p>We received a request to reset your password for your ITManage account. Click the button below to set a new password:</p>"
                + "<div style='margin: 30px 0;'>"
                + "<a href='" + resetLink + "' style='background-color: #3b82f6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>Reset Password</a>"
                + "</div>"
                + "<p>If you didn't request this, you can safely ignore this email.</p>"
                + "<p>Best regards,<br>The ITManage Team</p>"
                + "</div>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
