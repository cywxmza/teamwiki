package com.teamwiki.controller;

import com.teamwiki.dto.KbRequest;
import com.teamwiki.dto.KnowledgeBaseDTO;
import com.teamwiki.dto.UserDTO;
import com.teamwiki.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    @GetMapping
    public ResponseEntity<List<KnowledgeBaseDTO>> getAccessibleKbs(
            @RequestAttribute("username") String username) {
        return ResponseEntity.ok(kbService.getAccessibleKbs(username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBaseDTO> getKbById(@PathVariable Long id,
                                                        @RequestAttribute("username") String username) {
        return ResponseEntity.ok(kbService.getKbById(id, username));
    }

    @PostMapping
    public ResponseEntity<KnowledgeBaseDTO> createKb(@RequestBody KbRequest request,
                                                       @RequestAttribute("username") String username) {
        return ResponseEntity.ok(kbService.createKb(request, username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBaseDTO> updateKb(@PathVariable Long id,
                                                       @RequestBody KbRequest request,
                                                       @RequestAttribute("username") String username) {
        return ResponseEntity.ok(kbService.updateKb(id, request, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKb(@PathVariable Long id,
                                          @RequestAttribute("username") String username) {
        kbService.deleteKb(id, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<UserDTO>> getMembers(@PathVariable Long id) {
        return ResponseEntity.ok(kbService.getMembers(id));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           @RequestAttribute("username") String username) {
        Long userId = Long.valueOf(body.get("userId").toString());
        String permissionLevel = body.get("permissionLevel").toString();
        kbService.addMember(id, userId, permissionLevel, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id,
                                              @PathVariable Long userId,
                                              @RequestAttribute("username") String username) {
        kbService.removeMember(id, userId, username);
        return ResponseEntity.ok().build();
    }
}
