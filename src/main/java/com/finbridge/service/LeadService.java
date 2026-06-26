package com.finbridge.service;

import com.finbridge.entity.Lead;
import com.finbridge.entity.LeadNote;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.LeadRepository;
import com.finbridge.repository.UserRepository;
import com.finbridge.repository.OrganizationRepository;
import com.finbridge.repository.OrganizationUserRepository;
import com.finbridge.repository.ServiceRequestRepository;
import com.finbridge.entity.Organization;
import com.finbridge.entity.ServiceRequest;
import com.finbridge.entity.OrganizationProposal;
import com.finbridge.entity.OrganizationUser;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.finbridge.repository.OrganizationProposalRepository;
import com.finbridge.entity.Consultation;
import com.finbridge.repository.ConsultationRepository;
import java.time.LocalDate;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final SequenceGenerator sequenceGenerator;
    private final OrganizationRepository organizationRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final OrganizationProposalRepository organizationProposalRepository;
    private final ConsultationRepository consultationRepository;
    private final EmailService emailService;

    /** Result of converting a lead into a client account. */
    public record ConversionResult(boolean isNewClient, String tempPassword, User client) {
    }

    public List<Lead> getAll() {
        return leadRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    public String getPhoneByEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        return leadRepository.findByEmailIgnoreCase(email).stream()
                .map(Lead::getPhone)
                .filter(p -> p != null && !p.isBlank())
                .findFirst()
                .orElse("");
    }

    public List<Lead> getByDepartment(String dept) {
        return leadRepository.findByDepartmentAndActiveTrue(dept);
    }

    /** Filtered, non-paginated list for the CRM/department views. */
    @Transactional(readOnly = true)
    public List<Lead> getFiltered(String department, String status) {
        return leadRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .filter(l -> department == null || department.equalsIgnoreCase(l.getDepartment()))
                .filter(l -> status == null || status.equalsIgnoreCase(l.getStatus()))
                .toList();
    }

    public Page<Lead> getAll(String department, String status, Pageable pageable) {
        if (department != null && status != null)
            return leadRepository.findByDepartmentAndStatusAndActiveTrue(department, status, pageable);
        if (department != null)
            return leadRepository.findByDepartmentAndActiveTrue(department, pageable);
        if (status != null)
            return leadRepository.findByStatusAndActiveTrue(status, pageable);
        return leadRepository.findByActiveTrue(pageable);
    }

    public Lead getById(UUID id) {
        return leadRepository.findById(id)
                .or(() -> userRepository.findById(id)
                        .flatMap(user -> leadRepository.findByEmailIgnoreCase(user.getEmail()).stream().findFirst()))
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }

    @Transactional
    public Lead create(Lead lead) {
        lead.setLeadId(sequenceGenerator.next(SequenceGenerator.Seq.LEAD));
        lead.setScore(calculateScore(lead));
        lead.setPriority(scoreToPriority(lead.getScore()));
        Lead saved = leadRepository.save(lead);
        log.info("Lead created: {} score={} priority={}", saved.getLeadId(), saved.getScore(), saved.getPriority());

        try {
            for (User crmAdmin : userRepository.findByRoleAndActiveTrue("crm-admin")) {
                notificationService.create(crmAdmin, "lead", "New Lead Created",
                        "A new lead has been created: " + saved.getName() + " (" + saved.getLeadId() + ") for department: " + saved.getDepartment() + ".");
            }
        } catch (Exception e) {
            log.error("Failed to notify CRM admins of new lead: {}", e.getMessage());
        }

        return saved;
    }

    @Transactional
    public Lead update(UUID id, com.finbridge.dto.LeadUpdateRequest patch) {
        Lead lead = getById(id);
        if (patch.name() != null)
            lead.setName(patch.name());
        if (patch.email() != null)
            lead.setEmail(patch.email());
        if (patch.phone() != null)
            lead.setPhone(patch.phone());
        if (patch.income() != null)
            lead.setIncome(patch.income());
        if (patch.requirement() != null)
            lead.setRequirement(patch.requirement());
        if (patch.budget() != null)
            lead.setBudget(patch.budget());
        if (patch.source() != null)
            lead.setSource(patch.source());
        if (patch.serviceType() != null)
            lead.setServiceType(patch.serviceType());
        if (patch.status() != null) {
            lead.setStatus(patch.status());
            if ("qualified".equals(patch.status()) || "new".equals(patch.status())
                    || "pending_fee".equals(patch.status())) {
                lead.setAssignedConsultant(null);
            }
        }
        if (patch.department() != null)
            lead.setDepartment(normalizeDepartment(patch.department()));
        if (patch.priority() != null)
            lead.setPriority(patch.priority());
        if (patch.score() != null)
            lead.setScore(patch.score());
        if (patch.assignedConsultant() != null) {
            userRepository.findById(patch.assignedConsultant()).ifPresent(consultant -> {
                lead.setAssignedConsultant(consultant);
                try {
                    notificationService.create(consultant, "lead", "Lead Assigned",
                            "You have been assigned as the consultant for Lead: " + lead.getName() + " (" + lead.getLeadId() + ").");
                } catch (Exception e) {
                    log.error("Failed to notify consultant of assignment: {}", e.getMessage());
                }
                organizationUserRepository.findByEmailIgnoreCase(lead.getEmail()).ifPresent(orgUser -> {
                    Organization org = orgUser.getOrganization();
                    if (org != null) {
                        List<ServiceRequest> requests = serviceRequestRepository
                                .findByOrganizationIdAndActiveTrue(org.getId());
                        for (ServiceRequest sr : requests) {
                            if (sr.getDepartmentId() != null
                                    && sr.getDepartmentId().equalsIgnoreCase(lead.getDepartment())) {
                                sr.setConsultant(consultant);
                                serviceRequestRepository.save(sr);
                                log.info("Assigned consultant {} to Service Request {} (dept: {})",
                                        consultant.getName(), sr.getRequestNumber(), sr.getDepartmentId());
                            }
                        }
                    }
                });

                // Automatically ensure a client User exists and has a pending Consultation
                // scheduled
                User clientUser = userRepository.findByEmailIgnoreCase(lead.getEmail()).orElseGet(() -> {
                    User u = new User();
                    u.setName(lead.getName());
                    u.setEmail(lead.getEmail());
                    u.setPhone(lead.getPhone());
                    u.setRole("client");
                    u.setDepartment(lead.getDepartment());
                    u.setPassword(
                            passwordEncoder.encode("FB" + java.util.UUID.randomUUID().toString().substring(0, 8)));
                    return userRepository.save(u);
                });

                boolean hasConsultation = consultationRepository.findByClientIdOrderByCreatedAtDesc(clientUser.getId())
                        .stream()
                        .anyMatch(c -> c.getDepartment() != null
                                && c.getDepartment().equalsIgnoreCase(lead.getDepartment()));

                if (!hasConsultation) {
                    Consultation consultation = new Consultation();
                    consultation.setClient(clientUser);
                    consultation.setConsultant(consultant);
                    consultation.setDepartment(lead.getDepartment());
                    consultation.setCategory(getDepartmentName(lead.getDepartment()) + " Consultation");
                    consultation.setStatus("pending");
                    consultation.setClientNotes(lead.getRequirement() != null ? lead.getRequirement() : "");
                    consultationRepository.save(consultation);
                    log.info("Created pending Consultation for client {} and consultant {}", clientUser.getEmail(),
                            consultant.getName());
                } else {
                    consultationRepository.findByClientIdOrderByCreatedAtDesc(clientUser.getId()).stream()
                            .filter(c -> c.getDepartment() != null
                                    && c.getDepartment().equalsIgnoreCase(lead.getDepartment()))
                            .forEach(c -> {
                                c.setConsultant(consultant);
                                if (!"accepted".equalsIgnoreCase(c.getStatus())) {
                                    c.setStatus("pending");
                                }
                                consultationRepository.save(c);
                                log.info("Updated Consultation consultant to {} for client {}", consultant.getName(),
                                        clientUser.getEmail());
                            });
                }
            });

            // Send email notification to client about assigned consultant
            emailService.sendConsultantAssigned(
                    lead.getEmail(), lead.getName(),
                    userRepository.findById(patch.assignedConsultant()).map(User::getName).orElse("Your Consultant"),
                    lead.getDepartment(), lead.getId());
        }
        if (patch.followUpDate() != null)
            lead.setFollowUpDate(patch.followUpDate());
        return leadRepository.save(lead);
    }

    @Transactional
    public Lead addNote(UUID id, String text, String addedBy) {
        Lead lead = getById(id);
        LeadNote note = new LeadNote();
        note.setLead(lead);
        note.setText(text);
        note.setAddedBy(addedBy);
        note.setAddedAt(Instant.now());
        lead.getNotes().add(note);
        return leadRepository.save(lead);
    }

    /**
     * Routes a lead to a department, marks it assigned, logs a note, and notifies
     * the dept admins.
     */
    @Transactional
    public Lead sendToDepartment(UUID id, String department, String notes, String actorName) {
        String normalizedDept = normalizeDepartment(department);
        Lead lead = getById(id);
        lead.setDepartment(normalizedDept);
        if (!"won".equals(lead.getStatus()))
            lead.setStatus("assigned");
        if (notes != null && !notes.isBlank()) {
            LeadNote note = new LeadNote();
            note.setLead(lead);
            note.setText("Routed to " + normalizedDept + ": " + notes);
            note.setAddedBy(actorName != null ? actorName : "CRM");
            note.setAddedAt(Instant.now());
            lead.getNotes().add(note);
        }
        Lead saved = leadRepository.save(lead);

        // Notify every department admin for this department.
        for (User admin : userRepository.findByRoleAndDepartmentAndActiveTrue("department-admin", normalizedDept)) {
            notificationService.create(admin, "lead",
                    "New lead routed to " + normalizedDept,
                    "Lead " + lead.getName() + " (" + lead.getLeadId() + ") has been assigned to your department.");
        }
        log.info("Lead {} routed to department {}", lead.getLeadId(), normalizedDept);
        return saved;
    }

    /**
     * Converts a lead into a client user account (creating one if needed) and marks
     * it won.
     */
    @Transactional
    public ConversionResult convertToClient(UUID id) {
        Lead lead = getById(id);
        lead.setStatus("won");

        // If this lead corresponds to a B2B organization registration, activate the
        // organization.
        organizationUserRepository.findByEmailIgnoreCase(lead.getEmail()).ifPresent(orgUser -> {
            Organization org = orgUser.getOrganization();
            if (org != null) {
                org.setStatus("active");
                organizationRepository.save(org);
                log.info("B2B Organization {} status updated to active via lead conversion", org.getCompanyName());
            }
        });

        User existing = userRepository.findByEmailIgnoreCase(lead.getEmail()).orElse(null);
        if (existing != null) {
            lead.setConvertedClient(existing);
            leadRepository.save(lead);
            return new ConversionResult(false, null, existing);
        }

        String tempPassword = "FB" + UUID.randomUUID().toString().substring(0, 8);
        User client = new User();
        client.setName(lead.getName());
        client.setEmail(lead.getEmail());
        client.setPhone(lead.getPhone());
        client.setRole("client");
        client.setDepartment(lead.getDepartment());
        client.setPassword(passwordEncoder.encode(tempPassword));
        User savedClient = userRepository.save(client);

        lead.setConvertedClient(savedClient);
        leadRepository.save(lead);
        log.info("Lead {} converted to new client {}", lead.getLeadId(), savedClient.getEmail());
        return new ConversionResult(true, tempPassword, savedClient);
    }

    /**
     * Rich pipeline stats grouped by status, priority, department and source for
     * CRM dashboards.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        List<Lead> leads = leadRepository.findByActiveTrueOrderByCreatedAtDesc();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pipeline", countList(leads, Lead::getStatus, "status"));
        out.put("byPriority", countList(leads, Lead::getPriority, "_id"));
        out.put("byDepartment",
                countList(leads, l -> l.getDepartment() == null ? "unassigned" : l.getDepartment(), "_id"));
        out.put("bySource", countList(leads, Lead::getSource, "_id"));
        out.put("total", (long) leads.size());
        return out;
    }

    private List<Map<String, Object>> countList(List<Lead> leads,
            java.util.function.Function<Lead, String> key, String keyName) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Lead l : leads) {
            String k = key.apply(l);
            if (k == null)
                continue;
            counts.merge(k, 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put(keyName, e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .toList();
    }

    private int calculateScore(Lead lead) {
        int score = 0;
        if (lead.getIncome() != null) {
            double income = lead.getIncome().doubleValue();
            if (income >= 1500000)
                score += 35;
            else if (income >= 600000)
                score += 20;
        }
        if (lead.getBudget() != null) {
            double budget = lead.getBudget().doubleValue();
            if (budget >= 10000000)
                score += 35;
            else if (budget >= 3000000)
                score += 20;
        }
        if (lead.getRequirement() != null && !lead.getRequirement().isBlank())
            score += 15;
        if (lead.getPhone() != null && !lead.getPhone().isBlank())
            score += 10;
        if (lead.getEmail() != null && !lead.getEmail().isBlank())
            score += 5;
        return Math.min(score, 100);
    }

    private String scoreToPriority(int score) {
        if (score >= 65)
            return "hot";
        if (score >= 35)
            return "warm";
        return "cold";
    }

    @Transactional
    public Lead sendFeeProposal(UUID id, User deptAdmin) {
        Lead lead = getById(id);

        OrganizationUser orgUser = organizationUserRepository.findByEmailIgnoreCase(lead.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "B2B Organization user not found for lead email: " + lead.getEmail()));
        Organization org = orgUser.getOrganization();
        if (org == null) {
            throw new ResourceNotFoundException("Organization not found for user: " + orgUser.getEmail());
        }

        BigDecimal fee = getFeeForDepartment(lead.getDepartment());
        String deptName = getDepartmentName(lead.getDepartment());

        ServiceRequest matchedSr = null;
        List<ServiceRequest> requests = serviceRequestRepository.findByOrganizationIdAndActiveTrue(org.getId());
        for (ServiceRequest sr : requests) {
            if (sr.getDepartmentId() != null && sr.getDepartmentId().equalsIgnoreCase(lead.getDepartment())) {
                if (matchedSr == null || (sr.getCreatedAt() != null && matchedSr.getCreatedAt() != null
                        && sr.getCreatedAt().isAfter(matchedSr.getCreatedAt()))) {
                    matchedSr = sr;
                }
            }
        }

        OrganizationProposal proposal = new OrganizationProposal();
        proposal.setOrganization(org);
        proposal.setConsultant(deptAdmin);
        proposal.setDepartment(lead.getDepartment());
        proposal.setTitle(deptName + " Consultation Fee Request");
        proposal.setSummary("Pre-assignment consultation fee for " + deptName + " advisory services.");
        proposal.setFeeAmount(fee);
        proposal.setCurrency("INR");
        proposal.setStatus("sent");
        proposal.setValidUntil(LocalDate.now().plusDays(15));
        proposal.setActive(true);
        proposal.setServiceRequest(matchedSr);
        proposal.setDetails(java.util.Map.of("leadId", lead.getId().toString()));

        organizationProposalRepository.save(proposal);

        lead.setStatus("pending_fee");
        leadRepository.save(lead);

        if (matchedSr != null) {
            matchedSr.setStatus("pending_payment");
            serviceRequestRepository.save(matchedSr);
            log.info("ServiceRequest {} status updated to pending_payment", matchedSr.getRequestNumber());
        }

        // Send email notification to client about the fee proposal
        if (orgUser.getEmail() != null) {
            emailService.sendProposalCreated(
                    orgUser.getEmail(), orgUser.getName(),
                    proposal.getTitle(), proposal.getDepartment(), proposal.getId());
            log.info("Sent proposal email to {} for organization {}", orgUser.getEmail(), org.getCompanyName());
        }

        log.info("Sent fee proposal of {} for lead {}, organization {}", fee, lead.getLeadId(), org.getCompanyName());
        return lead;
    }

    private BigDecimal getFeeForDepartment(String dept) {
        if (dept == null)
            return new BigDecimal("2000");
        return switch (dept.toLowerCase()) {
            case "loans" -> new BigDecimal("5000");
            case "tax" -> new BigDecimal("2000");
            case "investment" -> new BigDecimal("3000");
            case "insurance" -> new BigDecimal("1500");
            case "wealth" -> new BigDecimal("10000");
            default -> new BigDecimal("2000");
        };
    }

    private String normalizeDepartment(String dept) {
        if (dept == null)
            return null;
        return switch (dept.toLowerCase()) {
            case "investment" -> "investments";
            default -> dept;
        };
    }

    private String getDepartmentName(String dept) {
        if (dept == null)
            return "Consultation";
        return switch (dept.toLowerCase()) {
            case "loans" -> "Loans";
            case "tax" -> "Tax Planning / Filing";
            case "investment" -> "Investments";
            case "insurance" -> "Insurance";
            case "wealth" -> "Wealth Management";
            default -> dept.substring(0, 1).toUpperCase() + dept.substring(1);
        };
    }
}
