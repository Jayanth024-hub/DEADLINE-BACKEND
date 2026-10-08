package com.controller;

import org.springframework.web.bind.annotation.*;
import com.dto.ChatRequest;
import com.dto.ChatResponse;
import com.dto.MessageDto;
import com.entity.Conversation;
import com.service.ChatService;
import com.deadlineiq.model.User;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    private User resolveCurrentUser(HttpServletRequest request, HttpSession session, Long fallbackUserId) {
        // 1. Session attribute
        if (session != null) {
            User sessionUser = (User) session.getAttribute("currentUser");
            if (sessionUser != null) {
                return sessionUser;
            }
        }

        // 2. Authorization Bearer token header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer-")) {
            Optional<User> uOpt = userService.getUserByToken(authHeader.trim());
            if (uOpt.isPresent()) {
                return uOpt.get();
            }
        }

        // 3. X-User-Id header
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null) {
            try {
                Long uid = Long.parseLong(userIdHeader.trim());
                Optional<User> uOpt = userService.findById(uid);
                if (uOpt.isPresent()) {
                    return uOpt.get();
                }
            } catch (Exception ignored) {}
        }

        // 4. Fallback explicit userId param
        if (fallbackUserId != null) {
            Optional<User> uOpt = userService.findById(fallbackUserId);
            if (uOpt.isPresent()) {
                return uOpt.get();
            }
        }

        return null;
    }

    @PostMapping
    public ResponseEntity<?> chat(
            @RequestBody ChatRequest request,
            HttpServletRequest httpRequest,
            HttpSession session) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty."));
        }

        User user = resolveCurrentUser(httpRequest, session, request.getUserId());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required. Please log in to chat with AI."));
        }

        String response = chatService.generateResponse(
                request.getMessage(),
                request.getConversationId(),
                user.getId(),
                user.getEmail()
        );
        return ResponseEntity.ok(new ChatResponse(response));
    }

    @GetMapping
    public ResponseEntity<?> chatGet(
            @RequestParam(value = "message", defaultValue = "Hello! Can you help me organize my deadlines?") String message,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            @RequestParam(value = "userId", required = false) Long userId,
            HttpServletRequest httpRequest,
            HttpSession session) {
        User user = resolveCurrentUser(httpRequest, session, userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required. Please log in to chat with AI."));
        }

        String response = chatService.generateResponse(message, conversationId, user.getId(), user.getEmail());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getUserConversations(
            @RequestParam(value = "userId", required = false) Long userId,
            HttpServletRequest httpRequest,
            HttpSession session) {
        User user = resolveCurrentUser(httpRequest, session, userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required."));
        }

        List<Conversation> list = chatService.getUserConversations(user.getId());
        List<Map<String, Object>> summary = list.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("conversationId", c.getConversationId());
            map.put("createdAt", c.getCreatedAt());
            map.put("updatedAt", c.getUpdatedAt());
            map.put("messageCount", c.getMessages() != null ? c.getMessages().size() : 0);
            return map;
        }).toList();

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/history/{conversationId}")
    public ResponseEntity<?> getHistory(
            @PathVariable String conversationId,
            @RequestParam(value = "userId", required = false) Long userId,
            HttpServletRequest httpRequest,
            HttpSession session) {
        User user = resolveCurrentUser(httpRequest, session, userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required."));
        }

        List<MessageDto> history = chatService.getConversationMessages(conversationId, user.getId()).stream()
                .map(m -> new MessageDto(m.getRole().name(), m.getContent(), m.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<?> clearConversation(
            @PathVariable String conversationId,
            @RequestParam(value = "userId", required = false) Long userId,
            HttpServletRequest httpRequest,
            HttpSession session) {
        User user = resolveCurrentUser(httpRequest, session, userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication required."));
        }

        chatService.clearConversation(conversationId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
