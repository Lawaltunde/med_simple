package com.medsimple.notification.controller;

import com.medsimple.notification.dto.NotificationResponse;
import com.medsimple.notification.dto.UnreadCountResponse;
import com.medsimple.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getFeed(@PathVariable UUID userId,
                                                              @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(unreadOnly
                ? notificationService.getUnread(userId)
                : notificationService.getFeed(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@PathVariable UUID userId) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.unreadCount(userId)));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID userId,
                                                          @PathVariable UUID notificationId) {
        return ResponseEntity.ok(notificationService.markAsRead(userId, notificationId));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(@PathVariable UUID userId) {
        return ResponseEntity.ok(Map.of("marked", notificationService.markAllAsRead(userId)));
    }
}
