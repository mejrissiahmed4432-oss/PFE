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
                + "<a href='" + resetLink
                + "' style='background-color: #3b82f6; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;'>Reset Password</a>"
                + "</div>"
                + "<p>If you didn't request this, you can safely ignore this email.</p>"
                + "<p>Best regards,<br>The ITManage Team</p>"
                + "</div>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    public void sendWelcomeEmail(String to, String firstName, String setPasswordLink) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom("Medina.tn");
        helper.setTo(to);
        helper.setSubject("Welcome to Medina ITManage – Set Your Password");

        String htmlContent = "<div style='font-family: Inter, Arial, sans-serif; max-width: 560px; margin: 0 auto; background: #f8fafc; padding: 32px;'>"
                + "  <div style='background: linear-gradient(135deg, #3b82f6, #06b6d4); border-radius: 16px; padding: 32px; text-align: center; margin-bottom: 24px;'>"
                + "    <h1 style='color: white; margin: 0; font-size: 24px; font-weight: 800;'> Welcome to Medina ITManage</h1>"
                + "    <p style='color: rgba(255,255,255,0.85); margin: 8px 0 0 0; font-size: 15px;'>Your account has been created</p>"
                + "  </div>"
                + "  <div style='background: white; border-radius: 16px; padding: 32px; border: 1px solid #e2e8f0;'>"
                + "    <p style='color: #1e293b; font-size: 16px; margin: 0 0 16px;'>Hello <strong>" + firstName
                + "</strong>,</p>"
                + "    <p style='color: #475569; font-size: 15px; line-height: 1.6; margin: 0 0 24px;'>"
                + "      The IT Manager has granted you access to the <strong>Medina ITManage platform</strong>. "
                + "      Click the button below to set your password and activate your account."
                + "    </p>"
                + "    <div style='text-align: center; margin: 32px 0;'>"
                + "      <a href='" + setPasswordLink
                + "' style='display: inline-block; background: linear-gradient(135deg, #3b82f6, #06b6d4); color: white; padding: 16px 36px; text-decoration: none; border-radius: 12px; font-weight: 700; font-size: 16px; letter-spacing: 0.01em;'>Set My Password →</a>"
                + "    </div>"
                + "    <p style='color: #94a3b8; font-size: 13px; text-align: center; margin: 0;'> This link is valid for <strong>7 days</strong>. After that, contact your IT Manager to resend the invitation.</p>"
                + "  </div>"
                + "  <p style='color: #94a3b8; font-size: 12px; text-align: center; margin: 24px 0 0;'>© Medina ITManage Platform – Do not reply to this email.</p>"
                + "</div>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }
}
