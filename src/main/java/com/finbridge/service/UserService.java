package com.finbridge.service;

import com.finbridge.entity.User;
import com.finbridge.exception.BadRequestException;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final List<String> ADMIN_ROLES = List.of("crm-admin", "department-admin");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllUsers() { return userRepository.findAll(); }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public List<User> getConsultants() {
        return userRepository.findByRoleOrderByCreatedAtDesc("consultant");
    }

    public List<User> getConsultantsByDepartment(String department) {
        return userRepository.findByRoleAndDepartmentOrderByCreatedAtDesc("consultant", department);
    }

    public List<User> getClients() {
        return userRepository.findByRoleOrderByCreatedAtDesc("client");
    }

    public List<User> getClientsByDepartment(String department) {
        return userRepository.findByRoleAndDepartmentOrderByCreatedAtDesc("client", department);
    }

    /** Admins, optionally filtered by a specific admin role and/or department. */
    public List<User> getAdmins(String role, String department) {
        if (role != null && department != null)
            return userRepository.findByRoleAndDepartmentOrderByCreatedAtDesc(role, department);
        if (role != null)
            return userRepository.findByRoleOrderByCreatedAtDesc(role);
        if (department != null)
            return userRepository.findByRoleInAndDepartmentOrderByCreatedAtDesc(ADMIN_ROLES, department);
        return userRepository.findByRoleInOrderByCreatedAtDesc(ADMIN_ROLES);
    }

    @Transactional
    public User createStaff(String name, String email, String rawPassword,
                            String phone, String role, String department) {
        if (userRepository.existsByEmailIgnoreCase(email))
            throw new BadRequestException("Email already registered: " + email);
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(rawPassword != null ? rawPassword : "Welcome@123"));
        u.setRole(role);
        u.setDepartment(department);
        u.setPhone(phone);
        u.setEmailVerified(true); // staff created by an admin are pre-verified
        return userRepository.save(u);
    }

    @Transactional
    public User updateStaff(UUID id, Boolean active, String name, String phone, String department) {
        User user = getById(id);
        if (active != null)     user.setActive(active);
        if (name != null)       user.setName(name);
        if (phone != null)      user.setPhone(phone);
        if (department != null) user.setDepartment(department);
        return userRepository.save(user);
    }

    public User save(User user) { return userRepository.save(user); }

    public User updateActive(UUID id, boolean active) {
        User user = getById(id);
        user.setActive(active);
        return userRepository.save(user);
    }

    @Transactional
    public void delete(UUID id) {
        User user = getById(id);
        // Soft-deactivate rather than hard-delete to preserve referential history.
        user.setActive(false);
        userRepository.save(user);
    }
}
