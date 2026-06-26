package com.finbridge.service;

import com.finbridge.entity.DeptCase;
import com.finbridge.entity.Lead;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.DeptCaseRepository;
import com.finbridge.repository.LeadRepository;
import com.finbridge.repository.UserRepository;
import com.finbridge.repository.ProposalRepository;
import com.finbridge.entity.Proposal;
import com.finbridge.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeptCaseService {

    private static final List<String> DEFAULT_DOCS = List.of("PAN Card", "Aadhaar Card", "Income Proof",
            "Bank Statements");
    // Keys handled as first-class columns — everything else in the create/patch
    // body goes into JSONB data.
    private static final Set<String> RESERVED = Set.of("clientId", "leadId", "stage", "department", "invoiceId");

    private final DeptCaseRepository deptCaseRepository;
    private final UserRepository userRepository;
    private final LeadRepository leadRepository;
    private final SequenceGenerator sequenceGenerator;
    private final ProposalRepository proposalRepository;
    private final ProposalService proposalService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getByDepartment(String department) {
        return deptCaseRepository.findByDepartmentAndActiveTrueOrderByCreatedAtDesc(department)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public Map<String, Object> create(String department, Map<String, Object> body, User consultant) {
        DeptCase c = new DeptCase();
        c.setCaseId(department.substring(0, Math.min(3, department.length())).toUpperCase()
                + "-" + String.format("%05d", sequenceGenerator.nextValue(SequenceGenerator.Seq.DEPT_CASE)));
        c.setDepartment(department);
        c.setConsultant(consultant);
        c.setStage("document_collection");

        if (body.get("clientId") != null)
            userRepository.findById(UUID.fromString(body.get("clientId").toString())).ifPresent(c::setClient);
        if (body.get("leadId") != null) {
            Lead lead = leadRepository.findById(UUID.fromString(body.get("leadId").toString())).orElse(null);
            c.setLead(lead);
            if (c.getClient() == null && lead != null && lead.getConvertedClient() != null)
                c.setClient(lead.getConvertedClient());
        }

        Map<String, Object> data = new HashMap<>();
        // Seed the document checklist.
        List<Map<String, Object>> docs = new ArrayList<>();
        for (String name : DEFAULT_DOCS) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("_id", UUID.randomUUID().toString());
            d.put("name", name);
            d.put("category", "KYC");
            d.put("status", "Pending");
            d.put("rejectionNote", "");
            d.put("uploadedAt", null);
            docs.add(d);
        }
        data.put("documents", docs);
        data.put("notes", new ArrayList<>());
        // Carry any extra dept-specific fields from the create payload.
        body.forEach((k, v) -> {
            if (!RESERVED.contains(k))
                data.put(k, v);
        });
        c.setData(data);

        log.info("Dept case created: {} ({})", c.getCaseId(), department);
        return toResponse(deptCaseRepository.save(c));
    }

    @Transactional
    public Map<String, Object> patch(UUID id, Map<String, Object> body, User actor) {
        DeptCase c = loadOwned(id, actor);
        Map<String, Object> data = new HashMap<>(c.getData() != null ? c.getData() : new HashMap<>());

        boolean transitioningToApproval = false;
        if (body.containsKey("stage")) {
            String newStage = body.get("stage") != null ? body.get("stage").toString() : null;
            if ("client_approval".equals(newStage) && !"client_approval".equals(c.getStage())) {
                transitioningToApproval = true;
            }
        }

        body.forEach((k, v) -> {
            if ("stage".equals(k))
                c.setStage(v == null ? null : v.toString());
            else if ("invoiceId".equals(k) && v != null)
                c.setInvoiceId(UUID.fromString(v.toString()));
            else if (!RESERVED.contains(k))
                data.put(k, v);
        });

        if (transitioningToApproval) {
            createProposalForCase(c, data);
        }

        c.setData(data);
        return toResponse(deptCaseRepository.save(c));
    }

    private void createProposalForCase(DeptCase c, Map<String, Object> data) {
        // Double check if a proposal already exists for this case
        List<Proposal> existing = proposalRepository.findByCaseIdAndCaseModelAndActiveTrue(c.getId(), "DeptCase");
        if (!existing.isEmpty()) {
            return;
        }

        Proposal p = new Proposal();
        p.setClient(c.getClient());
        p.setConsultant(c.getConsultant());
        p.setDepartment(c.getDepartment());
        p.setCaseId(c.getId());
        p.setCaseModel("DeptCase");
        p.setTitle(c.getDepartment().toUpperCase() + " Recommendations Plan - " + c.getCaseId());
        p.setSummary("Advisory plan and recommendations generated for your " + c.getDepartment() + " case.");

        Map<String, Object> details = new HashMap<>();
        if (data.get("recommendations") != null) {
            details.put("recommendations", data.get("recommendations"));
        } else if (data.get("recommendation") != null) {
            details.put("recommendation", data.get("recommendation"));
        } else if (data.get("assetAllocation") != null) {
            details.put("assetAllocation", data.get("assetAllocation"));
        } else if (data.get("portfolio") != null) {
            details.put("portfolio", data.get("portfolio"));
        }
        p.setDetails(details);
        p.setStatus("sent");

        Proposal saved = proposalRepository.save(p);
        data.put("proposalId", saved.getId().toString());

        // Sync proposal to organization B2B portal
        proposalService.syncToOrgProposal(saved);
        log.info("Automatically generated CRM proposal {} for DeptCase {}", saved.getId(), c.getCaseId());

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
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateDocument(UUID id, String docId, String status, String rejectionNote, User actor) {
        DeptCase c = loadOwned(id, actor);
        Map<String, Object> data = new HashMap<>(c.getData() != null ? c.getData() : new HashMap<>());
        List<Map<String, Object>> docs = (List<Map<String, Object>>) data.getOrDefault("documents", new ArrayList<>());
        String docName = "Document";
        for (Map<String, Object> d : docs) {
            if (docId.equals(String.valueOf(d.get("_id")))) {
                d.put("status", status);
                if ("Rejected".equals(status))
                    d.put("rejectionNote", rejectionNote != null ? rejectionNote : "");
                if ("Uploaded".equals(status) || "Verified".equals(status))
                    d.put("uploadedAt", Instant.now().toString());
                docName = String.valueOf(d.get("name"));
                break;
            }
        }
        data.put("documents", docs);
        c.setData(data);
        Map<String, Object> res = toResponse(deptCaseRepository.save(c));

        // Notify client
        try {
            if (c.getClient() != null) {
                if ("Verified".equals(status)) {
                    notificationService.create(c.getClient(), "document", "Document Verified",
                            "Your document \"" + docName + "\" has been verified successfully.");
                } else if ("Rejected".equals(status)) {
                    notificationService.create(c.getClient(), "document", "Document Rejected",
                            "Your document \"" + docName + "\" has been rejected. Reason: " + rejectionNote);
                }
            }
        } catch (Exception e) {
            log.error("Failed to notify client of document status update: {}", e.getMessage());
        }

        return res;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> addNote(UUID id, String text, String addedBy, User actor) {
        DeptCase c = loadOwned(id, actor);
        Map<String, Object> data = new HashMap<>(c.getData() != null ? c.getData() : new HashMap<>());
        List<Map<String, Object>> notes = (List<Map<String, Object>>) data.getOrDefault("notes", new ArrayList<>());
        Map<String, Object> note = new LinkedHashMap<>();
        note.put("text", text);
        note.put("addedBy", addedBy);
        note.put("addedAt", Instant.now().toString());
        notes.add(note);
        data.put("notes", notes);
        c.setData(data);
        return toResponse(deptCaseRepository.save(c));
    }

    private DeptCase load(UUID id) {
        return deptCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + id));
    }

    /**
     * Load and enforce that the acting user may mutate this case (consultants → own
     * cases only).
     */
    private DeptCase loadOwned(UUID id, User actor) {
        DeptCase c = load(id);
        OwnershipGuard.assertConsultantOwns(actor, c.getConsultant(), "case");
        return c;
    }

    /**
     * Merges base columns + the JSONB data blob into the flat shape the frontend
     * expects.
     */
    private Map<String, Object> toResponse(DeptCase c) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("_id", c.getId());
        out.put("id", c.getId());
        out.put("caseId", c.getCaseId());
        out.put("department", c.getDepartment());
        out.put("stage", c.getStage());
        out.put("invoiceId", c.getInvoiceId());
        out.put("clientId", c.getClient() == null ? null : ref(c.getClient()));
        out.put("consultantId", c.getConsultant() == null ? null : ref(c.getConsultant()));
        out.put("createdAt", c.getCreatedAt());
        if (c.getData() != null)
            out.putAll(c.getData());
        return out;
    }

    private Map<String, Object> ref(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("_id", u.getId());
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        return m;
    }
}
