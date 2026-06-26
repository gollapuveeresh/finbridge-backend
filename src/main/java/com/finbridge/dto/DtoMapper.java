package com.finbridge.dto;

import com.finbridge.entity.*;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    public UserResponse toUserResponse(User u) {
        return new UserResponse(
            u.getId(), u.getName(), u.getEmail(), u.getRole(),
            u.getDepartment(), u.getPhone(), u.getCompanyName(),
            u.isActive(), u.isEmailVerified(), u.getCreatedAt()
        );
    }

    public StaffResponse toStaffResponse(User u) {
        return new StaffResponse(
            u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole(),
            u.getDepartment(), u.getCompanyName(), u.isActive(),
            u.isEmailVerified(), u.getCreatedAt()
        );
    }

    public User toUser(RegisterRequest r) {
        User u = new User();
        u.setName(r.name());
        u.setEmail(r.email());
        u.setRole(r.role() != null ? r.role() : "client");
        u.setDepartment(r.department());
        u.setPhone(r.phone());
        u.setCompanyName(r.companyName());
        return u;
    }

    public Lead toLead(LeadRequest r) {
        Lead l = new Lead();
        l.setName(r.name());
        l.setEmail(r.email());
        l.setPhone(r.phone());
        l.setIncome(r.income());
        l.setRequirement(r.requirement());
        l.setBudget(r.budget());
        l.setSource(r.source() != null ? r.source() : "website_form");
        l.setDepartment(r.department());
        l.setServiceType(r.serviceType());
        return l;
    }

    public LeadResponse toLeadResponse(Lead l) {
        return new LeadResponse(
            l.getId(), l.getLeadId(), l.getName(), l.getEmail(), l.getPhone(),
            l.getIncome(), l.getRequirement(), l.getBudget(), l.getSource(),
            l.getStatus(), l.getPriority(), l.getScore(), l.getDepartment(),
            l.getServiceType(),
            l.getAssignedConsultant() != null ? l.getAssignedConsultant().getId() : null,
            l.getAssignedConsultant() != null ? l.getAssignedConsultant().getName() : null,
            l.getConvertedClient() != null ? l.getConvertedClient().getId() : null,
            l.getNotes().stream()
                .map(n -> new LeadNoteDto(n.getText(), n.getAddedBy(), n.getAddedAt()))
                .toList(),
            l.getFollowUpDate(), l.isActive(), l.getCreatedAt()
        );
    }

    public Proposal toProposal(ProposalRequest r, User consultant) {
        Proposal p = new Proposal();
        p.setConsultant(consultant);
        p.setDepartment(r.department());
        p.setTitle(r.title());
        p.setSummary(r.summary());
        p.setDetails(r.details());
        p.setValidUntil(r.validUntil() == null ? null
                : r.validUntil().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        return p;
    }

    public ProposalResponse toProposalResponse(Proposal p) {
        ProposalResponse.Ref lead = p.getLead() != null
                ? new ProposalResponse.Ref(p.getLead().getId(), p.getLead().getName()) : null;
        ProposalResponse.Ref client = p.getClient() != null
                ? new ProposalResponse.Ref(p.getClient().getId(), p.getClient().getName()) : null;
        ProposalResponse.Ref consultant = p.getConsultant() != null
                ? new ProposalResponse.Ref(p.getConsultant().getId(), p.getConsultant().getName()) : null;
        return new ProposalResponse(
            p.getId(), lead, client, consultant,
            p.getDepartment(), p.getTitle(), p.getSummary(), p.getDetails(),
            p.getStatus(), p.getClientFeedback(), p.getValidUntil(), p.getCreatedAt()
        );
    }

    public Loan toLoan(LoanRequest r, User user) {
        Loan l = new Loan();
        l.setUser(user);
        l.setLoanNumber(r.loanNumber());
        l.setLoanType(r.loanType());
        l.setLenderName(r.lenderName());
        l.setPrincipalAmount(r.principalAmount());
        l.setOutstandingBalance(r.outstandingBalance());
        l.setInterestRate(r.interestRate());
        l.setTenureMonths(r.tenureMonths());
        l.setMonthlyEmi(r.monthlyEmi());
        l.setStartDate(r.startDate());
        l.setEndDate(r.endDate());
        l.setNotes(r.notes() != null ? r.notes() : "");
        return l;
    }

    public Investment toInvestment(InvestmentRequest r, User user) {
        Investment i = new Investment();
        i.setUser(user);
        i.setInvestmentType(r.investmentType());
        i.setAmountInvested(r.amountInvested());
        i.setCurrentValue(r.currentValue());
        i.setPurchaseDate(r.purchaseDate());
        i.setRiskLevel(r.riskLevel());
        i.setNotes(r.notes() != null ? r.notes() : "");
        return i;
    }

    public Consultation toConsultation(ConsultationRequest r, User client) {
        Consultation c = new Consultation();
        c.setClient(client);
        c.setDepartment(r.department());
        c.setCategory(r.category());
        c.setClientNotes(r.clientNotes() != null ? r.clientNotes() : "");
        return c;
    }
}
