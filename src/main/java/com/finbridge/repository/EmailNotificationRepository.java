package com.finbridge.repository;

import com.finbridge.entity.EmailNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, UUID> {
    boolean existsByRecipientAndTypeAndRelatedEntityIdAndStatus(String recipient, String type, UUID relatedEntityId, String status);
}
