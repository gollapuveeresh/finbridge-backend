package com.finbridge.service;

import com.finbridge.dto.*;
import com.finbridge.entity.*;
import com.finbridge.exception.BadRequestException;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.*;
import com.finbridge.security.B2BAccessGuard;
import com.finbridge.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class B2BService {

    private static final long VERIFY_TTL_MS = 24 * 60 * 60 * 1000L; // 24h

    private final OrganizationRepository orgRepo;
    private final OrganizationUserRepository orgUserRepo;
    private final ServiceRequestRepository serviceReqRepo;
    private final OrganizationDocumentRepository orgDocRepo;
    private final OrganizationProposalRepository orgProposalRepo;
    private final OrganizationMeetingRepository orgMeetingRepo;
    private final OrganizationPaymentRepository orgPaymentRepo;
    private final SupportTicketRepository supportTicketRepo;
    private final InvoiceRepository invoiceRepo;
    private final LeadService leadService;
    private final LeadRepository leadRepo;
    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SequenceGenerator sequenceGenerator;
    private final ConsultationRepository consultationRepo;
    private final UserRepository userRepo;
    private final ProposalRepository proposalRepository;
    private final DeptCaseRepository deptCaseRepository;
    private final LoanCaseRepository loanCaseRepository;
    private final ProposalService proposalService;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @org.springframework.beans.factory.annotation.Value("${app.payments.mock-enabled:true}")
    private boolean mockPaymentsEnabled;

    // ─── Registration ────────────────────────────────────────────────────────
    @Transactional
    public OrgLoginResponse register(OrgRegisterRequest req) {
        String cleanGstin = req.getGstin() != null && !req.getGstin().trim().isEmpty() ? req.getGstin().trim() : null;
        String cleanCin = req.getCin() != null && !req.getCin().trim().isEmpty() ? req.getCin().trim() : null;
        String cleanPan = req.getPan() != null && !req.getPan().trim().isEmpty() ? req.getPan().trim() : null;
        String cleanWebsite = req.getWebsite() != null && !req.getWebsite().trim().isEmpty() ? req.getWebsite().trim()
                : null;

        if (orgUserRepo.existsByEmailIgnoreCase(req.getAdminEmail()))
            throw new BadRequestException("Email already registered");
        if (cleanGstin != null && orgRepo.findByGstin(cleanGstin).isPresent())
            throw new BadRequestException("GSTIN already registered");

        Organization org = new Organization();
        org.setCompanyName(req.getCompanyName());
        org.setIndustry(req.getIndustry());
        org.setGstin(cleanGstin);
        org.setCin(cleanCin);
        org.setPan(cleanPan);
        org.setAnnualTurnover(req.getAnnualTurnover());
        org.setEmployeeCount(req.getEmployeeCount());
        org.setAddress(req.getAddress());
        org.setCity(req.getCity());
        org.setState(req.getState());
        org.setPincode(req.getPincode());
        org.setWebsite(cleanWebsite);
        if (req.getServices() != null)
            org.setServices(req.getServices().toArray(new String[0]));
        org.setStatus("pending");
        orgRepo.save(org);

        // Auto-generate service requests for selected services
        if (req.getServices() != null) {
            for (String serviceName : req.getServices()) {
                String deptId = mapServiceToDepartmentId(serviceName);
                if (deptId != null) {
                    ServiceRequest sr = new ServiceRequest();
                    sr.setRequestNumber(sequenceGenerator.next(SequenceGenerator.Seq.SERVICE_REQUEST));
                    sr.setOrganization(org);
                    sr.setDepartmentId(deptId);
                    sr.setTitle(serviceName + " Request");
                    sr.setDescription("Automatically requested service during registration.");
                    sr.setPriority("medium");
                    sr.setStatus("submitted");
                    serviceReqRepo.save(sr);
                }
            }
        }

        OrganizationUser admin = new OrganizationUser();
        admin.setOrganization(org);
        admin.setName(req.getAdminName());
        admin.setEmail(req.getAdminEmail());
        admin.setPasswordHash(passwordEncoder.encode(req.getAdminPassword()));
        admin.setRole("COMPANY_ADMIN");
        admin.setActive(false); // Require email verification before login
        admin.setEmailVerified(false);
        orgUserRepo.save(admin);

        // Auto-generate a CRM lead so the sales team can follow up on the new B2B
        // sign-up.
        createCrmLead(org, req);

        // Send verification email
        String verifyToken = jwtService.generatePurposeToken(admin.getId().toString(), "verify-email-b2b",
                VERIFY_TTL_MS);
        String verifyLink = frontendUrl + "/verify-email?token=" + verifyToken;
        emailService.sendVerificationEmail(admin.getEmail(), admin.getName(), verifyLink);
        log.info("B2B registration: verification email sent to {}", admin.getEmail());

        return buildLoginResponse(null, admin, org);
    }

    /** Creates a CRM lead from a freshly registered organization. */
    private void createCrmLead(Organization org, OrgRegisterRequest req) {
        Lead lead = new Lead();
        lead.setName(org.getCompanyName());
        lead.setEmail(req.getAdminEmail());
        lead.setPhone(req.getAdminPhone());
        lead.setSource("b2b_registration");
        lead.setIncome(org.getAnnualTurnover());
        lead.setServiceType(req.getServices() != null ? String.join(", ", req.getServices()) : null);
        lead.setDepartment(primaryDepartment(req.getServices()));
        lead.setRequirement(buildRequirement(org, req));
        leadService.create(lead);
    }

    private String buildRequirement(Organization org, OrgRegisterRequest req) {
        StringBuilder sb = new StringBuilder("New B2B organization registration");
        if (org.getIndustry() != null)
            sb.append(" • Industry: ").append(org.getIndustry());
        if (req.getServices() != null && !req.getServices().isEmpty())
            sb.append(" • Services: ").append(String.join(", ", req.getServices()));
        if (org.getEmployeeCount() != null)
            sb.append(" • Employees: ").append(org.getEmployeeCount());
        return sb.toString();
    }

    /** Maps a service name string to a department ID. */
    private String mapServiceToDepartmentId(String serviceName) {
        if (serviceName == null)
            return null;
        String s = serviceName.toLowerCase();
        if (s.contains("loan"))
            return "loans";
        if (s.contains("tax"))
            return "tax";
        if (s.contains("invest"))
            return "investments";
        if (s.contains("insurance"))
            return "insurance";
        if (s.contains("wealth"))
            return "wealth";
        return null;
    }

    /**
     * Maps the first selected service to a CRM department code (null if
     * none/unknown).
     */
    private String primaryDepartment(List<String> services) {
        if (services == null || services.isEmpty())
            return null;
        String s = services.get(0).toLowerCase();
        if (s.contains("loan"))
            return "loans";
        if (s.contains("tax"))
            return "tax";
        if (s.contains("invest"))
            return "investments";
        if (s.contains("insurance"))
            return "insurance";
        if (s.contains("wealth"))
            return "wealth";
        return null;
    }

    // ─── Login ───────────────────────────────────────────────────────────────
    @Transactional
    public OrgLoginResponse login(OrgLoginRequest req) {
        OrganizationUser user = orgUserRepo.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
            throw new BadRequestException("Invalid credentials");
        if (!user.isEmailVerified())
            throw new BadRequestException("Please verify your email before logging in. Check your inbox for the verification link.");
        if (!user.isActive())
            throw new BadRequestException("Account is deactivated");

        user.setLastLogin(Instant.now());
        orgUserRepo.save(user);

        String token = jwtService.generateB2BToken(user.getId().toString(), user.getOrganization().getId().toString());
        return buildLoginResponse(token, user, user.getOrganization());
    }

    // ─── Service Requests ────────────────────────────────────────────────────
    @Transactional
    public ServiceRequestResponse createServiceRequest(UUID orgId, ServiceRequestRequest req) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        ServiceRequest sr = new ServiceRequest();
        sr.setRequestNumber(sequenceGenerator.next(SequenceGenerator.Seq.SERVICE_REQUEST));
        sr.setOrganization(org);
        sr.setDepartmentId(req.getDepartmentId());
        sr.setTitle(req.getTitle());
        sr.setDescription(req.getDescription());
        sr.setPriority(req.getPriority() != null ? req.getPriority() : "medium");
        sr.setAmountInvolved(req.getAmountInvolved());
        sr.setCurrency(req.getCurrency() != null ? req.getCurrency() : "INR");
        sr.setNotes(req.getNotes());
        serviceReqRepo.save(sr);

        // Also create a Lead for the department admin to assign a consultant
        Lead lead = new Lead();
        lead.setName(org.getCompanyName());

        String adminEmail = "";
        List<OrganizationUser> members = orgUserRepo.findByOrganizationId(org.getId());
        for (OrganizationUser u : members) {
            if ("COMPANY_ADMIN".equals(u.getRole())) {
                adminEmail = u.getEmail();
                break;
            }
        }
        if (adminEmail.isEmpty() && !members.isEmpty()) {
            adminEmail = members.get(0).getEmail();
        }
        String adminPhone = leadService.getPhoneByEmail(adminEmail);

        lead.setEmail(adminEmail);
        lead.setPhone(adminPhone);
        lead.setSource("b2b_request");
        lead.setIncome(org.getAnnualTurnover());
        lead.setServiceType(req.getTitle());
        lead.setDepartment(req.getDepartmentId());
        lead.setRequirement(req.getDescription());
        if (lead.getDepartment() != null) {
            lead.setDepartment(
                    lead.getDepartment().toLowerCase().equals("investment") ? "investments" : lead.getDepartment());
        }
        lead.setBudget(req.getAmountInvolved());
        lead.setStatus("qualified"); // direct routing to department
        leadService.create(lead);

        return toResponse(sr);
    }

    public List<ServiceRequestResponse> getOrgServiceRequests(UUID orgId) {
        return serviceReqRepo.findByOrganizationIdAndActiveTrue(orgId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public ServiceRequestResponse updateStatus(UUID srId, String status, Object principal) {
        ServiceRequest sr = serviceReqRepo.findById(srId)
                .orElseThrow(() -> new ResourceNotFoundException("Service request not found"));
        B2BAccessGuard.assertOrgAccess(principal, sr.getOrganization().getId());
        sr.setStatus(status);
        if ("completed".equals(status) || "rejected".equals(status))
            sr.setClosedAt(Instant.now());
        serviceReqRepo.save(sr);
        return toResponse(sr);
    }

    // ─── Department Admin: list by dept ──────────────────────────────────────
    public List<ServiceRequestResponse> getDeptServiceRequests(String deptId) {
        return serviceReqRepo.findByDepartmentIdAndActiveTrue(deptId)
                .stream().map(this::toResponse).toList();
    }

    // ─── Organization Dashboard Stats ────────────────────────────────────────
    public java.util.Map<String, Object> getOrgStats(UUID orgId) {
        var all = serviceReqRepo.findByOrganizationIdAndActiveTrue(orgId);
        long pending = all.stream().filter(s -> !List.of("completed", "rejected").contains(s.getStatus())).count();
        long completed = all.stream().filter(s -> "completed".equals(s.getStatus())).count();
        var docs = orgDocRepo.findByOrganizationId(orgId);
        long pendingDocs = docs.stream().filter(d -> "pending".equals(d.getStatus())).count();
        var payments = orgPaymentRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        var totalPaid = payments.stream().filter(p -> "paid".equals(p.getStatus()))
                .mapToDouble(p -> p.getAmount().doubleValue()).sum();

        java.util.Set<String> requiredTypes = java.util.Set.of("GST_CERTIFICATE", "PAN", "CIN", "INCORPORATION",
                "BANK_STATEMENT");
        boolean allRequiredUploaded = requiredTypes.stream()
                .allMatch(type -> docs.stream().anyMatch(d -> type.equals(d.getDocumentType())));

        return java.util.Map.of(
                "totalRequests", all.size(),
                "pendingRequests", pending,
                "completedRequests", completed,
                "totalDocuments", docs.size(),
                "pendingDocuments", pendingDocs,
                "totalPaid", totalPaid,
                "activeProposals", orgProposalRepo.findByOrganizationIdAndActiveTrue(orgId).size(),
                "allRequiredUploaded", allRequiredUploaded);
    }

    // ─── Support Tickets ─────────────────────────────────────────────────────
    @Transactional
    public SupportTicket createTicket(UUID orgId, String subject, String description, String category,
            String priority) {
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketNumber(sequenceGenerator.next(SequenceGenerator.Seq.SUPPORT_TICKET));
        ticket.setOrganization(org);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setCategory(category != null ? category : "general");
        ticket.setPriority(priority != null ? priority : "medium");
        return supportTicketRepo.save(ticket);
    }

    public List<SupportTicket> getOrgTickets(UUID orgId) {
        return supportTicketRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    // ─── Team Members ────────────────────────────────────────────────────────
    public List<OrganizationUser> getTeamMembers(UUID orgId) {
        return orgUserRepo.findByOrganizationId(orgId);
    }

    @Transactional
    public OrganizationUser addTeamMember(UUID orgId, String name, String email, String role, String password) {
        if (orgUserRepo.existsByEmailIgnoreCase(email))
            throw new BadRequestException("Email already registered");
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        OrganizationUser u = new OrganizationUser();
        u.setOrganization(org);
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(role);
        return orgUserRepo.save(u);
    }

    // ─── Proposals (org-facing) ───────────────────────────────────────────────
    public List<OrganizationProposal> getOrgProposals(UUID orgId) {
        return orgProposalRepo.findByOrganizationIdAndActiveTrue(orgId);
    }

    @Transactional
    public OrganizationProposal decideProposal(UUID proposalId, String decision, String feedback, Object principal) {
        OrganizationProposal p = orgProposalRepo.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        B2BAccessGuard.assertOrgAccess(principal, p.getOrganization().getId());
        p.setStatus(decision); // approved | changes_requested | rejected
        p.setOrgFeedback(feedback);

        // Propagate decision back to CRM Proposal (which will auto-propagate to case)
        UUID crmProposalId = null;
        if (p.getDetails() != null && p.getDetails().containsKey("proposalId")) {
            try {
                crmProposalId = UUID.fromString(p.getDetails().get("proposalId").toString());
            } catch (Exception ex) {
                // ignore
            }
        }
        if (crmProposalId != null) {
            try {
                proposalService.updateStatus(crmProposalId, decision, feedback);
            } catch (Exception ex) {
                // ignore if already updated or fails
            }
        }

        if ("approved".equals(decision) && p.getTitle() != null && p.getTitle().contains("Consultation Fee Request")) {
            UUID orgId = p.getOrganization().getId();
            List<OrganizationUser> members = orgUserRepo.findByOrganizationId(orgId);
            Lead matchedLead = null;
            if (p.getDetails() != null && p.getDetails().containsKey("leadId")) {
                try {
                    UUID leadId = UUID.fromString(p.getDetails().get("leadId").toString());
                    matchedLead = leadRepo.findById(leadId).orElse(null);
                } catch (Exception ex) {
                    // ignore
                }
            }

            if (matchedLead == null) {
                for (OrganizationUser member : members) {
                    List<Lead> leads = leadRepo.findByEmailIgnoreCase(member.getEmail());
                    for (Lead l : leads) {
                        if (l.isActive() && l.getDepartment() != null
                                && l.getDepartment().equalsIgnoreCase(p.getDepartment())) {
                            if ("pending_fee".equals(l.getStatus())) {
                                matchedLead = l;
                                break;
                            }
                            if (matchedLead == null || (l.getCreatedAt() != null && matchedLead.getCreatedAt() != null
                                    && l.getCreatedAt().isAfter(matchedLead.getCreatedAt()))) {
                                matchedLead = l;
                            }
                        }
                    }
                    if (matchedLead != null && "pending_fee".equals(matchedLead.getStatus())) {
                        break;
                    }
                }
            }

            if (matchedLead != null) {
                matchedLead.setStatus("fee_paid");
                leadRepo.save(matchedLead);
            }

            ServiceRequest matchedSr = p.getServiceRequest();
            if (matchedSr == null) {
                List<ServiceRequest> serviceRequests = serviceReqRepo.findByOrganizationIdAndActiveTrue(orgId);
                for (ServiceRequest sr : serviceRequests) {
                    if (sr.getDepartmentId() != null && sr.getDepartmentId().equalsIgnoreCase(p.getDepartment())) {
                        if (matchedSr == null || (sr.getCreatedAt() != null && matchedSr.getCreatedAt() != null
                                && sr.getCreatedAt().isAfter(matchedSr.getCreatedAt()))) {
                            matchedSr = sr;
                        }
                    }
                }
            }

            if (matchedSr != null) {
                matchedSr.setStatus("fee_paid");
                serviceReqRepo.save(matchedSr);
            }

            OrganizationPayment payment = new OrganizationPayment();
            payment.setPaymentNumber(sequenceGenerator.next(SequenceGenerator.Seq.ORG_PAYMENT));
            payment.setOrganization(p.getOrganization());
            payment.setServiceRequest(matchedSr);
            payment.setProposal(p);
            payment.setAmount(p.getFeeAmount() != null ? p.getFeeAmount() : java.math.BigDecimal.ZERO);
            payment.setCurrency(p.getCurrency() != null ? p.getCurrency() : "INR");
            payment.setGateway("razorpay");
            payment.setGatewayPaymentId("pay_mock_" + UUID.randomUUID().toString().substring(0, 12));
            payment.setStatus("paid");
            payment.setPaidAt(Instant.now());
            orgPaymentRepo.save(payment);
        }

        return orgProposalRepo.save(p);
    }

    public List<OrganizationMeeting> getOrgMeetings(UUID orgId) {
        List<OrganizationMeeting> dbMeetings = orgMeetingRepo.findByOrganizationIdOrderByScheduledAtDesc(orgId);
        List<OrganizationUser> orgUsers = orgUserRepo.findByOrganizationId(orgId);
        if (orgUsers.isEmpty()) {
            return dbMeetings;
        }
        List<String> emails = orgUsers.stream().map(OrganizationUser::getEmail).toList();
        List<Consultation> consultations = consultationRepo.findByClientEmailInOrderByCreatedAtDesc(emails);

        List<OrganizationMeeting> combined = new ArrayList<>(dbMeetings);
        for (Consultation c : consultations) {
            if (c.getConfirmedDate() == null || c.getConfirmedDate().isBlank()) {
                continue;
            }
            OrganizationMeeting om = new OrganizationMeeting();
            om.setId(c.getId());
            Organization org = new Organization();
            org.setId(orgId);
            om.setOrganization(org);
            om.setConsultant(c.getConsultant());
            String category = c.getCategory();
            if (category == null)
                category = "Consultation";
            String title = category;
            if (!category.toLowerCase().contains("consultation")) {
                title += " Consultation";
            }
            title += " (" + c.getDepartment() + ")";
            om.setTitle(title);
            om.setMeetingType("video");

            Instant scheduled = null;
            try {
                String date = c.getConfirmedDate().trim();
                String time = c.getConfirmedTime().trim();
                if (time.length() == 5) {
                    // HH:mm
                } else if (time.length() == 4) {
                    time = "0" + time;
                } else {
                    time = "00:00";
                }
                java.time.ZoneOffset offset = java.time.OffsetDateTime.now(java.time.ZoneId.systemDefault())
                        .getOffset();
                scheduled = java.time.OffsetDateTime.parse(date + "T" + time + ":00" + offset.getId()).toInstant();
            } catch (Exception e) {
                scheduled = c.getCreatedAt();
            }
            om.setScheduledAt(scheduled);
            om.setDurationMinutes(60);
            om.setMeetingLink(c.getMeetingLink());
            om.setAgenda(c.getClientNotes());

            String status = "scheduled";
            if ("completed".equalsIgnoreCase(c.getStatus())
                    || "completed_by_consultant".equalsIgnoreCase(c.getStatus())) {
                status = "completed";
            }
            om.setStatus(status);
            combined.add(om);
        }

        // Sort combined meetings by scheduledAt descending
        combined.sort((a, b) -> {
            Instant t1 = a.getScheduledAt() != null ? a.getScheduledAt() : Instant.MIN;
            Instant t2 = b.getScheduledAt() != null ? b.getScheduledAt() : Instant.MIN;
            return t2.compareTo(t1);
        });

        return combined;
    }

    // ─── Payments ────────────────────────────────────────────────────────────
    public List<OrganizationPayment> getOrgPayments(UUID orgId) {
        return orgPaymentRepo.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    /**
     * Settle a pending payment. For now this is a MOCK gateway (no real charge) so
     * the flow can be
     * demoed end-to-end; once Razorpay keys are provided, replace the body with:
     * create order →
     * client opens Razorpay Checkout → verify the returned signature here → then
     * mark paid.
     *
     * @param gatewayRef the payment id returned by the (mock) gateway
     */
    @Transactional
    public OrganizationPayment payPayment(UUID paymentId, Object principal, String gatewayRef) {
        if (!mockPaymentsEnabled)
            throw new BadRequestException(
                    "Online payment is not available yet. Please contact FinBridge to settle this invoice.");
        OrganizationPayment op = orgPaymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        B2BAccessGuard.assertOrgAccess(principal, op.getOrganization().getId());
        if ("paid".equals(op.getStatus()))
            return op; // idempotent

        op.setStatus("paid");
        op.setPaidAt(Instant.now());
        op.setGateway("razorpay");
        op.setGatewayPaymentId(
                gatewayRef != null ? gatewayRef : "pay_mock_" + UUID.randomUUID().toString().substring(0, 12));
        orgPaymentRepo.save(op);

        // Reflect the settlement on the originating CRM invoice (gateway_order_id holds
        // the invoice number),
        // so the consultant/department side and revenue analytics show it as paid too.
        if (op.getGatewayOrderId() != null) {
            invoiceRepo.findByInvoiceNumber(op.getGatewayOrderId()).ifPresent(inv -> {
                if (!"paid".equals(inv.getStatus())) {
                    inv.setStatus("paid");
                    inv.setPaidAt(Instant.now());
                    invoiceRepo.save(inv);
                    paymentService.recordForInvoice(inv, "razorpay");
                }
            });
        }
        return op;
    }

    /**
     * Build a printable invoice payload for an org payment (real CRM invoice if
     * linked, else a basic one).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentInvoice(UUID paymentId, Object principal) {
        OrganizationPayment op = orgPaymentRepo.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        B2BAccessGuard.assertOrgAccess(principal, op.getOrganization().getId());
        Organization org = op.getOrganization();

        Invoice inv = op.getGatewayOrderId() == null ? null
                : invoiceRepo.findByInvoiceNumber(op.getGatewayOrderId()).orElse(null);

        Map<String, Object> out = new LinkedHashMap<>();
        if (inv != null) {
            out.put("invoiceNumber", inv.getInvoiceNumber());
            out.put("clientId", ref(org.getCompanyName(), inv.getClient() != null ? inv.getClient().getEmail() : null));
            out.put("consultantId",
                    ref(inv.getConsultant() != null ? inv.getConsultant().getName() : "FinBridge Advisory", null));
            out.put("serviceTitle", inv.getServiceTitle());
            out.put("department", inv.getDepartment());
            List<Map<String, Object>> items = new ArrayList<>();
            for (InvoiceLineItem li : inv.getLineItems())
                items.add(lineItem(li.getDescription(), li.getAmount()));
            out.put("lineItems", items);
            out.put("subtotal", inv.getSubtotal());
            out.put("tax", inv.getTax());
            out.put("taxPercent", inv.getTaxPercent());
            out.put("totalAmount", inv.getTotalAmount());
            out.put("status", inv.getStatus());
            out.put("dueDate", inv.getDueDate());
            out.put("paidAt", inv.getPaidAt());
            out.put("notes", inv.getNotes());
            out.put("currency", inv.getCurrency());
            out.put("createdAt", inv.getCreatedAt());
        } else {
            // No linked CRM invoice — synthesize a minimal invoice from the payment row.
            out.put("invoiceNumber", op.getPaymentNumber());
            out.put("clientId", ref(org.getCompanyName(), null));
            out.put("consultantId", ref("FinBridge Advisory", null));
            out.put("serviceTitle", "Professional services");
            out.put("lineItems", List.of(lineItem("Professional services", op.getAmount())));
            out.put("subtotal", op.getAmount());
            out.put("tax", java.math.BigDecimal.ZERO);
            out.put("taxPercent", java.math.BigDecimal.ZERO);
            out.put("totalAmount", op.getAmount());
            out.put("status", op.getStatus());
            out.put("paidAt", op.getPaidAt());
            out.put("currency", op.getCurrency());
            out.put("createdAt", op.getCreatedAt());
        }
        return out;
    }

    private Map<String, Object> ref(String name, String email) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("email", email);
        return m;
    }

    private Map<String, Object> lineItem(String description, java.math.BigDecimal amount) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("description", description);
        m.put("amount", amount);
        return m;
    }

    // ─── Documents ───────────────────────────────────────────────────────────
    public List<OrganizationDocument> getOrgDocuments(UUID orgId) {
        return orgDocRepo.findByOrganizationId(orgId);
    }

    private static final int MAX_DOC_CHARS = 8_000_000; // ~6 MB file once base64-encoded
    private static final Set<String> ALLOWED_DOC_TYPES = Set.of(
            "GST_CERTIFICATE", "PAN", "CIN", "INCORPORATION", "BANK_STATEMENT", "FINANCIAL_STATEMENT", "OTHER");
    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf", "image/jpeg", "image/jpg", "image/png");

    /**
     * Upload (or replace) a KYC document for an organization. Content is a base64
     * data URI.
     */
    @Transactional
    public OrganizationDocument uploadDocument(UUID orgId, String documentType, String fileName,
            String content, Object principal) {
        if (documentType == null || content == null || content.isBlank())
            throw new BadRequestException("documentType and file content are required");
        if (!ALLOWED_DOC_TYPES.contains(documentType))
            throw new BadRequestException("Unsupported document type: " + documentType);
        if (content.length() > MAX_DOC_CHARS)
            throw new BadRequestException("File too large. Maximum size is ~6 MB.");
        // Must be a base64 data URI of an allowed file type, e.g.
        // data:application/pdf;base64,XXXX
        if (!content.startsWith("data:") || !content.contains(";base64,"))
            throw new BadRequestException("Invalid file content; expected a base64 data URI.");
        String mime = content.substring(5, content.indexOf(';')).toLowerCase();
        if (!ALLOWED_MIME.contains(mime))
            throw new BadRequestException("Unsupported file type. Allowed: PDF, JPG, PNG.");
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        // Re-upload replaces the existing document of the same type; otherwise create a
        // new one.
        OrganizationDocument doc = orgDocRepo.findByOrganizationId(orgId).stream()
                .filter(d -> documentType.equals(d.getDocumentType()))
                .findFirst().orElseGet(OrganizationDocument::new);
        doc.setOrganization(org);
        doc.setDocumentType(documentType);
        doc.setFileName(fileName);
        doc.setFileUrl(content);
        doc.setStatus("pending");
        doc.setReviewerNote(null);
        if (principal instanceof OrganizationUser ou)
            doc.setUploadedBy(ou);
        return orgDocRepo.save(doc);
    }

    /**
     * Fetch a single document (including its base64 content) for download, scoped
     * to the
     * organization the caller was already authorized for. Verifying the document
     * belongs to
     * {@code orgId} closes the cross-org IDOR where a valid docId from another org
     * could be read.
     */
    @Transactional(readOnly = true)
    public OrganizationDocument getDocument(UUID docId, UUID orgId) {
        OrganizationDocument doc = orgDocRepo.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (doc.getOrganization() == null || !orgId.equals(doc.getOrganization().getId()))
            throw new ResourceNotFoundException("Document not found");
        return doc;
    }

    @Transactional(readOnly = true)
    public Organization getOrg(UUID orgId) {
        return orgRepo.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private OrgLoginResponse buildLoginResponse(String token, OrganizationUser user, Organization org) {
        return OrgLoginResponse.builder()
                .token(token)
                .orgUserId(user.getId())
                .organizationId(org.getId())
                .companyName(org.getCompanyName())
                .industry(org.getIndustry())
                .gstin(org.getGstin())
                .status(org.getStatus())
                .kycVerified(org.isKycVerified())
                .userName(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private ServiceRequestResponse toResponse(ServiceRequest sr) {
        String email = null;
        List<OrganizationUser> members = orgUserRepo.findByOrganizationId(sr.getOrganization().getId());
        for (OrganizationUser u : members) {
            if ("COMPANY_ADMIN".equals(u.getRole())) {
                email = u.getEmail();
                break;
            }
        }
        if (email == null && !members.isEmpty()) {
            email = members.get(0).getEmail();
        }

        String meetingStatus = null;
        String meetingDate = null;
        String meetingTime = null;
        String meetingLink = null;

        if (email != null) {
            java.util.Optional<User> clientUserOpt = userRepo.findByEmailIgnoreCase(email);
            if (clientUserOpt.isPresent()) {
                UUID clientUserId = clientUserOpt.get().getId();
                List<Consultation> consultations = consultationRepo.findByClientIdOrderByCreatedAtDesc(clientUserId);
                for (Consultation c : consultations) {
                    if (c.getDepartment() != null && c.getDepartment().equalsIgnoreCase(sr.getDepartmentId())) {
                        meetingStatus = c.getStatus();
                        meetingDate = c.getConfirmedDate();
                        meetingTime = c.getConfirmedTime();
                        meetingLink = c.getMeetingLink();
                        break;
                    }
                }
            }
        }

        return ServiceRequestResponse.builder()
                .id(sr.getId())
                .requestNumber(sr.getRequestNumber())
                .organizationId(sr.getOrganization().getId())
                .companyName(sr.getOrganization().getCompanyName())
                .departmentId(sr.getDepartmentId())
                .consultantName(sr.getConsultant() != null ? sr.getConsultant().getName() : null)
                .title(sr.getTitle())
                .description(sr.getDescription())
                .priority(sr.getPriority())
                .status(sr.getStatus())
                .amountInvolved(sr.getAmountInvolved())
                .currency(sr.getCurrency())
                .notes(sr.getNotes())
                .createdAt(sr.getCreatedAt())
                .updatedAt(sr.getUpdatedAt())
                .meetingStatus(meetingStatus)
                .meetingDate(meetingDate)
                .meetingTime(meetingTime)
                .meetingLink(meetingLink)
                .build();
    }
}
