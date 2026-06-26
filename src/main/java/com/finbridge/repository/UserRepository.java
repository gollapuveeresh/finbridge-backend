package com.finbridge.repository;

import com.finbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    // Case-insensitive variants — email is a natural key and must not be case-sensitive at login.
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRoleAndActiveTrue(String role);
    List<User> findByRoleAndDepartmentAndActiveTrue(String role, String department);
    boolean existsByEmail(String email);

    List<User> findByRoleOrderByCreatedAtDesc(String role);
    List<User> findByRoleAndDepartmentOrderByCreatedAtDesc(String role, String department);
    List<User> findByRoleInOrderByCreatedAtDesc(List<String> roles);
    List<User> findByRoleInAndDepartmentOrderByCreatedAtDesc(List<String> roles, String department);
}
