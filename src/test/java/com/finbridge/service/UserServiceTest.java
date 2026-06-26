package com.finbridge.service;

import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService userService;

    @Test
    void getById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getConsultants_shouldReturnOnlyConsultants() {
        User c1 = new User(); c1.setRole("consultant"); c1.setName("Alice");
        User c2 = new User(); c2.setRole("consultant"); c2.setName("Bob");
        when(userRepository.findByRoleOrderByCreatedAtDesc("consultant")).thenReturn(List.of(c1, c2));

        List<User> result = userService.getConsultants();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(u -> u.getRole().equals("consultant"));
    }

    @Test
    void updateActive_shouldSetActiveFlag() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setActive(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateActive(user.getId(), false);

        assertThat(result.isActive()).isFalse();
    }
}
