package com.finbridge.controller;

import com.finbridge.entity.ChatMessage;
import com.finbridge.entity.User;
import com.finbridge.repository.ChatMessageRepository;
import com.finbridge.repository.UserRepository;
import com.finbridge.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Consultant & client internal chatting system")
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault());

    public record ChatContactResponse(String id, String name, String role, boolean online) {}
    public record ChatMessageResponse(String from, String text, String time) {}
    public record SendMessageRequest(UUID recipientId, String text) {}

    @GetMapping("/contacts")
    public ResponseEntity<List<ChatContactResponse>> getContacts(@AuthenticationPrincipal User currentUser) {
        List<ChatContactResponse> contacts = new ArrayList<>();
        if (currentUser == null) {
            return ResponseEntity.ok(contacts);
        }

        String dept = currentUser.getDepartment();

        // 1. Department Admins
        List<User> admins = userService.getAdmins("department-admin", dept);
        for (User u : admins) {
            if (!u.getId().equals(currentUser.getId())) {
                contacts.add(new ChatContactResponse(u.getId().toString(), u.getName(), "Dept Admin", true));
            }
        }

        // 2. Operations / Consultants
        List<User> consultants = (dept != null && !dept.isBlank())
                ? userService.getConsultantsByDepartment(dept)
                : userService.getConsultants();
        for (User u : consultants) {
            if (!u.getId().equals(currentUser.getId())) {
                contacts.add(new ChatContactResponse(u.getId().toString(), u.getName(), "Internal", true));
            }
        }

        // 3. Clients
        List<User> clients = (dept != null && !dept.isBlank())
                ? userService.getClientsByDepartment(dept)
                : userService.getClients();
        for (User u : clients) {
            if (!u.getId().equals(currentUser.getId())) {
                contacts.add(new ChatContactResponse(u.getId().toString(), u.getName(), "Client", true));
            }
        }

        return ResponseEntity.ok(contacts);
    }

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@AuthenticationPrincipal User currentUser,
                                                                 @RequestParam UUID contactId) {
        if (currentUser == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<ChatMessage> history = chatMessageRepository.findChatHistory(currentUser.getId(), contactId);
        List<ChatMessageResponse> response = history.stream().map(msg -> {
            String from = msg.getSender().getId().equals(currentUser.getId()) ? "me" : "them";
            String time = msg.getCreatedAt() != null ? TIME_FORMATTER.format(msg.getCreatedAt()) : "—";
            return new ChatMessageResponse(from, msg.getText(), time);
        }).toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(@AuthenticationPrincipal User currentUser,
                                                           @RequestBody SendMessageRequest req) {
        if (currentUser == null || req.recipientId() == null || req.text() == null || req.text().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        User recipient = userRepository.findById(req.recipientId())
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        ChatMessage msg = new ChatMessage();
        msg.setSender(currentUser);
        msg.setRecipient(recipient);
        msg.setText(req.text());

        ChatMessage saved = chatMessageRepository.save(msg);
        String time = saved.getCreatedAt() != null ? TIME_FORMATTER.format(saved.getCreatedAt()) : TIME_FORMATTER.format(java.time.Instant.now());
        
        return ResponseEntity.ok(new ChatMessageResponse("me", saved.getText(), time));
    }
}
