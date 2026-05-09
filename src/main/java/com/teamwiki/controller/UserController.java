package com.teamwiki.controller;

import com.teamwiki.dto.*;
import com.teamwiki.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }

    @PutMapping("/{id}/admin")
    public ResponseEntity<UserDTO> updateUserByAdmin(@PathVariable Long id,
                                                      @RequestBody UserDTO dto,
                                                      @RequestAttribute("username") String adminUsername) {
        return ResponseEntity.ok(userService.updateUserByAdmin(id, dto, adminUsername));
    }

    @PatchMapping("/{id}/toggle-enabled")
    public ResponseEntity<Void> toggleUserEnabled(@PathVariable Long id) {
        userService.toggleUserEnabled(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }
}
