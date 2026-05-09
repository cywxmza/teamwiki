package com.teamwiki.service;

import com.teamwiki.dto.KbRequest;
import com.teamwiki.dto.KnowledgeBaseDTO;
import com.teamwiki.dto.UserDTO;
import com.teamwiki.entity.KnowledgeBase;
import com.teamwiki.entity.KbPermission;
import com.teamwiki.entity.User;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.KbPermissionRepository;
import com.teamwiki.repository.KnowledgeBaseRepository;
import com.teamwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository kbRepository;
    private final KbPermissionRepository kbPermissionRepository;
    private final UserRepository userRepository;

    public List<KnowledgeBaseDTO> getAccessibleKbs(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        List<KnowledgeBase> kbs = kbRepository.findAccessible(user.getId(), user.getDepartment(), KnowledgeBase.KbVisibility.PUBLIC);
        return kbs.stream().map(kb -> toKbDTO(kb, user.getId())).collect(Collectors.toList());
    }

    public KnowledgeBaseDTO getKbById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        KnowledgeBase kb = kbRepository.findById(id)
                .orElseThrow(() -> new BusinessException("知识库不存在", HttpStatus.NOT_FOUND));

        if (!canAccess(kb, user)) {
            throw new BusinessException("无权访问此知识库", HttpStatus.FORBIDDEN);
        }

        return toKbDTO(kb, user.getId());
    }

    @Transactional
    public KnowledgeBaseDTO createKb(KbRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        KnowledgeBase kb = KnowledgeBase.builder()
                .name(request.getName())
                .description(request.getDescription())
                .visibility(request.getVisibility() != null ? KnowledgeBase.KbVisibility.valueOf(request.getVisibility()) : KnowledgeBase.KbVisibility.DEPARTMENT)
                .owner(user)
                .department(request.getDepartment() != null ? request.getDepartment() : user.getDepartment())
                .icon(request.getIcon())
                .build();

        kb = kbRepository.save(kb);

        KbPermission perm = KbPermission.builder()
                .knowledgeBase(kb)
                .user(user)
                .permissionLevel(KbPermission.PermissionLevel.MANAGE)
                .build();
        kbPermissionRepository.save(perm);

        return toKbDTO(kb, user.getId());
    }

    @Transactional
    public KnowledgeBaseDTO updateKb(Long id, KbRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        KnowledgeBase kb = kbRepository.findById(id)
                .orElseThrow(() -> new BusinessException("知识库不存在", HttpStatus.NOT_FOUND));

        if (!kb.getOwner().getId().equals(user.getId()) && user.getRole() != User.UserRole.ADMIN) {
            throw new BusinessException("无权修改此知识库", HttpStatus.FORBIDDEN);
        }

        if (request.getName() != null) kb.setName(request.getName());
        if (request.getDescription() != null) kb.setDescription(request.getDescription());
        if (request.getVisibility() != null) kb.setVisibility(KnowledgeBase.KbVisibility.valueOf(request.getVisibility()));
        if (request.getDepartment() != null) kb.setDepartment(request.getDepartment());
        if (request.getIcon() != null) kb.setIcon(request.getIcon());

        kb = kbRepository.save(kb);
        return toKbDTO(kb, user.getId());
    }

    @Transactional
    public void deleteKb(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        KnowledgeBase kb = kbRepository.findById(id)
                .orElseThrow(() -> new BusinessException("知识库不存在", HttpStatus.NOT_FOUND));

        if (!kb.getOwner().getId().equals(user.getId()) && user.getRole() != User.UserRole.ADMIN) {
            throw new BusinessException("无权删除此知识库", HttpStatus.FORBIDDEN);
        }

        kb.setIsDeleted(true);
        kbRepository.save(kb);
    }

    @Transactional
    public void addMember(Long kbId, Long userId, String permissionLevel, String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException("知识库不存在", HttpStatus.NOT_FOUND));

        if (!kb.getOwner().getId().equals(currentUser.getId()) && currentUser.getRole() != User.UserRole.ADMIN) {
            throw new BusinessException("无权管理成员", HttpStatus.FORBIDDEN);
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("目标用户不存在", HttpStatus.NOT_FOUND));

        if (kbPermissionRepository.existsByKnowledgeBaseIdAndUserId(kbId, userId)) {
            throw new BusinessException("用户已是成员", HttpStatus.BAD_REQUEST);
        }

        KbPermission perm = KbPermission.builder()
                .knowledgeBase(kb)
                .user(targetUser)
                .permissionLevel(KbPermission.PermissionLevel.valueOf(permissionLevel))
                .build();
        kbPermissionRepository.save(perm);
    }

    @Transactional
    public void removeMember(Long kbId, Long userId, String username) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException("知识库不存在", HttpStatus.NOT_FOUND));

        if (!kb.getOwner().getId().equals(currentUser.getId()) && currentUser.getRole() != User.UserRole.ADMIN) {
            throw new BusinessException("无权管理成员", HttpStatus.FORBIDDEN);
        }

        kbPermissionRepository.deleteByKnowledgeBaseIdAndUserId(kbId, userId);
    }

    public List<UserDTO> getMembers(Long kbId) {
        return kbPermissionRepository.findByKnowledgeBaseId(kbId).stream()
                .map(p -> toUserDTO(p.getUser()))
                .collect(Collectors.toList());
    }

    private boolean canAccess(KnowledgeBase kb, User user) {
        if (user.getRole() == User.UserRole.ADMIN) return true;
        if (kb.getOwner().getId().equals(user.getId())) return true;
        if (kb.getVisibility() == KnowledgeBase.KbVisibility.PUBLIC) return true;
        if (kb.getVisibility() == KnowledgeBase.KbVisibility.DEPARTMENT && kb.getDepartment() != null && kb.getDepartment().equals(user.getDepartment())) return true;
        return kbPermissionRepository.existsByKnowledgeBaseIdAndUserId(kb.getId(), user.getId());
    }

    private KnowledgeBaseDTO toKbDTO(KnowledgeBase kb, Long currentUserId) {
        Long docCount = kbRepository.countDocumentsByKbId(kb.getId());
        List<UserDTO> members = kbPermissionRepository.findByKnowledgeBaseId(kb.getId()).stream()
                .map(p -> toUserDTO(p.getUser()))
                .collect(Collectors.toList());

        return KnowledgeBaseDTO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .visibility(kb.getVisibility().name())
                .owner(toUserDTO(kb.getOwner()))
                .department(kb.getDepartment())
                .icon(kb.getIcon())
                .documentCount(docCount)
                .members(members)
                .isOwner(kb.getOwner().getId().equals(currentUserId))
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
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
