package com.medsimple.notification.service;

import com.medsimple.auth.security.AccessGuard;
import com.medsimple.notification.dao.EmailDeliveryDao;
import com.medsimple.notification.dto.NotificationResponse;
import com.medsimple.notification.entity.Notification;
import com.medsimple.notification.entity.NotificationChannel;
import com.medsimple.notification.entity.NotificationType;
import com.medsimple.notification.exception.NotificationNotFoundException;
import com.medsimple.notification.repository.NotificationRepository;
import com.medsimple.shared.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailDeliveryDao emailDeliveryDao;
    private final EntityManager entityManager;
    private final AccessGuard accessGuard;

    /**
     * The contract every other service calls. Produces both an IN_APP row (available
     * immediately) and an EMAIL row (dispatched via EmailDeliveryDao). Never throws into
     * the caller's transaction for a delivery hiccup — a failed email is logged, not fatal.
     */
    @Transactional
    public void notify(UUID userId, NotificationType type, String title, String body) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            log.warn("notify() called for unknown user {} (type {})", userId, type);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        Notification inApp = Notification.builder()
                .user(user).type(type).channel(NotificationChannel.IN_APP)
                .title(title).body(body).sentAt(now).build();
        notificationRepository.save(inApp);

        Notification email = Notification.builder()
                .user(user).type(type).channel(NotificationChannel.EMAIL)
                .title(title).body(body).build();
        try {
            if (emailDeliveryDao.send(user.getEmail(), title, body)) {
                email.setSentAt(OffsetDateTime.now());
            }
        } catch (Exception e) {
            log.error("Email dispatch failed for notification to {} (type {})", userId, type, e);
        }
        notificationRepository.save(email);
    }

    // ── In-app feed reads ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NotificationResponse> getFeed(UUID userId) {
        accessGuard.requireSelf(userId);
        return notificationRepository
                .findByUserIdAndChannelOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnread(UUID userId) {
        accessGuard.requireSelf(userId);
        return notificationRepository
                .findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        accessGuard.requireSelf(userId);
        return notificationRepository.countByUserIdAndChannelAndReadAtIsNull(userId, NotificationChannel.IN_APP);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        accessGuard.requireSelf(userId);
        Notification n = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (n.getReadAt() == null) {
            n.setReadAt(OffsetDateTime.now());
            notificationRepository.save(n);
        }
        return toResponse(n);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        accessGuard.requireSelf(userId);
        List<Notification> unread = notificationRepository
                .findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(userId, NotificationChannel.IN_APP);
        OffsetDateTime now = OffsetDateTime.now();
        unread.forEach(n -> n.setReadAt(now));
        notificationRepository.saveAll(unread);
        return unread.size();
    }

    // ── Helper ──────────────────────────────────────────────────────────────────

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getBody(),
                n.getReadAt() != null, n.getReadAt(), n.getCreatedAt());
    }
}
