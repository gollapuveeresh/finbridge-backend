package com.finbridge.repository;

import com.finbridge.entity.OrganizationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationUserRepository extends JpaRepository<OrganizationUser, UUID> {
    Optional<OrganizationUser> findByEmail(String email);
    // Case-insensitive variants — email is a natural key and must not be case-sensitive at login.
    Optional<OrganizationUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<OrganizationUser> findByOrganizationId(UUID organizationId);
    boolean existsByEmail(String email);
}
