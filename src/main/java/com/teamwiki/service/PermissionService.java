package com.teamwiki.service;

import com.teamwiki.dto.*;
import com.teamwiki.entity.*;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.DocumentPermissionRepository;
import com.teamwiki.repository.DocumentRepository;
import com.teamwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final DocumentPermissionRepository permissionRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public List<PermissionDTO> getDocumentPermissions(Long documentId) {
        return permissionRepository.findByDocumentId(documentId).stream()
                .map(this::toPermissionDTO)
                .collect(Collectors.toList());
    }

    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toPermissionDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public PermissionDTO addPermission(PermissionRequest request) {
        Document doc = documentRepository.findById(request.getDocumentId() != null ? request.getDocumentId() : 0L)
                .orElseThrow(() -> new BusinessException("文档不存在", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        DocumentPermission existing = permissionRepository.findByDocumentIdAndUserId(doc.getId(), user.getId())
                .orElse(null);

        if (existing != null) {
            existing.setPermissionLevel(DocumentPermission.PermissionLevel.valueOf(request.getPermissionLevel()));
            existing = permissionRepository.save(existing);
            return toPermissionDTO(existing);
        }

        DocumentPermission permission = DocumentPermission.builder()
                .document(doc)
                .user(user)
                .permissionLevel(DocumentPermission.PermissionLevel.valueOf(request.getPermissionLevel()))
                .build();

        permission = permissionRepository.save(permission);
        return toPermissionDTO(permission);
    }

    @Transactional
    public void removePermission(Long documentId, Long userId) {
        permissionRepository.deleteByDocumentIdAndUserId(documentId, userId);
    }

    public boolean hasPermission(Long documentId, Long userId, DocumentPermission.PermissionLevel level) {
        return permissionRepository.findByDocumentIdAndUserId(documentId, userId)
                .map(p -> p.getPermissionLevel().ordinal() >= level.ordinal())
                .orElse(false);
    }

    private PermissionDTO toPermissionDTO(DocumentPermission permission) {
        return PermissionDTO.builder()
                .id(permission.getId())
                .documentId(permission.getDocument().getId())
                .documentTitle(permission.getDocument().getTitle())
                .user(toUserDTO(permission.getUser()))
                .permissionLevel(permission.getPermissionLevel().name())
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
