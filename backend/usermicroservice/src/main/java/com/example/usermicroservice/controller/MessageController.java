package com.example.usermicroservice.controller;

import com.example.usermicroservice.model.Message;
import com.example.usermicroservice.model.User;
import com.example.usermicroservice.repository.MessageRepository;
import com.example.usermicroservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        Object principal = auth.getPrincipal();
        String email = principal instanceof String ? (String) principal : principal.toString();
        return userRepository.findByEmail(email).orElse(null);
    }

    @GetMapping("/history/{otherUserId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String otherUserId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        String currentUserId = currentUser.getId();

        // Fetch messages where currentUser is sender or receiver
        List<Message> sent = messageRepository.findBySenderIdAndReceiverIdOrderByTimestampAsc(currentUserId, otherUserId);
        List<Message> received = messageRepository.findBySenderIdAndReceiverIdOrderByTimestampAsc(otherUserId, currentUserId);
        
        List<Message> history = new ArrayList<>(sent);
        history.addAll(received);
        
        // Mask content for deleted messages instead of removing them
        history.forEach(m -> {
            boolean isSender = m.getSenderId().equals(currentUserId);
            boolean isDeleted = m.isDeletedForEveryone() || (isSender ? m.isDeletedForSender() : m.isDeletedForReceiver());
            if (isDeleted) {
                m.setContent(null);
                m.setAttachmentId(null);
                m.setFileName(null);
                m.setFileType(null);
            }
        });

        history.sort(Comparator.comparing(Message::getTimestamp));

        return ResponseEntity.ok(history);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        return ResponseEntity.ok(Map.of("count", countUnreadFor(currentUser.getId())));
    }

    private long countUnreadFor(String userId) {
        return messageRepository.findByReceiverIdAndStatusOrderByTimestampAsc(userId, "SENT").size();
    }

    private void pushUnreadCount(String userId) {
        long count = countUnreadFor(userId);
        messagingTemplate.convertAndSend("/topic/unread-count/" + userId, Map.of("count", count));
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Message message) {
        User currentUser = getCurrentUser();
        
        // If no user in security context, we check if it's an internal system call (senderId must be provided)
        if (currentUser == null) {
            if (message.getSenderId() == null || message.getSenderId().isBlank()) {
                return ResponseEntity.status(401).body("Unauthorized: No sender context found.");
            }
            // Trust the senderId provided in the payload for internal/system calls
        } else {
            // Override with authenticated user for standard web requests
            message.setSenderId(currentUser.getId());
        }

        message.setTimestamp(LocalDateTime.now());
        if (message.getStatus() == null) {
            message.setStatus("SENT");
        }
        
        Message saved = messageRepository.save(message);
        
        // Notify the receiver in real-time
        messagingTemplate.convertAndSend("/topic/messages/" + saved.getReceiverId(), saved);
        
        // Update receiver's unread count badge
        pushUnreadCount(saved.getReceiverId());
        
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/read/{senderId}")
    public ResponseEntity<?> markAsRead(@PathVariable String senderId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        List<Message> unread = messageRepository.findByReceiverIdAndStatusOrderByTimestampAsc(currentUser.getId(), "SENT")
            .stream()
            .filter(m -> m.getSenderId().equals(senderId))
            .collect(Collectors.toList());

        unread.forEach(m -> m.setStatus("READ"));
        messageRepository.saveAll(unread);

        // Update current user's unread count (it decreased)
        pushUnreadCount(currentUser.getId());
        
        // Notify the sender that their messages were read
        messagingTemplate.convertAndSend("/topic/read-updates/" + senderId, Map.of("readerId", currentUser.getId()));

        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editMessage(@PathVariable String id, @RequestBody Map<String, String> payload) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");
        
        Optional<Message> msgOpt = messageRepository.findById(id);
        if (msgOpt.isPresent()) {
            Message msg = msgOpt.get();
            if (msg.getSenderId().equals(currentUser.getId())) {
                LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3);
                if (msg.getTimestamp().isBefore(threeMinutesAgo)) {
                    return ResponseEntity.status(403).body(Map.of("message", "Messages cannot be edited after 3 minutes"));
                }
                msg.setContent(payload.get("content"));
                msg.setEdited(true);
                Message updated = messageRepository.save(msg);
                
                // Notify both parties about the edit
                messagingTemplate.convertAndSend("/topic/message-updates/" + updated.getSenderId(), updated);
                messagingTemplate.convertAndSend("/topic/message-updates/" + updated.getReceiverId(), updated);
                
                return ResponseEntity.ok(updated);
            }
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable String id, @RequestParam(defaultValue = "false") boolean forEveryone) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");
        
        Optional<Message> msgOpt = messageRepository.findById(id);
        if (msgOpt.isPresent()) {
            Message msg = msgOpt.get();
            if (msg.getSenderId().equals(currentUser.getId())) {
                if (forEveryone) {
                    msg.setDeletedForEveryone(true);
                    msg.setDeletedForSender(true);
                    msg.setDeletedForReceiver(true);
                    msg.setContent("");
                    msg.setAttachmentId(null);
                    msg.setFileName(null);
                    messageRepository.save(msg);
                    
                    // Notify receiver about the tombstone
                    messagingTemplate.convertAndSend("/topic/message-updates/" + msg.getReceiverId(), msg);
                } else {
                    msg.setDeletedForSender(true);
                    messageRepository.save(msg);
                }
                return ResponseEntity.ok(Map.of("message", "Deleted"));
            } else if (msg.getReceiverId().equals(currentUser.getId())) {
                msg.setDeletedForReceiver(true);
                messageRepository.save(msg);
                return ResponseEntity.ok(Map.of("message", "Deleted for you"));
            }
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAttachment(@RequestParam("file") MultipartFile file) throws IOException {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");

        String fileId = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType()).toString();
        
        Map<String, String> response = new HashMap<>();
        response.put("attachmentId", fileId);
        response.put("fileName", file.getOriginalFilename());
        response.put("fileType", file.getContentType());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/attachment/{attachmentId}")
    public ResponseEntity<?> downloadAttachment(@PathVariable String attachmentId) throws IOException {
        GridFSFile gridFsFile = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(attachmentId)));

        if (gridFsFile == null) {
            return ResponseEntity.notFound().build();
        }

        GridFsResource resource = gridFsTemplate.getResource(gridFsFile);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(resource.getContentType()))
            .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(org.springframework.util.StreamUtils.copyToByteArray(resource.getInputStream()));
    }
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversationSummaries() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return ResponseEntity.status(401).body("Unauthorized");
        
        String currentUserId = currentUser.getId();
        List<Message> allMsgs = messageRepository.findBySenderIdOrReceiverIdOrderByTimestampDesc(currentUserId, currentUserId);
        
        Map<String, Map<String, Object>> summaries = new HashMap<>();
        
        for (Message m : allMsgs) {
            String otherUserId = m.getSenderId().equals(currentUserId) ? m.getReceiverId() : m.getSenderId();
            
            if (!summaries.containsKey(otherUserId)) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("contactId", otherUserId);
                
                // Content masking for tombstone messages
                boolean isSender = m.getSenderId().equals(currentUserId);
                boolean isDeletedForMe = isSender ? m.isDeletedForSender() : m.isDeletedForReceiver();
                
                if (m.isDeletedForEveryone()) {
                    summary.put("lastMessage", "Message deleted");
                } else if (isDeletedForMe) {
                    summary.put("lastMessage", "Message deleted for you");
                } else if (m.getAttachmentId() != null) {
                    summary.put("lastMessage", "Sent a file");
                } else {
                    summary.put("lastMessage", m.getContent());
                }
                
                summary.put("lastTime", m.getTimestamp());
                summary.put("unreadCount", 0L);
                summaries.put(otherUserId, summary);
            }
            
            // Increment unread count if applicable
            if (m.getReceiverId().equals(currentUserId) && "SENT".equals(m.getStatus())) {
                Map<String, Object> summary = summaries.get(otherUserId);
                summary.put("unreadCount", (long)summary.get("unreadCount") + 1);
            }
        }
        
        return ResponseEntity.ok(new ArrayList<>(summaries.values()));
    }
}
