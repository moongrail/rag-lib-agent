package com.ragassistant.adapter.rest;

import com.ragassistant.api.dto.ChatRequest;
import com.ragassistant.api.dto.ChatResponse;
import com.ragassistant.chat.ChatService;
import com.ragassistant.common.TenantContext;
import com.ragassistant.common.TenantIds;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String tenant = TenantIds.sanitize(TenantContext.get());
        ChatResponse response = chatService.ask(tenant, request.sessionId(), request.message());
        return ResponseEntity.ok(response);
    }
}
