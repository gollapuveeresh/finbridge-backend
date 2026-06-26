package com.finbridge.service;

import com.finbridge.dto.DtoMapper;
import com.finbridge.dto.ProposalResponse;
import com.finbridge.entity.Proposal;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.LeadRepository;
import com.finbridge.repository.OrganizationProposalRepository;
import com.finbridge.repository.OrganizationUserRepository;
import com.finbridge.repository.ProposalRepository;
import com.finbridge.repository.UserRepository;
import com.finbridge.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    ProposalRepository proposalRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    LeadRepository leadRepository;
    @Mock
    OrganizationUserRepository orgUserRepository;
    @Mock
    OrganizationProposalRepository orgProposalRepository;
    @Mock
    com.finbridge.repository.DeptCaseRepository deptCaseRepository;
    @Mock
    com.finbridge.repository.LoanCaseRepository loanCaseRepository;
    @Mock
    EmailService emailService;
    @Mock
    NotificationService notificationService;
    ProposalService proposalService;

    private Proposal proposal;

    @BeforeEach
    void setUp() {
        // Use a real DtoMapper so we can assert on the mapped response.
        proposalService = new ProposalService(proposalRepository, userRepository, leadRepository, orgUserRepository,
                orgProposalRepository, new DtoMapper(), deptCaseRepository, loanCaseRepository, emailService, notificationService);
        proposal = new Proposal();
        proposal.setId(UUID.randomUUID());
        proposal.setTitle("Home Loan Proposal");
        proposal.setDepartment("loans");
        proposal.setStatus("draft");
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(proposalRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_shouldChangeStatusAndFeedback() {
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProposalResponse result = proposalService.updateStatus(proposal.getId(), "approved", "Looks good");

        assertThat(result.status()).isEqualTo("approved");
        assertThat(result.clientFeedback()).isEqualTo("Looks good");
    }

    @Test
    void updateStatus_shouldHandleNullFeedback() {
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProposalResponse result = proposalService.updateStatus(proposal.getId(), "rejected", null);

        assertThat(result.status()).isEqualTo("rejected");
        assertThat(result.clientFeedback()).isNull();
    }
}
