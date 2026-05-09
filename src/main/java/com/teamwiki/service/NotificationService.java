package com.teamwiki.service;

import com.teamwiki.dto.NotificationDTO;
import com.teamwiki.entity.Notification;
import com.teamwiki.entity.User;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.NotificationRepository;
import com.teamwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.teamwiki.dto.PageResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public PageResponse<NotificationDTO> getNotifications(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notifPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<NotificationDTO> notifs = notifPage.getContent().stream()
                .map(this::toNotificationDTO)
                .collect(Collectors.toList());

        return PageResponse.<NotificationDTO>builder()
                .content(notifs)
                .totalElements(notifPage.getTotalElements())
                .totalPages(notifPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    public Long getUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("通知不存在", HttpStatus.NOT_FOUND));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessException("无权操作", HttpStatus.FORBIDDEN);
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
        Page<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        notifs.getContent().forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifs.getContent());
    }

    @Transactional
    public void createNotification(Long userId, Notification.NotificationType type, String message, Long documentId) {
        Notification notification = Notification.builder()
                .user(User.builder().id(userId).build())
                .type(type)
                .message(message)
                .documentId(documentId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    private NotificationDTO toNotificationDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .message(notification.getMessage())
                .payload(notification.getPayload())
                .isRead(notification.getIsRead())
                .documentId(notification.getDocumentId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
