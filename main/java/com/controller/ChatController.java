package com.controller;

import org.springframework.web.bind.annotation.*;
import com.dto.ChatRequest;
import com.dto.ChatResponse;
import com.dto.MessageDto;
import com.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "*"})
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message cannot be empty.");
        }
        String response = chatService.generateResponse(
                request.getMessage(),
                request.getConversationId()
        );
        return new ChatResponse(response);
    }

    @GetMapping
    public String chatGet(
            @RequestParam(value = "message", defaultValue = "Hello! Can you help me organize my deadlines?") String message,
            @RequestParam(value = "conversationId", required = false) String conversationId) {
        return chatService.generateResponse(message, conversationId);
    }

    @GetMapping("/history/{conversationId}")
    public List<MessageDto> getHistory(@PathVariable String conversationId) {
        return chatService.getConversationMessages(conversationId).stream()
                .map(m -> new MessageDto(m.getRole().name(), m.getContent(), m.getCreatedAt()))
                .toList();
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> clearConversation(@PathVariable String conversationId) {
        chatService.clearConversation(conversationId);
        return ResponseEntity.noContent().build();
    }
}
