package com.teamwiki.controller;

import com.teamwiki.dto.*;
import com.teamwiki.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<PermissionDTO>> getDocumentPermissions(@PathVariable Long documentId) {
        return ResponseEntity.ok(permissionService.getDocumentPermissions(documentId));
    }

    @PostMapping
    public ResponseEntity<PermissionDTO> addPermission(@RequestBody PermissionRequest request) {
        return ResponseEntity.ok(permissionService.addPermission(request));
    }

    @DeleteMapping("/document/{documentId}/user/{userId}")
    public ResponseEntity<Void> removePermission(@PathVariable Long documentId, @PathVariable Long userId) {
        permissionService.removePermission(documentId, userId);
        return ResponseEntity.ok().build();
    }
}
