package com.gomarket.controller;

import com.gomarket.model.ChatMessage;
import com.gomarket.model.ShoppingRequest;
import com.gomarket.repository.ChatMessageRepository;
import com.gomarket.repository.ShoppingRequestRepository;
import com.gomarket.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatMessageRepository chatRepository;
    private final UserRepository userRepository;
    private final ShoppingRequestRepository requestRepository;

    public ChatController(ChatMessageRepository chatRepository,
                          UserRepository userRepository,
                          ShoppingRequestRepository requestRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
    }

    /** POST /api/chat/send — Gửi tin nhắn */
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, Object> body) {
        try {
            ChatMessage msg = new ChatMessage();
            msg.setRequestId(((Number) body.get("requestId")).longValue());
            msg.setSenderId(((Number) body.get("senderId")).longValue());
            msg.setReceiverId(((Number) body.get("receiverId")).longValue());
            msg.setMessage((String) body.get("message"));

            ChatMessage saved = chatRepository.save(msg);
            enrichSenderName(saved);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/chat/{requestId}/messages — Lấy tin nhắn theo đơn */
    @GetMapping("/{requestId}/messages")
    public ResponseEntity<List<ChatMessage>> getMessages(@PathVariable Long requestId,
                                                          @RequestParam(required = false) Long afterId) {
        List<ChatMessage> messages;
        if (afterId != null && afterId > 0) {
            messages = chatRepository.findNewMessages(requestId, afterId);
        } else {
            messages = chatRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
        }
        messages.forEach(this::enrichSenderName);
        return ResponseEntity.ok(messages);
    }

    /** GET /api/chat/conversations/{userId} — Danh sách hội thoại */
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getConversations(@PathVariable Long userId) {
        List<Long> requestIds = chatRepository.findConversationRequestIds(userId);
        List<Map<String, Object>> conversations = new ArrayList<>();

        for (Long reqId : requestIds) {
            Map<String, Object> conv = new LinkedHashMap<>();
            conv.put("requestId", reqId);

            // Get request info
            requestRepository.findById(reqId).ifPresent(req -> {
                conv.put("status", req.getStatus());

                // Determine the other user
                long otherUserId = req.getUserId().equals(userId) ?
                        (req.getShopperId() != null ? req.getShopperId() : 0L) :
                        req.getUserId();
                conv.put("otherUserId", otherUserId);

                userRepository.findById(otherUserId).ifPresent(user -> {
                    conv.put("otherUserName", user.getFullName());
                    conv.put("otherUserAvatar", user.getAvatarUrl());
                });
            });

            // Get latest message
            ChatMessage latest = chatRepository.findLatestByRequestId(reqId);
            if (latest != null) {
                conv.put("lastMessage", latest.getMessage());
                conv.put("lastMessageTime", latest.getCreatedAt().toString());
                conv.put("lastSenderId", latest.getSenderId());
            }

            conversations.add(conv);
        }

        return ResponseEntity.ok(conversations);
    }

    private void enrichSenderName(ChatMessage msg) {
        userRepository.findById(msg.getSenderId()).ifPresent(user ->
                msg.setSenderName(user.getFullName()));
    }
}
