package com.teamwiki.service;

import com.teamwiki.dto.UserCreateRequest;
import com.teamwiki.dto.UserDTO;
import com.teamwiki.entity.User;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        return toUserDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());

        user = userRepository.save(user);
        return toUserDTO(user);
    }

    @Transactional
    public UserDTO updateUserByAdmin(Long id, UserDTO dto, String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new BusinessException("管理员不存在", HttpStatus.NOT_FOUND));
        if (admin.getRole() != User.UserRole.ADMIN) {
            throw new BusinessException("只有管理员可以执行此操作", HttpStatus.FORBIDDEN);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getDepartment() != null) user.setDepartment(dto.getDepartment());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getRole() != null) user.setRole(User.UserRole.valueOf(dto.getRole()));
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());

        user = userRepository.save(user);
        return toUserDTO(user);
    }

    @Transactional
    public void toggleUserEnabled(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }

    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BusinessException("用户名已存在", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .email(request.getEmail())
                .department(request.getDepartment())
                .role(request.getRole() != null ? User.UserRole.valueOf(request.getRole()) : User.UserRole.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        return toUserDTO(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        userRepository.delete(user);
    }

    @Transactional
    public UserDTO updateAvatar(String username, String avatarUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("用户不存在", HttpStatus.NOT_FOUND));
        user.setAvatar(avatarUrl);
        user = userRepository.save(user);
        return toUserDTO(user);
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
