package com.borrowbox.controller;

import com.borrowbox.dto.*;
import com.borrowbox.service.ChatService;
import com.borrowbox.service.DescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final ChatService chat;
    private final DescriptionService descriptions;
    public ChatController(ChatService chat, DescriptionService descriptions) { this.chat = chat; this.descriptions = descriptions; }
    @PostMapping("/chat") public ChatResponse chat(@RequestBody @Valid ChatRequest request, Authentication auth) {
        return chat.respond(auth == null || "anonymousUser".equals(auth.getName()) ? null : auth.getName(), request);
    }
    @PostMapping("/ai/generate-description") @ResponseStatus(HttpStatus.OK)
    public DescriptionResponse description(@RequestBody @Valid DescriptionRequest request) {
        return descriptions.generate(request.name(), request.condition(), request.details());
    }
}
