package com.teamwiki.service;

import com.teamwiki.dto.ActivityDTO;
import com.teamwiki.dto.PageResponse;
import com.teamwiki.dto.UserDTO;
import com.teamwiki.entity.Activity;
import com.teamwiki.entity.User;
import com.teamwiki.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    public PageResponse<ActivityDTO> getActivities(int page, int size, String type) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Activity> activityPage;

        if (type != null && !type.isEmpty()) {
            activityPage = activityRepository.findByTypeOrderByCreatedAtDesc(Activity.ActivityType.valueOf(type), pageable);
        } else {
            activityPage = activityRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<ActivityDTO> activities = activityPage.getContent().stream()
                .map(this::toActivityDTO)
                .collect(Collectors.toList());

        return PageResponse.<ActivityDTO>builder()
                .content(activities)
                .totalElements(activityPage.getTotalElements())
                .totalPages(activityPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    private ActivityDTO toActivityDTO(Activity activity) {
        return ActivityDTO.builder()
                .id(activity.getId())
                .user(toUserDTO(activity.getUser()))
                .documentId(activity.getDocument() != null ? activity.getDocument().getId() : null)
                .documentTitle(activity.getDocument() != null ? activity.getDocument().getTitle() : null)
                .type(activity.getType().name())
                .description(activity.getDescription())
                .createdAt(activity.getCreatedAt())
                .build();
    }

    private UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .department(user.getDepartment())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .build();
    }
}
