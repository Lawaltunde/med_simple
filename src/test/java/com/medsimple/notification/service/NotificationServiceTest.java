package com.medsimple.notification.service;

import com.medsimple.notification.dao.EmailDeliveryDao;
import com.medsimple.notification.dto.NotificationResponse;
import com.medsimple.notification.entity.Notification;
import com.medsimple.notification.entity.NotificationChannel;
import com.medsimple.notification.entity.NotificationType;
import com.medsimple.notification.exception.NotificationNotFoundException;
import com.medsimple.notification.repository.NotificationRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock EmailDeliveryDao emailDeliveryDao;
    @Mock EntityManager entityManager;
    @Mock com.medsimple.auth.security.AccessGuard accessGuard;

    @InjectMocks NotificationService notificationService;

    @Captor ArgumentCaptor<Notification> notificationCaptor;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).role(UserRole.PATIENT)
                .firstName("Jane").lastName("Doe").email("jane@example.com").build();
    }

    // ── ownership gate ──────────────────────────────────────────────────────────

    @Test
    void getFeed_deniedForAnotherUser() {
        doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
                .when(accessGuard).requireSelf(userId);

        assertThatThrownBy(() -> notificationService.getFeed(userId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verifyNoInteractions(notificationRepository);
    }

    // ── notify() — dual-channel ─────────────────────────────────────────────────

    @Test
    void notify_createsBothInAppAndEmailRows() {
        when(entityManager.find(User.class, userId)).thenReturn(user);
        when(emailDeliveryDao.send(eq("jane@example.com"), anyString(), anyString())).thenReturn(true);

        notificationService.notify(userId, NotificationType.RESULT_READY, "Result ready", "Your test is ready");

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        List<Notification> saved = notificationCaptor.getAllValues();
        assertThat(saved).extracting(Notification::getChannel)
                .containsExactlyInAnyOrder(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
        // Email was dispatched to the user's address.
        verify(emailDeliveryDao).send("jane@example.com", "Result ready", "Your test is ready");
    }

    @Test
    void notify_inAppHasSentAtImmediately() {
        when(entityManager.find(User.class, userId)).thenReturn(user);
        when(emailDeliveryDao.send(any(), any(), any())).thenReturn(true);

        notificationService.notify(userId, NotificationType.URGENCY_UPDATED, "t", "b");

        verify(notificationRepository, times(2)).save(notificationCaptor.capture());
        Notification inApp = notificationCaptor.getAllValues().stream()
                .filter(n -> n.getChannel() == NotificationChannel.IN_APP).findFirst().orElseThrow();
        assertThat(inApp.getSentAt()).isNotNull();
    }

    @Test
    void notify_emailFailure_doesNotThrow_inAppStillSaved() {
        when(entityManager.find(User.class, userId)).thenReturn(user);
        when(emailDeliveryDao.send(any(), any(), any())).thenThrow(new RuntimeException("SMTP down"));

        assertThatNoException().isThrownBy(() ->
                notificationService.notify(userId, NotificationType.EMERGENCY_ALERT, "t", "b"));
        // Both rows still persisted; the email row just has no sent_at.
        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void notify_unknownUser_isNoOp() {
        when(entityManager.find(User.class, userId)).thenReturn(null);

        notificationService.notify(userId, NotificationType.RESULT_READY, "t", "b");

        verify(notificationRepository, never()).save(any());
        verify(emailDeliveryDao, never()).send(any(), any(), any());
    }

    // ── reads ───────────────────────────────────────────────────────────────────

    @Test
    void getUnread_returnsOnlyInAppUnread() {
        Notification n = Notification.builder().id(UUID.randomUUID()).user(user)
                .type(NotificationType.RESULT_READY).channel(NotificationChannel.IN_APP)
                .title("t").body("b").createdAt(OffsetDateTime.now()).build();
        when(notificationRepository.findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                userId, NotificationChannel.IN_APP)).thenReturn(List.of(n));

        List<NotificationResponse> unread = notificationService.getUnread(userId);

        assertThat(unread).hasSize(1);
        assertThat(unread.get(0).read()).isFalse();
    }

    @Test
    void markAsRead_setsReadAt() {
        UUID notifId = UUID.randomUUID();
        Notification n = Notification.builder().id(notifId).user(user)
                .type(NotificationType.RESULT_READY).channel(NotificationChannel.IN_APP)
                .title("t").body("b").createdAt(OffsetDateTime.now()).build();
        when(notificationRepository.findByIdAndUserId(notifId, userId)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(userId, notifId);

        assertThat(response.read()).isTrue();
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    void markAsRead_alreadyRead_isIdempotent() {
        UUID notifId = UUID.randomUUID();
        OffsetDateTime already = OffsetDateTime.now().minusHours(1);
        Notification n = Notification.builder().id(notifId).user(user)
                .type(NotificationType.RESULT_READY).channel(NotificationChannel.IN_APP)
                .title("t").body("b").readAt(already).createdAt(OffsetDateTime.now()).build();
        when(notificationRepository.findByIdAndUserId(notifId, userId)).thenReturn(Optional.of(n));

        notificationService.markAsRead(userId, notifId);

        assertThat(n.getReadAt()).isEqualTo(already); // unchanged
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_notFound_throws() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUserId(notifId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(userId, notifId))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void markAllAsRead_marksEveryUnread() {
        Notification a = Notification.builder().id(UUID.randomUUID()).user(user)
                .type(NotificationType.RESULT_READY).channel(NotificationChannel.IN_APP)
                .title("t").body("b").createdAt(OffsetDateTime.now()).build();
        Notification b = Notification.builder().id(UUID.randomUUID()).user(user)
                .type(NotificationType.URGENCY_UPDATED).channel(NotificationChannel.IN_APP)
                .title("t").body("b").createdAt(OffsetDateTime.now()).build();
        when(notificationRepository.findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                userId, NotificationChannel.IN_APP)).thenReturn(List.of(a, b));

        int marked = notificationService.markAllAsRead(userId);

        assertThat(marked).isEqualTo(2);
        assertThat(a.getReadAt()).isNotNull();
        assertThat(b.getReadAt()).isNotNull();
    }

    @Test
    void unreadCount_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndChannelAndReadAtIsNull(userId, NotificationChannel.IN_APP))
                .thenReturn(3L);

        assertThat(notificationService.unreadCount(userId)).isEqualTo(3L);
    }
}
