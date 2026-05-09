package com.teamwiki.service;

import com.teamwiki.entity.AuditLog;
import com.teamwiki.entity.Document;
import com.teamwiki.entity.User;
import com.teamwiki.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest httpServletRequest;

    @Async
    public void log(User user, String action, String entityType, Long entityId, String description, String details) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .user(user)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .description(description)
                    .details(details)
                    .ipAddress(httpServletRequest.getRemoteAddr())
                    .build();
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("审计日志写入失败", e);
        }
    }

    public void logDocumentAccess(User user, Document doc, String action) {
        log(user, action, "DOCUMENT", doc.getId(),
                user.getNickname() + " " + getActionName(action) + " 文档《" + doc.getTitle() + "》", null);
    }

    public Map<String, Object> getDailyStats() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Long activeUsers = auditLogRepository.countActiveUsersSince(since);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeUsers7d", activeUsers != null ? activeUsers : 0L);
        stats.put("period", "7d");
        return stats;
    }

    public List<Map<String, Object>> getRecentLogs(int count) {
        return auditLogRepository.findLatestLogs(org.springframework.data.domain.PageRequest.of(0, count))
                .getContent().stream()
                .map(log -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", log.getId());
                    entry.put("user", log.getUser().getNickname());
                    entry.put("action", log.getAction());
                    entry.put("entityType", log.getEntityType());
                    entry.put("description", log.getDescription());
                    entry.put("createdAt", log.getCreatedAt());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    private String getActionName(String action) {
        return switch (action) {
            case "CREATE" -> "创建";
            case "EDIT" -> "编辑";
            case "DELETE" -> "删除";
            case "VIEW" -> "查看";
            case "RESTORE" -> "恢复";
            case "SHARE" -> "分享";
            case "EXPORT" -> "导出";
            case "BATCH_DELETE" -> "批量删除";
            default -> action;
        };
    }
}
