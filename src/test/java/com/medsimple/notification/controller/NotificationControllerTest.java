package com.medsimple.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medsimple.config.GlobalExceptionHandler;
import com.medsimple.notification.dto.NotificationResponse;
import com.medsimple.notification.entity.NotificationType;
import com.medsimple.notification.exception.NotificationNotFoundException;
import com.medsimple.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock NotificationService notificationService;
    @InjectMocks NotificationController controller;

    private MockMvc mockMvc;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        userId = UUID.randomUUID();
    }

    private NotificationResponse sample(boolean read) {
        return new NotificationResponse(UUID.randomUUID(), NotificationType.RESULT_READY,
                "Result ready", "Your test is ready", read, read ? OffsetDateTime.now() : null,
                OffsetDateTime.now());
    }

    @Test
    void getFeed_returns200() throws Exception {
        when(notificationService.getFeed(userId)).thenReturn(List.of(sample(false)));

        mockMvc.perform(get("/api/v1/users/{userId}/notifications", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("RESULT_READY"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void getFeed_unreadOnly_usesUnreadQuery() throws Exception {
        when(notificationService.getUnread(userId)).thenReturn(List.of(sample(false)));

        mockMvc.perform(get("/api/v1/users/{userId}/notifications", userId).param("unreadOnly", "true"))
                .andExpect(status().isOk());

        verify(notificationService).getUnread(userId);
        verify(notificationService, never()).getFeed(any());
    }

    @Test
    void unreadCount_returns200() throws Exception {
        when(notificationService.unreadCount(userId)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/users/{userId}/notifications/unread-count", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    void markAsRead_returns200() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(notificationService.markAsRead(userId, notifId)).thenReturn(sample(true));

        mockMvc.perform(post("/api/v1/users/{userId}/notifications/{id}/read", userId, notifId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    void markAsRead_notFound_returns404() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(notificationService.markAsRead(userId, notifId))
                .thenThrow(new NotificationNotFoundException(notifId));

        mockMvc.perform(post("/api/v1/users/{userId}/notifications/{id}/read", userId, notifId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void markAllAsRead_returns200WithCount() throws Exception {
        when(notificationService.markAllAsRead(userId)).thenReturn(4);

        mockMvc.perform(post("/api/v1/users/{userId}/notifications/read-all", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked").value(4));
    }
}
