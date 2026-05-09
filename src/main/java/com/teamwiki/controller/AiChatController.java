package com.teamwiki.controller;

import com.teamwiki.dto.AiChatRequest;
import com.teamwiki.entity.Document;
import com.teamwiki.entity.User;
import com.teamwiki.repository.DocumentRepository;
import com.teamwiki.repository.UserRepository;
import com.teamwiki.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final SystemConfigService configService;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody AiChatRequest request,
                                                      @RequestAttribute("username") String username) {
        String apiKey = configService.getConfig("deepseek.api.key");
        if (apiKey == null || apiKey.isEmpty()) {
            return ResponseEntity.ok(Map.of("reply", "AI服务未配置，请联系管理员设置DeepSeek API密钥。"));
        }

        String apiUrl = configService.getConfig("deepseek.api.url", "https://api.deepseek.com/v1/chat/completions");
        String model = configService.getConfig("deepseek.api.model", "deepseek-chat");

        try {
            User user = userRepository.findByUsername(username).orElse(null);
            String context = buildKnowledgeContext(request.getMessage(), user);

            String systemPrompt = "你是「智库平台」的AI助手，一个企业内部知识库协同平台的智能助手。\n" +
                    "你的职责是：\n" +
                    "1. 帮助用户快速查找知识库中的文档和知识\n" +
                    "2. 基于知识库内容回答用户问题\n" +
                    "3. 推荐相关的文档和知识\n" +
                    "4. 协助用户整理和归纳知识\n\n" +
                    "以下是知识库中相关的文档内容，请基于这些内容回答用户问题：\n" +
                    context;

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            if (request.getHistory() != null) {
                for (Map<String, String> h : request.getHistory()) {
                    messages.add(h);
                }
            }

            messages.add(Map.of("role", "user", "content", request.getMessage()));

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("max_tokens", 2000);
            body.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String reply = (String) message.get("content");
                    return ResponseEntity.ok(Map.of("reply", reply));
                }
            }

            return ResponseEntity.ok(Map.of("reply", "AI服务暂时无法响应，请稍后再试。"));
        } catch (Exception e) {
            log.error("AI chat error", e);
            return ResponseEntity.ok(Map.of("reply", "AI服务调用失败，请稍后重试。"));
        }
    }

    private String buildKnowledgeContext(String query, User user) {
        List<Document> docs;
        try {
            if (user != null) {
                docs = documentRepository.findByIsDeletedFalse(PageRequest.of(0, 20)).getContent();
                docs = docs.stream()
                        .filter(d -> {
                            if (d.getVisibility() == Document.DocVisibility.PUBLIC) return true;
                            if (d.getCreatedBy() != null && d.getCreatedBy().getId().equals(user.getId())) return true;
                            if (d.getVisibility() == Document.DocVisibility.TEAM && user.getDepartment() != null) return true;
                            return false;
                        })
                        .limit(10)
                        .collect(Collectors.toList());
            } else {
                docs = documentRepository.findByIsDeletedFalse(PageRequest.of(0, 5)).getContent();
            }
        } catch (Exception e) {
            return "（暂无知识库内容）";
        }

        if (docs.isEmpty()) return "（知识库暂无文档）";

        StringBuilder sb = new StringBuilder();
        for (Document doc : docs) {
            sb.append("--- 文档：").append(doc.getTitle()).append(" ---\n");
            String content = doc.getContent();
            if (content != null) {
                content = content.replaceAll("<[^>]+>", "");
                if (content.length() > 500) content = content.substring(0, 500) + "...";
                sb.append(content);
            }
            sb.append("\n\n");
        }
        return sb.toString();
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getAiConfig(@RequestAttribute("username") String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getRole() != User.UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String apiKey = configService.getConfig("deepseek.api.key");
        String apiUrl = configService.getConfig("deepseek.api.url", "https://api.deepseek.com/v1/chat/completions");
        String model = configService.getConfig("deepseek.api.model", "deepseek-chat");
        String maskedKey = apiKey != null && apiKey.length() > 8
                ? apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4)
                : "";
        return ResponseEntity.ok(Map.of("apiKey", maskedKey, "apiUrl", apiUrl, "model", model, "configured", apiKey != null && !apiKey.isEmpty() ? "true" : "false"));
    }

    @PostMapping("/config")
    public ResponseEntity<Void> saveAiConfig(@RequestBody Map<String, String> config,
                                              @RequestAttribute("username") String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getRole() != User.UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (config.containsKey("apiKey") && !config.get("apiKey").contains("****")) {
            configService.setConfig("deepseek.api.key", config.get("apiKey"), "DeepSeek API Key");
        }
        if (config.containsKey("apiUrl")) {
            configService.setConfig("deepseek.api.url", config.get("apiUrl"), "DeepSeek API URL");
        }
        if (config.containsKey("model")) {
            configService.setConfig("deepseek.api.model", config.get("model"), "DeepSeek Model");
        }
        return ResponseEntity.ok().build();
    }
}
