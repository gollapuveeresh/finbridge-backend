package com.finbridge.service;

import com.finbridge.entity.Lead;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.LeadRepository;
import com.finbridge.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock LeadRepository leadRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock NotificationService notificationService;
    @Mock SequenceGenerator sequenceGenerator;
    @Mock com.finbridge.repository.OrganizationRepository organizationRepository;
    @Mock com.finbridge.repository.OrganizationUserRepository organizationUserRepository;
    @Mock com.finbridge.repository.ServiceRequestRepository serviceRequestRepository;
    @Mock com.finbridge.repository.OrganizationProposalRepository organizationProposalRepository;
    @Mock com.finbridge.repository.ConsultationRepository consultationRepository;
    @Mock EmailService emailService;
    @InjectMocks LeadService leadService;

    private Lead lead;

    @BeforeEach
    void setUp() {
        lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setName("John Doe");
        lead.setEmail("john@example.com");
        lead.setPhone("9999999999");
        lead.setIncome(new BigDecimal("1800000"));
        lead.setBudget(new BigDecimal("12000000"));
        lead.setRequirement("Home Loan");
    }

    @Test
    void create_shouldSetLeadIdAndScore() {
        when(sequenceGenerator.next(any())).thenReturn("LEAD-00001");
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead result = leadService.create(lead);

        assertThat(result.getLeadId()).isEqualTo("LEAD-00001");
        assertThat(result.getScore()).isGreaterThan(0);
        assertThat(result.getPriority()).isEqualTo("hot");
    }

    @Test
    void create_shouldScoreHotWhenHighIncomeAndBudget() {
        when(sequenceGenerator.next(any())).thenReturn("LEAD-00001");
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead result = leadService.create(lead);

        // income>=15L(+35) + budget>=1Cr(+35) + requirement(+15) + phone(+10) + email(+5) = 100
        assertThat(result.getScore()).isEqualTo(100);
        assertThat(result.getPriority()).isEqualTo("hot");
    }

    @Test
    void create_shouldScoreColdWhenNoData() {
        when(sequenceGenerator.next(any())).thenReturn("LEAD-00001");
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Lead cold = new Lead();
        cold.setName("Jane");
        cold.setEmail("jane@example.com");

        Lead result = leadService.create(cold);

        assertThat(result.getScore()).isLessThan(35);
        assertThat(result.getPriority()).isEqualTo("cold");
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(leadRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.getById(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Lead not found");
    }

    @Test
    void getById_shouldReturnLeadWhenFound() {
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));

        Lead result = leadService.getById(lead.getId());

        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void convertToClient_shouldCreateClientAndSetStatusWon() {
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmailIgnoreCase(lead.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LeadService.ConversionResult result = leadService.convertToClient(lead.getId());

        assertThat(lead.getStatus()).isEqualTo("won");
        assertThat(result.isNewClient()).isTrue();
        assertThat(result.tempPassword()).isNotBlank();
        assertThat(result.client().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void convertToClient_shouldLinkExistingClient() {
        User existing = new User();
        existing.setEmail(lead.getEmail());
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByEmailIgnoreCase(lead.getEmail())).thenReturn(Optional.of(existing));

        LeadService.ConversionResult result = leadService.convertToClient(lead.getId());

        assertThat(result.isNewClient()).isFalse();
        assertThat(result.client()).isSameAs(existing);
    }
}
