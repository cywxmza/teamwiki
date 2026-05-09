package com.teamwiki.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Map<String, Set<WebSocketSession>> documentRooms = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionDocumentMap = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsernameMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> documentLocks = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String type = jsonNode.get("type").asText();

            switch (type) {
                case "join" -> handleJoin(session, jsonNode);
                case "leave" -> handleLeave(session);
                case "edit" -> handleEdit(session, jsonNode);
                case "lock" -> handleLock(session, jsonNode);
                case "unlock" -> handleUnlock(session, jsonNode);
                case "cursor" -> handleCursor(session, jsonNode);
                case "save" -> handleSave(session, jsonNode);
                default -> log.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        handleLeave(session);
        log.info("WebSocket连接关闭: {}", session.getId());
    }

    private void handleJoin(WebSocketSession session, JsonNode jsonNode) throws IOException {
        Long documentId = jsonNode.get("documentId").asLong();
        Long userId = jsonNode.has("userId") ? jsonNode.get("userId").asLong() : 0L;
        String username = (String) session.getAttributes().get("username");
        if (username == null || username.isEmpty()) {
            username = jsonNode.has("username") ? jsonNode.get("username").asText() : "用户" + userId;
        }

        String roomKey = "doc:" + documentId;
        sessionDocumentMap.put(session.getId(), documentId);
        sessionUserMap.put(session.getId(), userId);
        sessionUsernameMap.put(session.getId(), username);

        documentRooms.computeIfAbsent(roomKey, k -> ConcurrentHashMap.newKeySet()).add(session);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "user_joined");
        response.put("userId", userId);
        response.put("username", username);
        broadcastToRoom(roomKey, response.toString(), null);

        sendOnlineUsers(roomKey, session);
    }

    private void handleLeave(WebSocketSession session) throws IOException {
        Long documentId = sessionDocumentMap.remove(session.getId());
        Long userId = sessionUserMap.remove(session.getId());
        String username = sessionUsernameMap.remove(session.getId());

        if (documentId != null) {
            String roomKey = "doc:" + documentId;
            Set<WebSocketSession> room = documentRooms.get(roomKey);
            if (room != null) {
                room.remove(session);
                if (room.isEmpty()) {
                    documentRooms.remove(roomKey);
                    documentLocks.remove(roomKey);
                } else {
                    ObjectNode response = objectMapper.createObjectNode();
                    response.put("type", "user_left");
                    response.put("userId", userId);
                    response.put("username", username);
                    broadcastToRoom(roomKey, response.toString(), null);

                    releaseUserLocks(roomKey, userId);
                }
            }
        }
    }

    private void handleEdit(WebSocketSession session, JsonNode jsonNode) throws IOException {
        Long documentId = sessionDocumentMap.get(session.getId());
        if (documentId == null) return;

        String roomKey = "doc:" + documentId;
        ObjectNode response = (ObjectNode) jsonNode;
        response.put("type", "edit");
        response.put("userId", sessionUserMap.get(session.getId()));
        broadcastToRoom(roomKey, response.toString(), session);
    }

    private void handleLock(WebSocketSession session, JsonNode jsonNode) throws IOException {
        Long documentId = sessionDocumentMap.get(session.getId());
        if (documentId == null) return;

        String roomKey = "doc:" + documentId;
        String paragraphId = jsonNode.get("paragraphId").asText();
        Long userId = sessionUserMap.get(session.getId());
        String username = sessionUsernameMap.get(session.getId());

        Map<String, String> locks = documentLocks.computeIfAbsent(roomKey, k -> new ConcurrentHashMap<>());

        if (!locks.containsKey(paragraphId)) {
            locks.put(paragraphId, userId + ":" + username);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "paragraph_locked");
            response.put("paragraphId", paragraphId);
            response.put("userId", userId);
            response.put("username", username);
            broadcastToRoom(roomKey, response.toString(), null);
        } else {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "lock_denied");
            response.put("paragraphId", paragraphId);
            session.sendMessage(new TextMessage(response.toString()));
        }
    }

    private void handleUnlock(WebSocketSession session, JsonNode jsonNode) throws IOException {
        Long documentId = sessionDocumentMap.get(session.getId());
        if (documentId == null) return;

        String roomKey = "doc:" + documentId;
        String paragraphId = jsonNode.get("paragraphId").asText();
        Long userId = sessionUserMap.get(session.getId());

        Map<String, String> locks = documentLocks.get(roomKey);
        if (locks != null && locks.containsKey(paragraphId)) {
            String lockOwner = locks.get(paragraphId);
            if (lockOwner.startsWith(userId + ":")) {
                locks.remove(paragraphId);

                ObjectNode response = objectMapper.createObjectNode();
                response.put("type", "paragraph_unlocked");
                response.put("paragraphId", paragraphId);
                response.put("userId", userId);
                broadcastToRoom(roomKey, response.toString(), null);
            }
        }
    }

    private void handleCursor(WebSocketSession session, JsonNode jsonNode) throws IOException {
        Long documentId = sessionDocumentMap.get(session.getId());
        if (documentId == null) return;

        String roomKey = "doc:" + documentId;
        ObjectNode response = (ObjectNode) jsonNode;
        response.put("type", "cursor");
        response.put("userId", sessionUserMap.get(session.getId()));
        response.put("username", sessionUsernameMap.get(session.getId()));
        broadcastToRoom(roomKey, response.toString(), session);
    }

    private void handleSave(WebSocketSession session, JsonNode jsonNode) throws IOException {
        Long documentId = sessionDocumentMap.get(session.getId());
        if (documentId == null) return;

        String roomKey = "doc:" + documentId;
        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "document_saved");
        response.put("userId", sessionUserMap.get(session.getId()));
        response.put("username", sessionUsernameMap.get(session.getId()));
        broadcastToRoom(roomKey, response.toString(), null);
    }

    private void releaseUserLocks(String roomKey, Long userId) throws IOException {
        Map<String, String> locks = documentLocks.get(roomKey);
        if (locks == null) return;

        List<String> toRemove = new ArrayList<>();
        locks.forEach((paragraphId, lockInfo) -> {
            if (lockInfo.startsWith(userId + ":")) {
                toRemove.add(paragraphId);
            }
        });

        for (String paragraphId : toRemove) {
            locks.remove(paragraphId);
            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "paragraph_unlocked");
            response.put("paragraphId", paragraphId);
            response.put("userId", userId);
            broadcastToRoom(roomKey, response.toString(), null);
        }
    }

    private void sendOnlineUsers(String roomKey, WebSocketSession session) throws IOException {
        Set<WebSocketSession> room = documentRooms.get(roomKey);
        if (room == null) return;

        List<Map<String, Object>> users = new ArrayList<>();
        for (WebSocketSession s : room) {
            Long userId = sessionUserMap.get(s.getId());
            String username = sessionUsernameMap.get(s.getId());
            if (userId != null) {
                Map<String, Object> user = new HashMap<>();
                user.put("userId", userId);
                user.put("username", username);
                users.add(user);
            }
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("type", "online_users");
        response.putPOJO("users", users);
        session.sendMessage(new TextMessage(response.toString()));
    }

    private void broadcastToRoom(String roomKey, String message, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> room = documentRooms.get(roomKey);
        if (room == null) return;

        TextMessage textMessage = new TextMessage(message);
        for (WebSocketSession s : room) {
            if (s.isOpen() && (excludeSession == null || !s.getId().equals(excludeSession.getId()))) {
                try {
                    s.sendMessage(textMessage);
                } catch (IOException e) {
                    log.error("发送消息失败", e);
                }
            }
        }
    }
}
