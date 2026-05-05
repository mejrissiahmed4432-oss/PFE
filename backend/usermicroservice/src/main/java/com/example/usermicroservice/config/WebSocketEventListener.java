package com.example.usermicroservice.config;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.example.usermicroservice.model.User;
import com.example.usermicroservice.repository.UserRepository;

@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    // Maps a WebSocket Session ID to a User ID
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    // Maps a User ID to a Set of active Session IDs (idempotent, prevents double-counting bugs)
    private final Map<String, java.util.Set<String>> activeUserSessions = new ConcurrentHashMap<>();

    // Expose online status accurately to the rest of the application
    public boolean isUserOnline(String userId) {
        java.util.Set<String> sessions = activeUserSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (destination != null && destination.startsWith("/topic/messages/") && sessionId != null) {
            String userId = destination.substring("/topic/messages/".length());
            
            // Map the session to the User ID
            sessionUserMap.put(sessionId, userId);
            
            // Add session to user's active session set idempotently
            java.util.Set<String> userSessions = activeUserSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
            boolean isFirstConnection = userSessions.isEmpty();
            userSessions.add(sessionId);

            System.out.println("[WebSocket Event] User " + userId + " subscribed. Active tabs for user: " + userSessions.size());

            // If this is the user's first tab, broadcast that they are online
            if (isFirstConnection) {
                System.out.println("[WebSocket Event] Broadcasting User " + userId + " ONLINE instantly.");
                updateDatabaseLastActive(userId, LocalDateTime.now());
                broadcastUserStatus(userId, true);
            }
        }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            String userId = sessionUserMap.remove(sessionId);
            
            if (userId != null) {
                java.util.Set<String> userSessions = activeUserSessions.get(userId);
                if (userSessions != null) {
                    userSessions.remove(sessionId);
                    
                    System.out.println("[WebSocket Event] User " + userId + " disconnected. Remaining active tabs: " + userSessions.size());
                    
                    // If the user has closed their last tab/connection, broadcast that they are offline
                    if (userSessions.isEmpty()) {
                        System.out.println("[WebSocket Event] Broadcasting User " + userId + " OFFLINE instantly.");
                        activeUserSessions.remove(userId);
                        // Substract 5 minutes to force offline state instantly in the HTTP polling layer too
                        updateDatabaseLastActive(userId, LocalDateTime.now().minusMinutes(5));
                        broadcastUserStatus(userId, false);
                    }
                }
            }
        }
    }

    private void updateDatabaseLastActive(String userId, LocalDateTime time) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActive(time);
            userRepository.save(user);
        });
    }

    private void broadcastUserStatus(String userId, boolean isOnline) {
        // Send a simple payload: { "userId": "...", "online": true/false }
        Map<String, Object> payload = Map.of(
            "userId", userId,
            "online", isOnline
        );
        messagingTemplate.convertAndSend("/topic/user-status", payload);
    }
}
