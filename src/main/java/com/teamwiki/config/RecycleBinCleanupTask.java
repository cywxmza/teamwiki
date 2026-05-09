package com.teamwiki.config;

import com.teamwiki.entity.Document;
import com.teamwiki.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecycleBinCleanupTask {

    private final DocumentRepository documentRepository;

    @Value("${app.recycle-bin.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredDocuments() {
        log.info("开始清理过期回收站文档...");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        
        int page = 0;
        int batchSize = 100;
        int totalDeleted = 0;

        while (true) {
            Page<Document> docs = documentRepository.findByIsDeletedTrue(PageRequest.of(page, batchSize));
            if (docs.isEmpty()) break;

            int deletedInBatch = 0;
            for (Document doc : docs.getContent()) {
                if (doc.getDeletedAt() != null && doc.getDeletedAt().isBefore(cutoff)) {
                    documentRepository.delete(doc);
                    deletedInBatch++;
                }
            }
            
            totalDeleted += deletedInBatch;
            page++;
            
            if (docs.isLast()) break;
        }

        log.info("回收站清理完成，共永久删除 {} 个过期文档", totalDeleted);
    }
}
