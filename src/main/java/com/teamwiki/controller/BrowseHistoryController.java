package com.teamwiki.controller;

import com.teamwiki.dto.*;
import com.teamwiki.service.BrowseHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/browse-history")
@RequiredArgsConstructor
public class BrowseHistoryController {

    private final BrowseHistoryService browseHistoryService;

    @GetMapping
    public ResponseEntity<PageResponse<DocumentDTO>> getBrowseHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestAttribute("username") String username) {
        return ResponseEntity.ok(browseHistoryService.getBrowseHistory(username, page, size));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearBrowseHistory(@RequestAttribute("username") String username) {
        browseHistoryService.clearBrowseHistory(username);
        return ResponseEntity.ok().build();
    }
}
