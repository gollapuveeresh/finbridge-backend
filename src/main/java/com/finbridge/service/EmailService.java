package com.finbridge.service;

import com.finbridge.entity.EmailNotification;
import com.finbridge.repository.EmailNotificationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailNotificationRepository emailNotificationRepository;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.from:}")
    private String mailFrom;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ─── Core sender ─────────────────────────────────────────────────────────

    /**
     * Send an HTML email with dedup protection and audit logging.
     * Silently skips if SMTP is not configured (empty MAIL_USERNAME).
     */
    @Async
    public void sendHtml(String to, String subject, String htmlBody, String type, UUID relatedEntityId) {
        if (to == null || to.isBlank())
            return;
        if (mailUsername == null || mailUsername.isBlank()) {
            log.warn("SMTP not configured — skipping email [{}] to {}", type, to);
            return;
        }

        // Dedup: don't send the same (recipient, type, entity) twice
        if (relatedEntityId != null &&
                emailNotificationRepository.existsByRecipientAndTypeAndRelatedEntityIdAndStatus(to, type,
                        relatedEntityId, "sent")) {
            log.info("Duplicate email suppressed: type={}, to={}, entity={}", type, to, relatedEntityId);
            return;
        }

        EmailNotification record = new EmailNotification();
        record.setRecipient(to);
        record.setType(type);
        record.setRelatedEntityId(relatedEntityId);
        record.setSubject(subject);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            String fromEmail = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : mailUsername;
            helper.setFrom(fromEmail, "FinBridge");
            mailSender.send(message);

            record.setStatus("sent");
            record.setSentAt(Instant.now());
            log.info("✉ Email sent [{}] to {}: {}", type, to, subject);
        } catch (Exception e) {
            record.setStatus("failed");
            record.setErrorMessage(
                    e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                            : "Unknown error");
            log.error("✉ Email FAILED [{}] to {}: {}", type, to, e.getMessage());
        }

        try {
            emailNotificationRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to save email notification record: {}", e.getMessage());
        }
    }

    // ─── Trigger Methods ─────────────────────────────────────────────────────

    @Async
    public void sendConsultantAssigned(String clientEmail, String clientName, String consultantName, String department,
            UUID leadId) {
        String dept = capitalize(department);
        String body = buildEmailHtml(
                "Consultant Assigned",
                "<p>Hi <strong>" + esc(clientName) + "</strong>,</p>"
                        + "<p>A dedicated consultant has been assigned to assist you with your <strong>" + dept
                        + "</strong> services.</p>"
                        + "<table style='margin:20px 0;border-collapse:collapse'>"
                        + row("Consultant", consultantName)
                        + row("Department", dept)
                        + "</table>"
                        + "<p>Your consultant will reach out to you shortly. You can also log in to your dashboard to view details and schedule a consultation.</p>",
                "Go to Dashboard",
                frontendUrl + "/client/dashboard");
        sendHtml(clientEmail, "Consultant Assigned — " + dept + " | FinBridge", body, "consultant_assigned", leadId);
    }

    @Async
    public void sendProposalCreated(String clientEmail, String clientName, String proposalTitle, String department,
            UUID proposalId) {
        String dept = capitalize(department);
        String body = buildEmailHtml(
                "New Proposal",
                "<p>Hi <strong>" + esc(clientName) + "</strong>,</p>"
                        + "<p>A new proposal has been prepared for you:</p>"
                        + "<table style='margin:20px 0;border-collapse:collapse'>"
                        + row("Proposal", proposalTitle)
                        + row("Department", dept)
                        + "</table>"
                        + "<p>Please review the proposal details and let us know your decision.</p>",
                "View Proposal",
                frontendUrl + "/client/proposals");
        sendHtml(clientEmail, "New Proposal: " + proposalTitle + " | FinBridge", body, "proposal_created", proposalId);
    }

    @Async
    public void sendProposalDecision(String clientEmail, String clientName, String proposalTitle, String status,
            UUID proposalId) {
        boolean approved = "approved".equalsIgnoreCase(status);
        String statusLabel = approved ? "Approved ✓" : "Update: " + capitalize(status);
        String color = approved ? "#22c55e" : "#ef4444";
        String body = buildEmailHtml(
                "Proposal " + statusLabel,
                "<p>Hi <strong>" + esc(clientName) + "</strong>,</p>"
                        + "<p>Your proposal <strong>" + esc(proposalTitle) + "</strong> has been "
                        + "<span style='color:" + color + ";font-weight:bold'>" + status + "</span>.</p>"
                        + (approved
                                ? "<p>Our team will now proceed with the next steps. You'll receive further updates shortly.</p>"
                                : "<p>Please check your dashboard for details or contact your consultant.</p>"),
                "View Details",
                frontendUrl + "/client/proposals");
        sendHtml(clientEmail, "Proposal " + statusLabel + ": " + proposalTitle + " | FinBridge", body,
                "proposal_" + status, proposalId);
    }

    @Async
    public void sendPaymentRequested(String clientEmail, String clientName, String invoiceNumber,
            BigDecimal amount, Instant dueDate, UUID invoiceId) {
        String dueDateStr = dueDate != null
                ? DateTimeFormatter.ofPattern("dd MMM yyyy").format(dueDate.atZone(ZoneId.systemDefault()))
                : "As soon as possible";
        String amountStr = amount != null ? "₹" + amount.toPlainString() : "—";
        String body = buildEmailHtml(
                "Payment Requested",
                "<p>Hi <strong>" + esc(clientName) + "</strong>,</p>"
                        + "<p>An invoice has been generated for your services:</p>"
                        + "<table style='margin:20px 0;border-collapse:collapse'>"
                        + row("Invoice #", invoiceNumber)
                        + row("Amount", amountStr)
                        + row("Due Date", dueDateStr)
                        + "</table>"
                        + "<p>Please make the payment at your earliest convenience to continue with your services.</p>",
                "Make Payment",
                frontendUrl + "/client/payments");
        sendHtml(clientEmail, "Invoice " + invoiceNumber + " — Payment Requested | FinBridge", body,
                "payment_requested", invoiceId);
    }

    @Async
    public void sendPaymentCompleted(String clientEmail, String clientName, String invoiceNumber,
            BigDecimal amount, UUID paymentId) {
        String amountStr = amount != null ? "₹" + amount.toPlainString() : "—";
        String body = buildEmailHtml(
                "Payment Confirmed ✓",
                "<p>Hi <strong>" + esc(clientName) + "</strong>,</p>"
                        + "<p>We've received your payment. Thank you!</p>"
                        + "<table style='margin:20px 0;border-collapse:collapse'>"
                        + row("Invoice #", invoiceNumber)
                        + row("Amount Paid", amountStr)
                        + row("Status", "<span style='color:#22c55e;font-weight:bold'>Paid</span>")
                        + "</table>"
                        + "<p>You can view your payment history and download receipts from your dashboard.</p>",
                "View Payments",
                frontendUrl + "/client/payments");
        sendHtml(clientEmail, "Payment Confirmed — " + invoiceNumber + " | FinBridge", body, "payment_completed",
                paymentId);
    }

    @Async
    public void sendPasswordReset(String to, String resetLink) {
        String body = buildEmailHtml(
                "Reset Your Password",
                "<p>We received a request to reset your password.</p>"
                        + "<p>Click the button below to create a new password. This link expires in <strong>1 hour</strong>.</p>"
                        + "<p style='color:#9ca3af;font-size:13px;margin-top:20px'>If you didn't request this, you can safely ignore this email.</p>",
                "Reset Password",
                resetLink);
        sendHtml(to, "Reset Your Password | FinBridge", body, "password_reset", null);
    }

    @Async
    public void sendVerificationEmail(String to, String name, String verifyLink) {
        String body = buildEmailHtml(
                "Verify Your Email Address",
                "<p>Hi <strong>" + esc(name) + "</strong>,</p>"
                        + "<p>Thank you for registering with FinBridge!</p>"
                        + "<p>Please verify your email address by clicking the button below. This link expires in <strong>24 hours</strong>.</p>"
                        + "<p style='color:#9ca3af;font-size:13px;margin-top:20px'>If you didn't create an account, you can safely ignore this email.</p>",
                "Verify Email",
                verifyLink);
        sendHtml(to, "Verify Your Email | FinBridge", body, "email_verification", null);
    }

    public String buildNotificationEmailBody(String name, String title, String messageHtml) {
        return buildEmailHtml(
                title,
                "<p>Hi <strong>" + esc(name) + "</strong>,</p>"
                        + "<p>" + messageHtml + "</p>"
                        + "<p>Please log in to your portal/dashboard to view details.</p>",
                "Go to Portal",
                frontendUrl + "/");
    }

    @Async
    public void sendNotificationEmail(String to, String name, String title, String message, String type, UUID relatedEntityId) {
        String body = buildNotificationEmailBody(name, title, esc(message));
        sendHtml(to, title + " | FinBridge", body, "notification_" + type, relatedEntityId);
    }

    // ─── HTML Template ───────────────────────────────────────────────────────

    private String buildEmailHtml(String title, String contentHtml, String ctaText, String ctaUrl) {
        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:#f1f5f9;font-family:Inter,Arial,sans-serif'>"
                + "<div style='max-width:600px;margin:40px auto;background:#0A192F;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.15)'>"
                // Header
                + "<div style='background:linear-gradient(135deg,#0A192F 0%,#112240 100%);padding:32px 40px;border-bottom:2px solid #D4AF37'>"
                + "<h1 style='margin:0;color:#D4AF37;font-size:20px;letter-spacing:1px'>FINBRIDGE</h1>"
                + "<p style='margin:4px 0 0;color:#8892b0;font-size:12px;letter-spacing:2px'>PREMIUM FINANCIAL SOLUTIONS</p>"
                + "</div>"
                // Body
                + "<div style='padding:36px 40px;color:#ccd6f6;font-size:15px;line-height:1.7'>"
                + "<h2 style='margin:0 0 20px;color:#e6f1ff;font-size:22px'>" + esc(title) + "</h2>"
                + contentHtml
                // CTA Button
                + (ctaText != null && ctaUrl != null
                        ? "<div style='text-align:center;margin:32px 0 16px'>"
                                + "<a href='" + ctaUrl
                                + "' style='display:inline-block;padding:14px 36px;background:#D4AF37;color:#0A192F;font-weight:700;font-size:14px;text-decoration:none;border-radius:8px;letter-spacing:0.5px'>"
                                + esc(ctaText) + "</a></div>"
                        : "")
                + "</div>"
                // Footer
                + "<div style='padding:20px 40px;background:#0d2137;border-top:1px solid rgba(212,175,55,0.2);text-align:center'>"
                + "<p style='margin:0;color:#4a5568;font-size:12px'>© 2026 FinBridge Solutions. All rights reserved.</p>"
                + "<p style='margin:6px 0 0;color:#4a5568;font-size:11px'>This is an automated notification. Please do not reply to this email.</p>"
                + "</div>"
                + "</div></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr>"
                + "<td style='padding:8px 16px 8px 0;color:#8892b0;font-size:13px;white-space:nowrap'>" + esc(label)
                + "</td>"
                + "<td style='padding:8px 0;color:#e6f1ff;font-size:14px;font-weight:600'>"
                + (value != null ? value : "—") + "</td>"
                + "</tr>";
    }

    private static String esc(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank())
            return "General";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
