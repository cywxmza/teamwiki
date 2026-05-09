package com.teamwiki.controller;

import com.teamwiki.service.ActivityService;
import com.teamwiki.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DocumentService documentService;
    private final ActivityService activityService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestAttribute("username") String username) {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("recentDocs", documentService.getRecentDocuments(username));
        dashboard.put("hotDocs", documentService.getHotDocuments());
        dashboard.put("activities", activityService.getActivities(0, 10, null).getContent());
        return ResponseEntity.ok(dashboard);
    }
}
