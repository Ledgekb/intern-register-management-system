package com.internregister.service;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.*;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

        @Autowired(required = false)
        private JavaMailSender mailSender;

        @Autowired(required = false)
        private EmailClient emailClient;

        @Value("${mail.from.address:UnivenSupport@univen.ac.za}")
        private String mailFromAddress;

        @Value("${azure.communication.sender-address:}")
        private String senderAddress;

        @Value("${mail.enabled:true}")
        private boolean mailEnabled;

        @Value("${app.system.url}")
        private String systemUrl;

        public EmailService() {
        }

        public EmailService(EmailClient emailClient) {
                this.emailClient = emailClient;
        }

        public void sendVerificationCode(String email, String code) {
                System.out.println("=====================================================");
                System.out.println("📧 SMTP TRIGGERED - SENDING EMAIL TO: " + email);
                System.out.println("🔐 VERIFICATION CODE: " + code);
                System.out.println("=====================================================");

                String subject = "Your Verification Code - Intern Register System";
                String bodyText = "Your verification code is: " + code + "\n\nThis code will expire in 1 minute.";
                String bodyHtml = "<html><body>" +
                                "<h2>Verification Code</h2>" +
                                "<p>Your verification code for the Intern Register System is:</p>" +
                                "<h1 style='color: #0078d4;'>" + code + "</h1>" +
                                "<p>This code will expire in 1 minute.</p>" +
                                "</body></html>";

                sendEmail(email, subject, bodyText, bodyHtml);
        }

        public void sendInternInvite(String email, String name, String password) {
                String defaultMessage = String.format(
                                "You have been successfully registered as an intern in the Intern Register System.\n\n"
                                                +
                                                "To access the system, please follow the link below:\n" +
                                                "🔗 %s\n\n" +
                                                "Your login credentials are:\n" +
                                                "📧 Username: %s\n" +
                                                "🔑 Password: %s\n\n" +
                                                "⚠️ IMPORTANT: For security reasons, please log in and change your password immediately after your first login.",
                                systemUrl, email, password);
                sendInternInviteWithCustomMessage(email, name, defaultMessage);
        }

        public void sendInternInviteWithCustomMessage(String email, String name, String messageContent) {
                String subject = "Welcome to the Intern Register System - Action Required";
                String plainText = String.format(
                                "Dear %s,\n\n%s\n\nBest regards,\nIntern Register System Team",
                                name, messageContent);

                String htmlContent = String.format(
                                "<html><body>" +
                                                "<h3>Dear %s,</h3>" +
                                                "<p>%s</p>" +
                                                "<br>" +
                                                "<p>Best regards,<br>Intern Register System Team</p>" +
                                                "</body></html>",
                                name, messageContent.replace("\n", "<br>"));

                sendEmail(email, subject, plainText, htmlContent);
        }

        public void sendAdminInvite(String email, String name, String password) {
                String subject = "Administrator Invitation - Intern Register System";
                String message = String.format(
                                "You have been appointed as an Administrator in the Intern Register System.\n\n" +
                                                "Access Link: %s\n" +
                                                "Email: %s\n\n" +
                                                "You can log in using your Univen staff credentials (email and password).\n\n"
                                                +
                                                "Alternatively, if you haven't set up your Univen credentials yet, you can use this temporary password: %s",
                                systemUrl, email, password);

                String htmlContent = String.format(
                                "<html><body style='font-family: Arial, sans-serif;'>" +
                                                "<h2>Administrator Access</h2>" +
                                                "<p>Dear %s,</p>" +
                                                "<p>You have been appointed as an Administrator in the <b>Intern Register System</b>.</p>"
                                                +
                                                "<p>Please log in using your <b>Univen staff credentials</b>:</p>" +
                                                "<ul>" +
                                                "  <li><b>Email:</b> %s</li>" +
                                                "</ul>" +
                                                "<p>If you prefer to use a temporary system-generated password, you may use: <code>%s</code></p>"
                                                +
                                                "<p><a href='%s' style='background-color: #002060; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Login to System</a></p>"
                                                +
                                                "<p>Best regards,<br>Intern Register System Team</p>" +
                                                "</body></html>",
                                name, email, password, systemUrl);

                sendEmail(email, subject, message, htmlContent);
        }

        public void sendAdminInvite(String email, String name, String password, String inviteLink, String customMessage) {
                String subject = "Administrator Invitation - Intern Register System";
                String plainText = String.format(
                                "Dear %s,\n\n%s\n\nAccess Link: %s\nTemporary Password: %s\n\nBest regards,\nIntern Register System Team",
                                name, customMessage, inviteLink, password);

                String htmlContent = String.format(
                                "<html><body style='font-family: Arial, sans-serif;'>" +
                                                "<h2>Administrator Access</h2>" +
                                                "<p>Dear %s,</p>" +
                                                "<p>%s</p>" +
                                                "<p><b>Access Link:</b> <a href='%s'>%s</a></p>" +
                                                "<p><b>Temporary Password:</b> <code>%s</code></p>" +
                                                "<br><p>Best regards,<br>Intern Register System Team</p></body></html>",
                                name, customMessage.replace("\n", "<br>"), inviteLink, inviteLink, password);

                sendEmail(email, subject, plainText, htmlContent);
        }

        public void sendAdminWelcome(String email, String name) {
                String subject = "Welcome to the Intern Register System";
                String message = String.format(
                                "You have been appointed as an Administrator in the Intern Register System.\n\n" +
                                                "Access Link: %s\n" +
                                                "Email: %s\n\n" +
                                                "Please log in using your standard Univen staff credentials.",
                                systemUrl, email);

                String htmlContent = String.format(
                                "<html><body style='font-family: Arial, sans-serif;'>" +
                                                "<h2>Administrator Profile Created</h2>" +
                                                "<p>Dear %s,</p>" +
                                                "<p>You have been appointed as an Administrator in the <b>Intern Register System</b>.</p>"
                                                +
                                                "<p>You can now log in using your <b>Univen staff credentials</b>:</p>"
                                                +
                                                "<ul>" +
                                                "  <li><b>Login URL:</b> <a href='%s'>%s</a></li>" +
                                                "  <li><b>Username:</b> %s</li>" +
                                                "</ul>" +
                                                "<br>" +
                                                "<p>Best regards,<br>Intern Register System Team</p>" +
                                                "</body></html>",
                                name, systemUrl, systemUrl, email);

                sendEmail(email, subject, message, htmlContent);
        }

        public void sendSupervisorInvite(String email, String name, String password) {
                String subject = "Supervisor Invitation - Intern Register System";
                String message = String.format(
                                "You have been registered as a Supervisor in the Intern Register System.\n\n" +
                                                "Access Link: %s\n" +
                                                "Email: %s\n\n" +
                                                "You can log in using your Univen staff credentials (email and password).\n\n"
                                                +
                                                "Alternatively, you can use this temporary password: %s",
                                systemUrl, email, password);

                String htmlContent = String.format(
                                "<html><body style='font-family: Arial, sans-serif;'>" +
                                                "<h2>Supervisor Access</h2>" +
                                                "<p>Dear %s,</p>" +
                                                "<p>You have been registered as a Supervisor in the <b>Intern Register System</b>.</p>"
                                                +
                                                "<p>Please log in using your <b>Univen staff credentials</b>:</p>" +
                                                "<ul>" +
                                                "  <li><b>Email:</b> %s</li>" +
                                                "</ul>" +
                                                "<p>Or use this temporary system password: <code>%s</code></p>" +
                                                "<p><a href='%s' style='background-color: #002060; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;'>Login to System</a></p>"
                                                +
                                                "<p>Best regards,<br>Intern Register System Team</p>" +
                                                "</body></html>",
                                name, email, password, systemUrl);

                sendEmail(email, subject, message, htmlContent);
        }

        public void sendSupervisorInvite(String email, String name, String password, String inviteLink, String customMessage) {
                String subject = "Supervisor Invitation - Intern Register System";
                String plainText = String.format(
                                "Dear %s,\n\n%s\n\nAccess Link: %s\n%s\n\nBest regards,\nIntern Register System Team",
                                name,
                                customMessage,
                                inviteLink,
                                password != null && !password.trim().isEmpty() ? "Temporary Password: " + password : "");

                String htmlContent = String.format(
                                "<html><body style='font-family: Arial, sans-serif;'>" +
                                                "<h3>Dear %s,</h3>" +
                                                "<p>%s</p>" +
                                                "<p><b>Access Link:</b> <a href='%s'>%s</a></p>" +
                                                (password != null && !password.trim().isEmpty() ? "<p><b>Temporary Password:</b> <code>" + password + "</code></p>" : "") +
                                                "<br><p>Best regards,<br>Intern Register System Team</p></body></html>",
                                name,
                                customMessage.replace("\n", "<br>"),
                                inviteLink,
                                inviteLink);

                sendEmail(email, subject, plainText, htmlContent);
        }

        public void sendSupervisorWelcome(String email, String name) {
                String subject = "Welcome to the Intern Register System";
                String message = String.format(
                                "You have been registered as a Supervisor in the Intern Register System.\n\n" +
                                                "Access Link: %s\n" +
                                                "Email: %s\n\n" +
                                                "Please log in using your standard Univen staff credentials.",
                                systemUrl, email);

                String htmlContent = String.format(
                                "<html><body style='font-family: Arial, sans-serif;'>" +
                                                "<h2>Supervisor Profile Created</h2>" +
                                                "<p>Dear %s,</p>" +
                                                "<p>You have been registered as a Supervisor in the <b>Intern Register System</b>.</p>"
                                                +
                                                "<p>You can now log in using your <b>Univen staff credentials</b>:</p>"
                                                +
                                                "<ul>" +
                                                "  <li><b>Login URL:</b> <a href='%s'>%s</a></li>" +
                                                "  <li><b>Username:</b> %s</li>" +
                                                "</ul>" +
                                                "<br>" +
                                                "<p>Best regards,<br>Intern Register System Team</p>" +
                                                "</body></html>",
                                name, systemUrl, systemUrl, email);

                sendEmail(email, subject, message, htmlContent);
        }

        private void sendEmail(String to, String subject, String plainText, String htmlContent) {
                if (!mailEnabled) {
                        System.out.println("===========================================");
                        System.out.println("EMAIL SIMULATION (mail.enabled=false)");
                        System.out.println("TO: " + to);
                        System.out.println("SUBJECT: " + subject);
                        System.out.println("BODY:\n" + plainText);
                        System.out.println("===========================================");
                        return;
                }

                boolean sent = false;

                // 1. Try standard SMTP via Spring JavaMailSender
                if (mailSender != null) {
                        try {
                                MimeMessage mimeMessage = mailSender.createMimeMessage();
                                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                                helper.setFrom(mailFromAddress);
                                helper.setTo(to);
                                helper.setSubject(subject);
                                helper.setText(plainText, htmlContent);

                                mailSender.send(mimeMessage);
                                System.out.println("✓ Email sent successfully via SMTP to: " + to);
                                sent = true;
                        } catch (Exception e) {
                                System.err.println("⚠️ SMTP send failed to " + to + ": " + e.getMessage());
                                e.printStackTrace();
                        }
                }

                // 2. Fallback to Azure Communication Services if SMTP failed or not available
                if (!sent && emailClient != null && senderAddress != null && !senderAddress.trim().isEmpty()) {
                        try {
                                EmailAddress toAddress = new EmailAddress(to);
                                EmailMessage emailMessage = new EmailMessage()
                                                .setSenderAddress(senderAddress)
                                                .setToRecipients(toAddress)
                                                .setSubject(subject)
                                                .setBodyPlainText(plainText)
                                                .setBodyHtml(htmlContent);

                                SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
                                PollResponse<EmailSendResult> result = poller.waitForCompletion();

                                if (result.getValue().getStatus() == EmailSendStatus.SUCCEEDED) {
                                        System.out.println("✓ Email sent successfully via Azure to: " + to);
                                        sent = true;
                                } else {
                                        System.err.println("❌ Failed to send email via Azure to " + to + ". Status: "
                                                        + result.getValue().getStatus());
                                }
                        } catch (Exception e) {
                                System.err.println("❌ Error sending email via Azure: " + e.getMessage());
                        }
                }

                if (!sent && mailSender == null && emailClient == null) {
                        System.err.println("❌ Neither JavaMailSender nor EmailClient is configured. Cannot send email to " + to);
                }
        }
}
