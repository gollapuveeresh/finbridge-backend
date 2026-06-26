package com.finbridge.service;

import com.finbridge.entity.Notification;
import com.finbridge.entity.User;
import com.finbridge.exception.ResourceNotFoundException;
import com.finbridge.repository.NotificationRepository;
import com.finbridge.security.OwnershipGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public List<Notification> getByUser(UUID userId) {
        return notificationRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public Notification create(User user, String type, String title, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        Notification saved = notificationRepository.save(n);

        try {
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                emailService.sendNotificationEmail(
                        user.getEmail(),
                        user.getName(),
                        title,
                        message,
                        type,
                        saved.getId()
                );
            }
        } catch (Exception e) {
            log.error("Failed to send email notification to {}: {}", user.getEmail(), e.getMessage());
        }

        return saved;
    }

    @Transactional
    public void markRead(UUID id, User actor) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        OwnershipGuard.assertOwnerOrAdmin(actor, n.getUser(), "notification");
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
    }
}
