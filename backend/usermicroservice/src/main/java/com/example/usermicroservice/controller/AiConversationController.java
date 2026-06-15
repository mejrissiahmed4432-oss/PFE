package com.example.usermicroservice.controller;

import com.example.usermicroservice.model.AiConversation;
import com.example.usermicroservice.model.User;
import com.example.usermicroservice.repository.AiConversationRepository;
import com.example.usermicroservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/aiconversations")
@CrossOrigin(origins = "*")
public class AiConversationController {

    @Autowired
    private AiConversationRepository aiConversationRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        String email = principal instanceof String ? (String) principal : principal.toString();
        return userRepository.findByEmail(email).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> getAllConversations() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        List<AiConversation> conversations = aiConversationRepository.findByUserIdOrderByUpdatedAtDesc(currentUser.getId());
        return ResponseEntity.ok(conversations);
    }

    @PostMapping
    public ResponseEntity<?> saveConversation(@RequestBody AiConversation conversation) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        conversation.setUserId(currentUser.getId());
        conversation.setUpdatedAt(new Date());

        if (conversation.getCreatedAt() == null) {
            conversation.setCreatedAt(new Date());
        }

        AiConversation saved = aiConversationRepository.save(conversation);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable String id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Optional<AiConversation> convOpt = aiConversationRepository.findById(id);
        if (convOpt.isPresent()) {
            AiConversation conv = convOpt.get();
            // Ensure the user owns this conversation
            if (!conv.getUserId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body("Forbidden");
            }
            aiConversationRepository.delete(conv);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
