package com.finbridge.service;

import com.finbridge.dto.DtoMapper;
import com.finbridge.dto.ProposalRequest;
import com.finbridge.dto.ProposalResponse;
import com.finbridge.entity.OrganizationProposal;
import com.finbridge.entity.OrganizationUser;
import com.finbridge.entity.Proposal;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.OrganizationProposalRepository;
import com.finbridge.repository.OrganizationUserRepository;
import com.finbridge.repository.ProposalRepository;
import com.finbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalService {
    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final com.finbridge.repository.LeadRepository leadRepository;
    private final OrganizationUserRepository orgUserRepository;
    private final OrganizationProposalRepository orgProposalRepository;
    private final DtoMapper mapper;
    private final com.finbridge.repository.DeptCaseRepository deptCaseRepository;
    private final com.finbridge.repository.LoanCaseRepository loanCaseRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    /** Proposals relevant to the user (consultant/client/department-admin), optional department override. */
    @Transactional(readOnly = true)
    public List<ProposalResponse> getForUser(User user, String department) {
        List<Proposal> list;
        if (department != null) {
            list = proposalRepository.findByDepartmentAndActiveTrueOrderByCreatedAtDesc(department);
        } else {
            list = switch (user.getRole()) {
                case "consultant" -> proposalRepository.findByConsultantIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
                case "department-admin" -> user.getDepartment() != null
                        ? proposalRepository.findByDepartmentAndActiveTrueOrderByCreatedAtDesc(user.getDepartment())
                        : proposalRepository.findByActiveTrueOrderByCreatedAtDesc();
                case "admin", "super-admin", "crm-admin" -> proposalRepository.findByActiveTrueOrderByCreatedAtDesc();
                default -> proposalRepository.findByClientIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
            };
        }
        return list.stream().map(mapper::toProposalResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProposalResponse getResponse(UUID id) { return mapper.toProposalResponse(getById(id)); }

    public Proposal getById(UUID id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found: " + id));
    }

    /** The proposal form sends a calendar date; the entity stores an Instant. Convert at the boundary. */
    private static java.time.Instant toInstant(java.time.LocalDate d) {
        return d == null ? null : d.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    @Transactional
    public void delete(UUID id) {
        Proposal p = getById(id);
        p.setActive(false);
        proposalRepository.save(p);
    }

    @Transactional
    public ProposalResponse create(ProposalRequest r, User currentUser) {
        Proposal p = new Proposal();
        p.setConsultant(currentUser);
        User client = null;
        if (r.clientId() != null) {
            client = userRepository.findById(r.clientId()).orElse(null);
            p.setClient(client);
        }
        if (r.leadId() != null) leadRepository.findById(r.leadId()).ifPresent(p::setLead);
        p.setDepartment(r.department());
        p.setTitle(r.title());
        p.setSummary(r.summary());
        p.setDetails(r.details());
        p.setValidUntil(toInstant(r.validUntil()));
        p.setStatus("draft");
        ProposalResponse response = mapper.toProposalResponse(proposalRepository.save(p));
        // NOTE: a draft is NOT pushed to the B2B portal — the org should only see a proposal
        // once it is actually sent. The sync happens in updateStatus() on status="sent".

        // Email the client about the new proposal
        if (client != null && client.getEmail() != null) {
            emailService.sendProposalCreated(client.getEmail(), client.getName(),
                    r.title(), r.department(), p.getId());
        }

        return response;
    }

    @Transactional
    public ProposalResponse update(UUID id, ProposalRequest r) {
        Proposal p = getById(id);
        if (r.title() != null) p.setTitle(r.title());
        if (r.summary() != null) p.setSummary(r.summary());
        if (r.details() != null) p.setDetails(r.details());
        if (r.validUntil() != null) p.setValidUntil(toInstant(r.validUntil()));
        return mapper.toProposalResponse(proposalRepository.save(p));
    }

    /** Status update (send / approve / reject / request-changes), with optional client feedback. */
    @Transactional
    public ProposalResponse updateStatus(UUID id, String status, String feedback) {
        Proposal p = getById(id);
        if (status != null) p.setStatus(status);
        if (feedback != null) p.setClientFeedback(feedback);
        ProposalResponse response = mapper.toProposalResponse(proposalRepository.save(p));

        // Sync proposal changes to the client's B2B portal
        syncToOrgProposal(p);

        // Update the linked case decision status
        propagateDecisionToCase(p, status, feedback);

        // Email the client about approval/rejection
        if (("approved".equals(status) || "rejected".equals(status)) && p.getClient() != null && p.getClient().getEmail() != null) {
            emailService.sendProposalDecision(p.getClient().getEmail(), p.getClient().getName(),
                    p.getTitle(), status, p.getId());
        }

        // Create dashboard & email notifications for staff when client acts on proposal
        try {
            if (("approved".equals(status) || "rejected".equals(status) || "changes_requested".equals(status)) 
                    && p.getClient() != null) {
                String action = "approved".equals(status) ? "approved" : 
                                "rejected".equals(status) ? "rejected" : "requested changes on";
                
                String capitalizedStatus = status.substring(0, 1).toUpperCase() + status.substring(1);
                String title = "Proposal " + capitalizedStatus;
                String message = "Client " + p.getClient().getName() + " has " + action + " the proposal \"" + p.getTitle() + "\".";
                
                // 1. Notify the assigned consultant
                if (p.getConsultant() != null) {
                    notificationService.create(p.getConsultant(), "proposal", title, message);
                }
                
                // 2. Notify department admins
                for (User admin : userRepository.findByRoleAndDepartmentAndActiveTrue("department-admin", p.getDepartment())) {
                    notificationService.create(admin, "proposal", title, message + " (Dept: " + p.getDepartment() + ")");
                }
                
                // 3. Notify CRM admins
                for (User crmAdmin : userRepository.findByRoleAndActiveTrue("crm-admin")) {
                    notificationService.create(crmAdmin, "proposal", title, message);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send staff notifications for proposal decision: {}", e.getMessage());
        }

        return response;
    }

    private void propagateDecisionToCase(Proposal p, String decision, String feedback) {
        if (p.getCaseId() == null || p.getCaseModel() == null) {
            return;
        }
        UUID caseId = p.getCaseId();
        String caseModel = p.getCaseModel();
        String mappedStatus = switch (decision) {
            case "approved" -> "Approved";
            case "changes_requested" -> "Changes Requested";
            case "rejected" -> "Rejected";
            default -> decision;
        };

        if ("DeptCase".equalsIgnoreCase(caseModel)) {
            deptCaseRepository.findById(caseId).ifPresent(dc -> {
                Map<String, Object> data = new HashMap<>(dc.getData() != null ? dc.getData() : new HashMap<>());
                Map<String, Object> clientDecision = new HashMap<>();
                clientDecision.put("status", mappedStatus);
                clientDecision.put("feedback", feedback);
                clientDecision.put("decidedAt", Instant.now().toString());
                data.put("clientDecision", clientDecision);
                dc.setData(data);
                deptCaseRepository.save(dc);
                log.info("Updated DeptCase {} client decision to {} based on CRM proposal decision", caseId, mappedStatus);
            });
        } else if ("LoanCase".equalsIgnoreCase(caseModel)) {
            loanCaseRepository.findById(caseId).ifPresent(lc -> {
                lc.setClientDecision(mappedStatus);
                lc.setDecidedAt(Instant.now());
                lc.setClientFeedback(feedback);
                loanCaseRepository.save(lc);
                log.info("Updated LoanCase {} client decision to {} based on CRM proposal decision", caseId, mappedStatus);
            });
        }
    }

    /**
     * If the proposal's client is also a B2B organization user, mirror it into the org's portal as an
     * OrganizationProposal. Idempotent: re-sending updates the existing mirror instead of duplicating it.
     */
    public void syncToOrgProposal(Proposal p) {
        User client = p.getClient();
        if (client == null || client.getEmail() == null) return;
        orgUserRepository.findByEmailIgnoreCase(client.getEmail()).ifPresent(orgUser -> {
            UUID orgId = orgUser.getOrganization().getId();
            // Reuse an existing mirror for the same org + title + department instead of creating a duplicate.
            OrganizationProposal op = orgProposalRepository.findByOrganizationIdAndActiveTrue(orgId).stream()
                    .filter(e -> p.getTitle() != null && p.getTitle().equals(e.getTitle())
                            && java.util.Objects.equals(p.getDepartment(), e.getDepartment()))
                    .findFirst()
                    .orElseGet(OrganizationProposal::new);
            op.setOrganization(orgUser.getOrganization());
            op.setConsultant(p.getConsultant());   // required (NOT NULL) — was missing, broke the sync
            op.setDepartment(p.getDepartment());
            op.setTitle(p.getTitle());
            op.setSummary(p.getSummary());
            
            Map<String, Object> details = p.getDetails() != null ? new HashMap<>(p.getDetails()) : new HashMap<>();
            details.put("proposalId", p.getId().toString());
            if (p.getCaseId() != null) {
                details.put("caseId", p.getCaseId().toString());
            }
            if (p.getCaseModel() != null) {
                details.put("caseModel", p.getCaseModel());
            }
            op.setDetails(details);
            
            op.setStatus(p.getStatus());
            if (p.getValidUntil() != null) {
                op.setValidUntil(p.getValidUntil().atZone(ZoneId.systemDefault()).toLocalDate());
            }
            orgProposalRepository.save(op);
            log.info("Synced proposal '{}' to organization {} as B2B proposal", p.getTitle(), orgId);
        });
    }
}
