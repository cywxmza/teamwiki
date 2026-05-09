package com.teamwiki.controller;

import com.teamwiki.dto.*;
import com.teamwiki.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationDTO>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute("username") String username) {
        return ResponseEntity.ok(notificationService.getNotifications(username, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestAttribute("username") String username) {
        return ResponseEntity.ok(notificationService.getUnreadCount(username));
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id,
                                            @RequestAttribute("username") String username) {
        notificationService.markAsRead(id, username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@RequestAttribute("username") String username) {
        notificationService.markAllAsRead(username);
        return ResponseEntity.ok().build();
    }
}
