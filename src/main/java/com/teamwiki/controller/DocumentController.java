package com.teamwiki.controller;

import com.teamwiki.dto.*;
import com.teamwiki.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<PageResponse<DocumentDTO>> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Long kbId,
            @RequestParam(required = false) String sort,
            @RequestAttribute("username") String username) {
        return ResponseEntity.ok(documentService.getDocuments(page, size, keyword, categoryId, tagId, kbId, sort, username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(@PathVariable Long id,
                                                    @RequestAttribute("username") String username) {
        return ResponseEntity.ok(documentService.getDocument(id, username));
    }

    @PostMapping
    public ResponseEntity<DocumentDTO> createDocument(@RequestBody DocumentRequest request,
                                                       @RequestAttribute("username") String username) {
        return ResponseEntity.ok(documentService.createDocument(request, username));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentDTO> updateDocument(@PathVariable Long id,
                                                       @RequestBody DocumentRequest request,
                                                       @RequestAttribute("username") String username) {
        return ResponseEntity.ok(documentService.updateDocument(id, request, username));
    }

    @PatchMapping("/{id}/auto-save")
    public ResponseEntity<Void> autoSaveDraft(@PathVariable Long id,
                                               @RequestBody Map<String, String> body,
                                               @RequestAttribute("username") String username) {
        documentService.autoSaveDraft(id, body.get("content"), username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<DocumentVersionDTO> publishDocument(@PathVariable Long id,
                                                               @RequestBody Map<String, String> body,
                                                               @RequestAttribute("username") String username) {
        return ResponseEntity.ok(documentService.publishDocument(id, body.get("changeDescription"), username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id,
                                                @RequestAttribute("username") String username) {
        documentService.deleteDocument(id, username);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreDocument(@PathVariable Long id,
                                                 @RequestAttribute("username") String username) {
        documentService.restoreDocument(id, username);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentDeleteDocument(@PathVariable Long id) {
        documentService.permanentDeleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    public ResponseEntity<PageResponse<DocumentDTO>> getMyDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created") String tab,
            @RequestAttribute("username") String username) {
        return ResponseEntity.ok(documentService.getMyDocuments(username, page, size, tab));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<DocumentDTO>> getRecentDocuments(
            @RequestAttribute("username") String username) {
        return ResponseEntity.ok(documentService.getRecentDocuments(username));
    }

    @GetMapping("/hot")
    public ResponseEntity<List<DocumentDTO>> getHotDocuments() {
        return ResponseEntity.ok(documentService.getHotDocuments());
    }

    @GetMapping("/deleted")
    public ResponseEntity<PageResponse<DocumentDTO>> getDeletedDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentService.getDeletedDocuments(page, size));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Void> toggleFavorite(@PathVariable Long id,
                                                @RequestAttribute("username") String username) {
        documentService.toggleFavorite(id, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/favorites")
    public ResponseEntity<List<DocumentDTO>> getFavorites(
            @RequestAttribute("username") String username) {
        return ResponseEntity.ok(documentService.getFavorites(username));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<PageResponse<DocumentVersionDTO>> getDocumentVersions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(documentService.getDocumentVersions(id, page, size));
    }

    @GetMapping("/{documentId}/versions/{versionId}")
    public ResponseEntity<DocumentVersionDTO> getVersionDetail(
            @PathVariable Long documentId, @PathVariable Long versionId) {
        return ResponseEntity.ok(documentService.getVersionDetail(documentId, versionId));
    }

    @PostMapping("/{documentId}/versions/{versionId}/rollback")
    public ResponseEntity<Void> rollbackToVersion(
            @PathVariable Long documentId, @PathVariable Long versionId,
            @RequestAttribute("username") String username) {
        documentService.rollbackToVersion(documentId, versionId, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/collaborators")
    public ResponseEntity<List<UserDTO>> getCollaborators(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getCollaborators(id));
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Map<String, String>> enableShare(@PathVariable Long id,
                                                            @RequestAttribute("username") String username) {
        String shareToken = documentService.enableShare(id, username);
        return ResponseEntity.ok(Map.of("shareToken", shareToken));
    }

    @DeleteMapping("/{id}/share")
    public ResponseEntity<Void> disableShare(@PathVariable Long id,
                                              @RequestAttribute("username") String username) {
        documentService.disableShare(id, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/shared/{shareToken}")
    public ResponseEntity<DocumentDTO> getSharedDocument(@PathVariable String shareToken) {
        return ResponseEntity.ok(documentService.getSharedDocument(shareToken));
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<Map<String, Integer>> batchDelete(@RequestBody List<Long> ids,
                                                             @RequestAttribute("username") String username) {
        int count = documentService.batchDelete(ids, username);
        return ResponseEntity.ok(Map.of("deleted", count));
    }

    @PostMapping("/batch-restore")
    public ResponseEntity<Map<String, Integer>> batchRestore(@RequestBody List<Long> ids,
                                                              @RequestAttribute("username") String username) {
        int count = documentService.batchRestore(ids, username);
        return ResponseEntity.ok(Map.of("restored", count));
    }

    @PostMapping("/batch-tag")
    public ResponseEntity<Void> batchTag(@RequestBody Map<String, Object> body,
                                         @RequestAttribute("username") String username) {
        @SuppressWarnings("unchecked")
        List<Long> docIds = ((List<Integer>) body.get("docIds")).stream()
                .map(Long::valueOf).collect(Collectors.toList());
        Long tagId = Long.valueOf(body.get("tagId").toString());
        documentService.batchTagDocuments(docIds, tagId, username);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(documentService.getStatistics());
    }
}
