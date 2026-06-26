package com.finbridge.service;

import com.finbridge.dto.ConsultationResponse;
import com.finbridge.entity.Consultation;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.ConsultationRepository;
import com.finbridge.repository.UserRepository;
import com.finbridge.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import com.finbridge.entity.ConsultantPayment;
import com.finbridge.repository.ConsultantPaymentRepository;
import com.finbridge.repository.OrganizationUserRepository;
import com.finbridge.repository.ServiceRequestRepository;
import com.finbridge.service.NotificationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsultationService {
    private final ConsultationRepository consultationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ConsultantPaymentRepository consultantPaymentRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final com.finbridge.repository.ConsultationRecordingRepository consultationRecordingRepository;

    /** Returns consultations relevant to the given user based on their role. */
    @Transactional(readOnly = true)
    public List<ConsultationResponse> getForUser(User user) {
        List<Consultation> list = switch (user.getRole()) {
            case "consultant" -> consultationRepository.findByConsultantIdOrderByCreatedAtDesc(user.getId());
            case "department-admin" -> user.getDepartment() != null
                    ? consultationRepository.findByDepartmentOrderByCreatedAtDesc(user.getDepartment())
                    : consultationRepository.findAllByOrderByCreatedAtDesc();
            case "admin", "super-admin", "crm-admin" -> consultationRepository.findAllByOrderByCreatedAtDesc();
            default -> consultationRepository.findByClientIdOrderByCreatedAtDesc(user.getId());
        };
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getOne(@NonNull UUID id, User actor) {
        Consultation c = load(id);
        OwnershipGuard.assertConsultationAccess(actor, c.getClient(), c.getConsultant(), "consultation");
        return toResponse(c);
    }

    @Transactional
    public ConsultationResponse create(Consultation c) {
        return toResponse(consultationRepository.save(c));
    }

    @Transactional
    public ConsultationResponse accept(@NonNull UUID id, String confirmedDate, String confirmedTime, Boolean recordingEnabled, User actor) {
        Consultation c = load(id);
        OwnershipGuard.assertConsultationAccess(actor, c.getClient(), c.getConsultant(), "consultation");
        c.setStatus("accepted");
        validateScheduleDateTime(c, confirmedDate, confirmedTime);
        if (confirmedDate != null) c.setConfirmedDate(confirmedDate);
        if (confirmedTime != null) c.setConfirmedTime(confirmedTime);
        if (recordingEnabled != null) {
            com.finbridge.entity.ConsultationRecording rec = consultationRecordingRepository.findById(c.getId()).orElseGet(() -> {
                com.finbridge.entity.ConsultationRecording r = new com.finbridge.entity.ConsultationRecording();
                r.setConsultationId(c.getId());
                return r;
            });
            rec.setRecordingEnabled(recordingEnabled);
            consultationRecordingRepository.save(rec);
        }
        if (c.getMeetingLink() == null || c.getMeetingLink().isBlank()) {
            c.setMeetingLink("https://zoom.us/j/" + (1000000000L + new java.util.Random().nextLong(900000000L)));
        }
        return toResponse(consultationRepository.save(c));
    }

    @Transactional
    public ConsultationResponse assign(@NonNull UUID id, @NonNull UUID consultantId, User actor) {
        // Assigning a consultant is an internal staff action — never a client's.
        if (!OwnershipGuard.isAdminTier(actor))
            throw new AccessDeniedException("You do not have access to assign this consultation");
        Consultation c = load(id);
        User consultant = userRepository.findById(consultantId)
                .orElseThrow(() -> new ResourceNotFoundException("Consultant not found: " + consultantId));
        c.setConsultant(consultant);
        if ("pending".equals(c.getStatus())) c.setStatus("assigned");
        return toResponse(consultationRepository.save(c));
    }

    @Transactional
    public ConsultationResponse update(@NonNull UUID id, Consultation patch, User actor) {
        Consultation c = load(id);
        OwnershipGuard.assertConsultationAccess(actor, c.getClient(), c.getConsultant(), "consultation");
        validateScheduleDateTime(c, patch.getConfirmedDate(), patch.getConfirmedTime());
        if (patch.getStatus() != null) {
            c.setStatus(patch.getStatus());
            if ("accepted".equalsIgnoreCase(c.getStatus()) && (c.getMeetingLink() == null || c.getMeetingLink().isBlank())) {
                c.setMeetingLink("https://zoom.us/j/" + (1000000000L + new java.util.Random().nextLong(900000000L)));
            }
        }
        if (patch.getConfirmedDate() != null) c.setConfirmedDate(patch.getConfirmedDate());
        if (patch.getConfirmedTime() != null) c.setConfirmedTime(patch.getConfirmedTime());
        if (patch.getMeetingLink() != null) c.setMeetingLink(patch.getMeetingLink());
        return toResponse(consultationRepository.save(c));
    }

    @Transactional
    public ConsultationResponse schedule(@NonNull UUID id, String confirmedDate, String confirmedTime, String meetingLink, Boolean recordingEnabled, User actor) {
        Consultation c = load(id);
        OwnershipGuard.assertConsultationAccess(actor, c.getClient(), c.getConsultant(), "consultation");
        c.setStatus("scheduled");
        validateScheduleDateTime(c, confirmedDate, confirmedTime);
        if (confirmedDate != null) c.setConfirmedDate(confirmedDate);
        if (confirmedTime != null) c.setConfirmedTime(confirmedTime);
        if (meetingLink != null) c.setMeetingLink(meetingLink);
        if (recordingEnabled != null) {
            com.finbridge.entity.ConsultationRecording rec = consultationRecordingRepository.findById(c.getId()).orElseGet(() -> {
                com.finbridge.entity.ConsultationRecording r = new com.finbridge.entity.ConsultationRecording();
                r.setConsultationId(c.getId());
                return r;
            });
            rec.setRecordingEnabled(recordingEnabled);
            consultationRecordingRepository.save(rec);
        }
        return toResponse(consultationRepository.save(c));
    }

    @Transactional
    public ConsultationResponse sendToClient(@NonNull UUID id, User actor) {
        Consultation c = load(id);
        OwnershipGuard.assertConsultationAccess(actor, c.getClient(), c.getConsultant(), "consultation");
        c.setStatus("accepted");
        Consultation saved = consultationRepository.save(c);

        // Notify client
        notificationService.create(c.getClient(), "consultation", "Meeting Scheduled",
                "You have a meeting scheduled at " + c.getConfirmedDate() + " " + c.getConfirmedTime() +
                        " with advisor " + (c.getConsultant() != null ? c.getConsultant().getName() : "Advisor") +
                        ". Join Zoom Meeting here: " + c.getMeetingLink());
        
        return toResponse(saved);
    }

    @Transactional
    public ConsultationResponse complete(@NonNull UUID id, User actor) {
        Consultation c = load(id);
        OwnershipGuard.assertConsultationAccess(actor, c.getClient(), c.getConsultant(), "consultation");
        c.setStatus("completed_by_consultant");
        com.finbridge.entity.ConsultationRecording rec = consultationRecordingRepository.findById(c.getId()).orElse(null);
        if (rec != null && Boolean.TRUE.equals(rec.getRecordingEnabled())) {
            rec.setVideoUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4");
            consultationRecordingRepository.save(rec);
        }
        Consultation saved = consultationRepository.save(c);

        // Notify every department admin for this department.
        for (User admin : userRepository.findByRoleAndDepartmentAndActiveTrue("department-admin", c.getDepartment())) {
            notificationService.create(admin, "consultation", "Meeting Completed",
                    "Meeting with client " + c.getClient().getName() + " has been marked as completed by consultant " +
                            (c.getConsultant() != null ? c.getConsultant().getName() : "Advisor") + ". Please verify.");
        }
        return toResponse(saved);
    }

    @Transactional
    public ConsultationResponse verifyComplete(@NonNull UUID id, User actor) {
        // Only department admin (or higher admin) can verify
        if (!OwnershipGuard.isAdminTier(actor))
            throw new AccessDeniedException("You do not have access to verify this consultation");
        
        Consultation c = load(id);
        if (c.getConsultant() == null) {
            throw new com.finbridge.exception.BadRequestException("Cannot verify consultation: No consultant assigned to this consultation.");
        }
        c.setStatus("completed");
        Consultation saved = consultationRepository.save(c);

        // Update corresponding B2B ServiceRequest to completed
        organizationUserRepository.findByEmailIgnoreCase(c.getClient().getEmail()).ifPresent(orgUser -> {
            if (orgUser.getOrganization() != null) {
                UUID orgId = orgUser.getOrganization().getId();
                serviceRequestRepository.findByOrganizationIdAndActiveTrue(orgId).stream()
                        .filter(sr -> sr.getDepartmentId() != null && sr.getDepartmentId().equalsIgnoreCase(c.getDepartment()))
                        .forEach(sr -> {
                            sr.setStatus("completed");
                            serviceRequestRepository.save(sr);
                        });
            }
        });

        // Calculate and create ConsultantPayment
        BigDecimal fee = getFeeForDepartment(c.getDepartment());
        BigDecimal commission = fee.multiply(new BigDecimal("0.20"));
        
        ConsultantPayment payment = new ConsultantPayment();
        payment.setConsultant(c.getConsultant());
        payment.setConsultation(c);
        payment.setClientName(c.getClient().getName());
        payment.setDepartment(c.getDepartment());
        payment.setFeeAmount(fee);
        payment.setCommissionAmount(commission);
        payment.setStatus("paid");
        payment.setProcessedAt(Instant.now());
        consultantPaymentRepository.save(payment);

        // Notify consultant
        if (c.getConsultant() != null) {
            notificationService.create(c.getConsultant(), "commission", "Commission Paid",
                    "You received a commission of ₹" + commission.setScale(2, BigDecimal.ROUND_HALF_UP) + 
                            " for consultation with client " + c.getClient().getName());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ConsultationResponse> getCompletedByConsultantForAdmin(User admin) {
        String dept = admin.getDepartment();
        List<Consultation> list;
        if (dept != null && !dept.isBlank()) {
            list = consultationRepository.findByDepartmentOrderByCreatedAtDesc(dept);
        } else {
            list = consultationRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream()
                .filter(c -> "completed_by_consultant".equalsIgnoreCase(c.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPaymentsForConsultant(User consultant) {
        return consultantPaymentRepository.findByConsultantIdOrderByCreatedAtDesc(consultant.getId())
                .stream().map(p -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("clientName", p.getClientName());
                    m.put("department", p.getDepartment());
                    m.put("feeAmount", p.getFeeAmount());
                    m.put("commissionAmount", p.getCommissionAmount());
                    m.put("status", p.getStatus());
                    m.put("processedAt", p.getProcessedAt());
                    m.put("createdAt", p.getCreatedAt());
                    return m;
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPaymentsForAdmin(User admin) {
        String dept = admin.getDepartment();
        List<ConsultantPayment> list;
        if (dept != null && !dept.isBlank() && "department-admin".equalsIgnoreCase(admin.getRole())) {
            list = consultantPaymentRepository.findByDepartmentOrderByCreatedAtDesc(dept);
        } else {
            list = consultantPaymentRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(p -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("consultantName", p.getConsultant() != null ? p.getConsultant().getName() : "Advisor");
            m.put("consultantEmail", p.getConsultant() != null ? p.getConsultant().getEmail() : "");
            m.put("clientName", p.getClientName());
            m.put("department", p.getDepartment());
            m.put("feeAmount", p.getFeeAmount());
            m.put("commissionAmount", p.getCommissionAmount());
            m.put("status", p.getStatus());
            m.put("processedAt", p.getProcessedAt());
            m.put("createdAt", p.getCreatedAt());
            return m;
        }).toList();
    }

    private BigDecimal getFeeForDepartment(String dept) {
        if (dept == null) return new BigDecimal("2000");
        return switch (dept.toLowerCase()) {
            case "loans" -> new BigDecimal("5000");
            case "tax" -> new BigDecimal("2000");
            case "investments", "investment" -> new BigDecimal("3000");
            case "insurance" -> new BigDecimal("1500");
            case "wealth" -> new BigDecimal("10000");
            default -> new BigDecimal("2000");
        };
    }

    private Consultation load(@NonNull UUID id) {
        return consultationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation not found: " + id));
    }

    private ConsultationResponse toResponse(Consultation c) {
        ConsultationResponse.Ref client = c.getClient() != null
                ? new ConsultationResponse.Ref(c.getClient().getId(), c.getClient().getName(),
                    c.getClient().getEmail(), c.getClient().getCompanyName()) : null;
        ConsultationResponse.Ref consultant = c.getConsultant() != null
                ? new ConsultationResponse.Ref(c.getConsultant().getId(), c.getConsultant().getName(),
                    c.getConsultant().getEmail(), c.getConsultant().getCompanyName()) : null;
        com.finbridge.entity.ConsultationRecording rec = consultationRecordingRepository.findById(c.getId()).orElse(null);
        Boolean recordingEnabled = rec != null ? rec.getRecordingEnabled() : false;
        String videoUrl = rec != null ? rec.getVideoUrl() : "";
        return new ConsultationResponse(
                c.getId(), client, consultant, c.getDepartment(), c.getCategory(),
                c.getStatus(), c.getClientNotes(), c.getConfirmedDate(), c.getConfirmedTime(),
                c.getMeetingLink(), c.getCreatedAt(), recordingEnabled, videoUrl
        );
    }

    private void validateScheduleDateTime(Consultation c, String confirmedDate, String confirmedTime) {
        String finalDate = confirmedDate != null ? confirmedDate : c.getConfirmedDate();
        String finalTime = confirmedTime != null ? confirmedTime : c.getConfirmedTime();

        if (finalDate == null || finalDate.isBlank()) return;

        // 1. Date is today or in the future
        try {
            java.time.LocalDate parsedDate = java.time.LocalDate.parse(finalDate.trim());
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.systemDefault());
            if (parsedDate.isBefore(today)) {
                throw new com.finbridge.exception.BadRequestException("Cannot schedule a meeting for a past date.");
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new com.finbridge.exception.BadRequestException("Invalid date format. Expected YYYY-MM-DD.");
        }

        // 2. Time is between 10:00 AM and 10:00 PM
        if (finalTime == null || finalTime.isBlank()) return;
        try {
            java.time.LocalTime parsedTime = java.time.LocalTime.parse(finalTime.trim());
            java.time.LocalTime start = java.time.LocalTime.of(10, 0);
            java.time.LocalTime end = java.time.LocalTime.of(22, 0);
            if (parsedTime.isBefore(start) || parsedTime.isAfter(end)) {
                throw new com.finbridge.exception.BadRequestException("Meetings can only be scheduled between 10:00 AM and 10:00 PM.");
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new com.finbridge.exception.BadRequestException("Invalid time format. Expected HH:mm.");
        }
    }
}
