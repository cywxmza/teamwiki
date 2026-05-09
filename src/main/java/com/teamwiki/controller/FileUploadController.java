package com.teamwiki.controller;

import com.teamwiki.dto.UserDTO;
import com.teamwiki.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final UserService userService;

    @PostMapping("/avatar")
    public ResponseEntity<UserDTO> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                  @RequestAttribute("username") String username) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().build();
            }

            String ext = contentType.split("/")[1].toLowerCase();
            if (ext.equals("jpeg")) ext = "jpg";
            
            Set<String> allowedExtensions = Set.of("png", "jpg", "gif", "webp", "svg+xml");
            if (!allowedExtensions.contains(ext)) {
                return ResponseEntity.badRequest().build();
            }

            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().build();
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path uploadDir = Paths.get("uploads/avatars");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(filename);
            file.transferTo(filePath.toFile());

            String avatarUrl = "/uploads/avatars/" + filename;
            UserDTO updated = userService.updateAvatar(username, avatarUrl);

            return ResponseEntity.ok(updated);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file,
                                                            @RequestAttribute("username") String username) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().build();
            }

            String ext = contentType.split("/")[1].toLowerCase();
            if (ext.equals("jpeg")) ext = "jpg";

            Set<String> allowed = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp");
            if (!allowed.contains(ext)) {
                return ResponseEntity.badRequest().build();
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().build();
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path uploadDir = Paths.get("uploads/images");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(filename);
            file.transferTo(filePath.toFile());

            String imageUrl = "/uploads/images/" + filename;
            log.info("用户 {} 上传了文档图片: {}", username, imageUrl);

            return ResponseEntity.ok(Map.of("url", imageUrl));
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
