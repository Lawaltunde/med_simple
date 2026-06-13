package com.medsimple.notification.repository;

import com.medsimple.notification.entity.Notification;
import com.medsimple.notification.entity.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // In-app feed for a user, newest first.
    List<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(UUID userId, NotificationChannel channel);

    // Unread in-app notifications.
    List<Notification> findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
            UUID userId, NotificationChannel channel);

    long countByUserIdAndChannelAndReadAtIsNull(UUID userId, NotificationChannel channel);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
}
