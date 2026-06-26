package com.finbridge.service;

import com.finbridge.dto.LoanCaseResponse;
import com.finbridge.entity.*;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.LeadRepository;
import com.finbridge.repository.LoanCaseRepository;
import com.finbridge.repository.UserRepository;
import com.finbridge.repository.ProposalRepository;
import com.finbridge.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanCaseService {

    private static final List<String> DEFAULT_DOCS = List.of("PAN Card", "Aadhaar Card", "Income Proof",
            "Bank Statements (6 months)", "Address Proof");

    private final LoanCaseRepository loanCaseRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final SequenceGenerator sequenceGenerator;
    private final ProposalRepository proposalRepository;
    private final ProposalService proposalService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<LoanCaseResponse> getForUser(User user) {
        List<LoanCase> list = switch (user.getRole()) {
            case "consultant" -> loanCaseRepository.findByConsultantIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
            case "client" -> loanCaseRepository.findByClientIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
            default -> loanCaseRepository.findByActiveTrueOrderByCreatedAtDesc();
        };
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public LoanCaseResponse create(Map<String, Object> body, User consultant) {
        LoanCase lc = new LoanCase();
        lc.setCaseId(sequenceGenerator.next(SequenceGenerator.Seq.LOAN_CASE));
        lc.setConsultant(consultant);

        User client = null;
        if (body.get("clientId") != null)
            client = userRepository.findById(UUID.fromString(body.get("clientId").toString())).orElse(null);
        if (body.get("leadId") != null) {
            Lead lead = leadRepository.findById(UUID.fromString(body.get("leadId").toString())).orElse(null);
            lc.setLead(lead);
            if (client == null && lead != null && lead.getConvertedClient() != null)
                client = lead.getConvertedClient();
        }
        // Loan cases require a client; fall back to the consultant only to satisfy the
        // FK in demo data.
        lc.setClient(client != null ? client : consultant);
        lc.setLoanType(str(body.get("loanType")));
        lc.setRequestedAmount(bd(body.get("requestedAmount")));

        for (String name : DEFAULT_DOCS) {
            LoanCaseDocument d = new LoanCaseDocument();
            d.setLoanCase(lc);
            d.setName(name);
            d.setCategory("KYC");
            d.setStatus("Pending");
            lc.getDocuments().add(d);
        }
        log.info("Loan case created: {}", lc.getCaseId());
        return toResponse(loanCaseRepository.save(lc));
    }

    @Transactional
    public LoanCaseResponse patch(UUID id, Map<String, Object> body, User actor) {
        LoanCase lc = loadOwned(id, actor);
        boolean transitioningToApproval = false;
        if (body.get("stage") != null) {
            String newStage = str(body.get("stage"));
            if ("client_approval".equals(newStage) && !"client_approval".equals(lc.getStage())) {
                transitioningToApproval = true;
            }
            lc.setStage(newStage);
        }

        Map<String, Object> elig = asMap(body.get("eligibility"));
        if (elig != null) {
            lc.setCreditScore(in(elig.get("creditScore")));
            lc.setDti(bd(elig.get("dti")));
            lc.setLtv(bd(elig.get("ltv")));
            lc.setEligible(bool(elig.get("eligible")));
            if (elig.get("analystNote") != null)
                lc.setAnalystNote(str(elig.get("analystNote")));
        }

        Map<String, Object> rec = asMap(body.get("recommendation"));
        if (rec != null) {
            if (rec.get("recommendedBank") != null)
                lc.setRecommendedBank(str(rec.get("recommendedBank")));
            lc.setRecommendedRate(bd(rec.get("recommendedRate")));
            lc.setRecommendedTenure(in(rec.get("recommendedTenure")));
            lc.setRecommendedEmi(bd(rec.get("recommendedEMI")));
            if (rec.get("note") != null)
                lc.setRecommendationNote(str(rec.get("note")));
            if (rec.get("sentToClient") != null)
                lc.setSentToClient(bool(rec.get("sentToClient")));
        }

        Map<String, Object> dec = asMap(body.get("clientDecision"));
        if (dec != null) {
            if (dec.get("status") != null)
                lc.setClientDecision(str(dec.get("status")));
            if (dec.get("decidedAt") != null)
                lc.setDecidedAt(inst(dec.get("decidedAt")));
            if (dec.get("feedback") != null)
                lc.setClientFeedback(str(dec.get("feedback")));
        }

        Map<String, Object> bank = asMap(body.get("bankProcessing"));
        if (bank != null) {
            if (bank.get("applicationRef") != null)
                lc.setApplicationRef(str(bank.get("applicationRef")));
            if (bank.get("submittedDate") != null)
                lc.setSubmittedDate(ld(bank.get("submittedDate")));
            if (bank.get("sanctionedAt") != null)
                lc.setSanctionedAt(inst(bank.get("sanctionedAt")));
            if (bank.get("status") != null)
                lc.setBankStatus(str(bank.get("status")));
            if (bank.get("remarks") != null)
                lc.setBankRemarks(str(bank.get("remarks")));
        }

        if (body.get("invoiceId") != null)
            lc.setInvoiceId(UUID.fromString(body.get("invoiceId").toString()));

        if (transitioningToApproval) {
            createProposalForLoanCase(lc);
        }

        return toResponse(loanCaseRepository.save(lc));
    }

    private void createProposalForLoanCase(LoanCase lc) {
        List<Proposal> existing = proposalRepository.findByCaseIdAndCaseModelAndActiveTrue(lc.getId(), "LoanCase");
        if (!existing.isEmpty()) {
            return;
        }

        Proposal p = new Proposal();
        p.setClient(lc.getClient());
        p.setConsultant(lc.getConsultant());
        p.setDepartment("loans");
        p.setCaseId(lc.getId());
        p.setCaseModel("LoanCase");
        p.setTitle("LOAN Proposal Terms - " + lc.getCaseId());
        p.setSummary("Recommended loan terms and interest rate options.");

        Map<String, Object> details = new HashMap<>();
        details.put("recommendedBank", lc.getRecommendedBank());
        details.put("recommendedRate", lc.getRecommendedRate());
        details.put("recommendedTenure", lc.getRecommendedTenure());
        details.put("recommendedEMI", lc.getRecommendedEmi());
        details.put("requestedAmount", lc.getRequestedAmount());
        p.setDetails(details);
        p.setStatus("sent");

        Proposal saved = proposalRepository.save(p);

        // Sync proposal to organization B2B portal
        proposalService.syncToOrgProposal(saved);
        log.info("Automatically generated CRM proposal {} for LoanCase {}", saved.getId(), lc.getCaseId());

        // Notify client
        try {
            if (saved.getClient() != null) {
                notificationService.create(saved.getClient(), "proposal", "New Proposal",
                        "A new proposal has been prepared for you: \"" + saved.getTitle() + "\". Please review and approve/reject.");
            }
        } catch (Exception e) {
            log.error("Failed to notify client of new proposal: {}", e.getMessage());
        }
    }

    @Transactional
    public LoanCaseResponse updateDocument(UUID id, UUID docId, String status, String rejectionNote, User actor) {
        LoanCase lc = loadOwned(id, actor);
        LoanCaseDocument doc = lc.getDocuments().stream().filter(d -> d.getId().equals(docId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + docId));
        doc.setStatus(status);
        if ("Rejected".equals(status))
            doc.setRejectionNote(rejectionNote != null ? rejectionNote : "");
        if ("Uploaded".equals(status) || "Verified".equals(status))
            doc.setUploadedAt(Instant.now());
        
        LoanCaseResponse res = toResponse(loanCaseRepository.save(lc));

        // Notify client
        try {
            if (lc.getClient() != null) {
                if ("Verified".equals(status)) {
                    notificationService.create(lc.getClient(), "document", "Document Verified",
                            "Your document \"" + doc.getName() + "\" has been verified successfully.");
                } else if ("Rejected".equals(status)) {
                    notificationService.create(lc.getClient(), "document", "Document Rejected",
                            "Your document \"" + doc.getName() + "\" has been rejected. Reason: " + rejectionNote);
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify client of document status update: {}", e.getMessage());
        }

        return res;
    }

    @Transactional
    public LoanCaseResponse disburse(UUID id, Map<String, Object> body, User actor) {
        LoanCase lc = loadOwned(id, actor);
        lc.setDisbursedAmount(bd(body.get("disbursedAmount")));
        lc.setDisbursedDate(ld(body.get("disbursedDate")));
        lc.setInterestRate(bd(body.get("interestRate")));
        lc.setTenureMonths(in(body.get("tenureMonths")));
        lc.setMonthlyEmi(bd(body.get("monthlyEMI")));
        lc.setBankName(str(body.get("bankName")));
        lc.setStage("emi_tracking");
        lc.setBankStatus("Disbursed");

        // Generate the EMI schedule.
        lc.getEmiSchedule().clear();
        LocalDate start = lc.getDisbursedDate() != null ? lc.getDisbursedDate() : LocalDate.now();
        int tenure = lc.getTenureMonths() != null ? lc.getTenureMonths() : 0;
        BigDecimal emi = lc.getMonthlyEmi() != null ? lc.getMonthlyEmi() : BigDecimal.ZERO;
        for (int i = 1; i <= tenure; i++) {
            LocalDate due = start.plusMonths(i);
            EmiScheduleItem item = new EmiScheduleItem();
            item.setLoanCase(lc);
            item.setMonth(due.getMonth() + " " + due.getYear());
            item.setDueDate(due);
            item.setAmount(emi);
            item.setStatus("Pending");
            lc.getEmiSchedule().add(item);
        }
        log.info("Loan case {} disbursed with {} EMIs", lc.getCaseId(), tenure);
        return toResponse(loanCaseRepository.save(lc));
    }

    @Transactional
    public LoanCaseResponse updateEmi(UUID id, UUID emiId, String status, Object paidDate, User actor) {
        LoanCase lc = loadOwned(id, actor);
        EmiScheduleItem emi = lc.getEmiSchedule().stream().filter(e -> e.getId().equals(emiId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("EMI not found: " + emiId));
        emi.setStatus(status);
        if ("Paid".equals(status))
            emi.setPaidDate(paidDate != null ? ld(paidDate) : LocalDate.now());
        return toResponse(loanCaseRepository.save(lc));
    }

    @Transactional
    public LoanCaseResponse addNote(UUID id, String text, String addedBy, User actor) {
        LoanCase lc = loadOwned(id, actor);
        LoanCaseNote note = new LoanCaseNote();
        note.setLoanCase(lc);
        note.setText(text);
        note.setAddedBy(addedBy);
        note.setAddedAt(Instant.now());
        lc.getNotes().add(note);
        return toResponse(loanCaseRepository.save(lc));
    }

    private LoanCase load(UUID id) {
        return loanCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan case not found: " + id));
    }

    /**
     * Load and enforce that the acting user may mutate this case (consultants → own
     * cases only).
     */
    private LoanCase loadOwned(UUID id, User actor) {
        LoanCase lc = load(id);
        OwnershipGuard.assertConsultantOwns(actor, lc.getConsultant(), "loan case");
        return lc;
    }

    private LoanCaseResponse toResponse(LoanCase lc) {
        var client = lc.getClient() != null
                ? new LoanCaseResponse.Ref(lc.getClient().getId(), lc.getClient().getName())
                : null;
        var consultant = lc.getConsultant() != null
                ? new LoanCaseResponse.Ref(lc.getConsultant().getId(), lc.getConsultant().getName())
                : null;
        var docs = lc.getDocuments().stream().map(d -> new LoanCaseResponse.DocumentDto(
                d.getId(), d.getName(), d.getCategory(), d.getStatus(), d.getRejectionNote(), d.getUploadedAt()))
                .toList();
        var emis = lc.getEmiSchedule().stream().map(e -> new LoanCaseResponse.EmiDto(
                e.getId(), e.getMonth(), e.getDueDate(), e.getAmount(), e.getPenalty(), e.getPaidDate(), e.getStatus()))
                .toList();
        var notes = lc.getNotes().stream().map(n -> new LoanCaseResponse.NoteDto(
                n.getText(), n.getAddedBy(), n.getAddedAt())).toList();

        return new LoanCaseResponse(
                lc.getId(), lc.getCaseId(), client, consultant, lc.getLoanType(), lc.getRequestedAmount(),
                lc.getStage(),
                docs,
                new LoanCaseResponse.Eligibility(lc.getCreditScore(), lc.getDti(), lc.getLtv(), lc.getEligible(),
                        lc.getAnalystNote()),
                new LoanCaseResponse.Recommendation(lc.getRecommendedBank(), lc.getRecommendedRate(),
                        lc.getRecommendedTenure(), lc.getRecommendedEmi(), lc.getRecommendationNote(),
                        lc.getSentToClient()),
                new LoanCaseResponse.ClientDecision(lc.getClientDecision(), lc.getDecidedAt(), lc.getClientFeedback()),
                new LoanCaseResponse.BankProcessing(lc.getApplicationRef(), lc.getSubmittedDate(),
                        lc.getSanctionedAt(), lc.getBankStatus(), lc.getBankRemarks()),
                lc.getInvoiceId(), lc.getDisbursedAmount(), lc.getDisbursedDate(), lc.getInterestRate(),
                lc.getTenureMonths(), lc.getMonthlyEmi(), lc.getBankName(), emis, notes, lc.getCreatedAt());
    }

    // ── JSON value coercion helpers ──────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }

    private BigDecimal bd(Object o) {
        if (o == null || o.toString().isBlank())
            return null;
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer in(Object o) {
        BigDecimal b = bd(o);
        return b == null ? null : b.intValue();
    }

    private Boolean bool(Object o) {
        return o == null ? null : Boolean.valueOf(o.toString());
    }

    private LocalDate ld(Object o) {
        if (o == null || o.toString().isBlank())
            return null;
        String s = o.toString();
        return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s);
    }

    private Instant inst(Object o) {
        if (o == null || o.toString().isBlank())
            return null;
        String s = o.toString();
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            try {
                return LocalDate.parse(s.substring(0, 10)).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
