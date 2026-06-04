package com.example.usermicroservice.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.usermicroservice.config.JwtUtils;
import com.example.usermicroservice.dto.UpdateEmailRequest;
import com.example.usermicroservice.dto.ChangePasswordRequest;
import com.example.usermicroservice.dto.ForgotPasswordRequest;
import com.example.usermicroservice.dto.LoginRequest;
import com.example.usermicroservice.dto.LoginResponse;
import com.example.usermicroservice.dto.ResetPasswordRequest;
import com.example.usermicroservice.model.RefreshToken;
import com.example.usermicroservice.model.User;
import com.example.usermicroservice.model.UserStatus;
import com.example.usermicroservice.repository.UserRepository;
import com.example.usermicroservice.service.EmailService;
import com.example.usermicroservice.service.RefreshTokenService;

import com.example.usermicroservice.config.WebSocketEventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost" }) // Allow Angular dev server and Docker Nginx
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketEventListener webSocketEventListener;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        if (userOpt.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            
            if (user.getStatus() != null && user.getStatus() == UserStatus.INACTIVE) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Your account has been deactivated. Please contact the IT Manager."));
            }

            user.setLastLogin(LocalDateTime.now());

            // Auto-activate on first login: PENDING → ACTIVE
            if (user.getStatus() == UserStatus.PENDING) {
                user.setStatus(UserStatus.ACTIVE);
                System.out.println("First login detected, activating user: " + user.getEmail());
            }

            userRepository.save(user);

            // Broadcast status change via WebSocket so IT Manager dashboard updates in real-time
            messagingTemplate.convertAndSend("/topic/user-status",
                Map.of(
                    "userId",    user.getId(),
                    "status",    user.getStatus().name(),
                    "email",     user.getEmail(),
                    "lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : ""
                ));

            String token = jwtUtils.generateToken(user.getEmail());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());
            return ResponseEntity.ok(new LoginResponse(
                    user.getId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getPhoto(),
                    token,
                    refreshToken.getToken(),
                    user.getPhoneNumber()));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String requestRefreshToken = request.get("refreshToken");
        if (requestRefreshToken == null || requestRefreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Refresh token is required"));
        }
        return refreshTokenService.findByToken(requestRefreshToken)
            .map(rt -> {
                if (refreshTokenService.isExpired(rt)) {
                    refreshTokenService.deleteByUserEmail(rt.getUserEmail());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "Refresh token expired. Please log in again."));
                }
                String newAccessToken = jwtUtils.generateToken(rt.getUserEmail());
                // Refresh Token Rotation: create a new one, which automatically deletes the old one in createRefreshToken
                RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(rt.getUserEmail());
                
                return ResponseEntity.ok(Map.of(
                    "token", newAccessToken,
                    "refreshToken", newRefreshToken.getToken()
                ));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid refresh token")));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email != null) {
            refreshTokenService.deleteByUserEmail(email);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            try {
                // Generate secure token
                String token = UUID.randomUUID().toString();
                user.setResetToken(token);
                user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(3));
                userRepository.save(user);

                // Send real link with token
                String resetLink = frontendUrl + "/login?token=" + token;
                System.out.println("Generated reset token for " + request.getEmail() + ": " + token);
                emailService.sendResetPasswordEmail(request.getEmail(), resetLink);
                return ResponseEntity.ok(Map.of("message", "Reset link sent successfully"));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Error sending email: " + e.getMessage()));
            }
        }

        // Fix: Return error if email doesn't exist as requested
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Email not found"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        System.out.println("Received reset password request. Token: [" + request.getToken() + "]");
        Optional<User> userOpt = userRepository.findByResetToken(request.getToken());

        if (userOpt.isEmpty()) {
            System.out.println("User not found for token: [" + request.getToken()
                    + "]. Possible token mismatch or database sync issue.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid or expired token"));
        }

        User user = userOpt.get();
        System.out.println("Found user: " + user.getEmail() + ". Link expiry: " + user.getResetTokenExpiry());

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            System.out.println(
                    "Token expired for user: " + user.getEmail() + ". Expiry was: " + user.getResetTokenExpiry());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Token has expired"));
        }

        // Update password and clear token — do NOT change status here.
        // Status transitions: PENDING → ACTIVE only happens on first successful login.
        // INACTIVE users remain INACTIVE even after resetting password.
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        System.out.println("Password saved for " + user.getEmail() + ". Status remains: " + user.getStatus());

        return ResponseEntity.ok(Map.of("message", "Password set successfully. You can now log in."));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Session expired. Please log in again."));
        }

        String email = auth.getPrincipal().toString();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found in current session."));
        }

        User user = userOpt.get();

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Incorrect current password"));
        }

        // Update to new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "message", "Password updated successfully"));
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@Valid @RequestBody UpdateEmailRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Session expired. Please log in again."));
        }

        String currentEmail = auth.getPrincipal().toString();

        // Check if new email is already taken
        if (userRepository.findByEmail(request.getNewEmail()).isPresent()) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Email address already in use"));
        }

        Optional<User> userOpt = userRepository.findByEmail(currentEmail);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User not found in current session."));
        }

        User user = userOpt.get();
        
        // Verify current password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid password. Email update denied."));
        }

        user.setEmail(request.getNewEmail());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("success", true, "message", "Email updated successfully. Please log in again with your new email."));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Session expired. Please log in again."));
        }

        String email = auth.getPrincipal().toString();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        User user = userOpt.get();
        if (request.containsKey("firstName")) user.setFirstName(request.get("firstName"));
        if (request.containsKey("lastName")) user.setLastName(request.get("lastName"));
        if (request.containsKey("phoneNumber")) user.setPhoneNumber(request.get("phoneNumber"));

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "Profile updated successfully",
            "firstName", user.getFirstName(),
            "lastName", user.getLastName(),
            "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : ""
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }
        
        String email = auth.getPrincipal().toString();
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "lastName", user.getLastName() != null ? user.getLastName() : "",
                "role", user.getRole() != null ? user.getRole() : "",
                "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : ""
            ));
        }
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
    }

    @PostMapping("/ping")
    public ResponseEntity<?> ping() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = auth.getPrincipal().toString();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastActive(LocalDateTime.now());
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @GetMapping
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Server-side calculation ignores any client-side timezone mismatches
        for (User user : users) {
             user.setOnline(webSocketEventListener.isUserOnline(user.getId()));
        }
        return users;
    }

    // ─────────────────────────────────────────────
    // IT MANAGER – USER PROVISIONING ENDPOINTS
    // ─────────────────────────────────────────────

    /** Provision a new user from an employee (IT Manager only) */
    @PostMapping("/provision")
    public ResponseEntity<?> provisionUser(@RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }
        // Verify requester is IT_MANAGER
        String requesterEmail = auth.getPrincipal().toString();
        Optional<User> requesterOpt = userRepository.findByEmail(requesterEmail);
        if (requesterOpt.isEmpty() || requesterOpt.get().getRole() != com.example.usermicroservice.model.Role.IT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied – IT Manager only"));
        }

        String email = request.get("email");
        String firstName = request.get("firstName");
        String lastName = request.get("lastName");
        String roleStr = request.get("role");
        String employeeId = request.get("employeeId");

        if (email == null || firstName == null || roleStr == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "email, firstName, and role are required"));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "A user with this email already exists"));
        }

        com.example.usermicroservice.model.Role role;
        try {
            role = com.example.usermicroservice.model.Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role: " + roleStr));
        }

        // Create user with empty password (will be set via token link)
        User newUser = new User();
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName != null ? lastName : "");
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // temporary random password
        newUser.setRole(role);
        newUser.setEmployeeId(employeeId);
        newUser.setStatus(UserStatus.PENDING); // Explicitly set status to PENDING

        // Generate 7-day token for password setup
        String token = UUID.randomUUID().toString();
        newUser.setResetToken(token);
        newUser.setResetTokenExpiry(LocalDateTime.now().plusDays(7));

        userRepository.save(newUser);

        try {
            String setPasswordLink = frontendUrl + "/login?token=" + token + "&mode=invitation";
            emailService.sendWelcomeEmail(email, firstName, setPasswordLink);
        } catch (Exception e) {
            System.err.println("Warning: Could not send welcome email: " + e.getMessage());
            // Don't fail the provisioning if email fails
        }

        return ResponseEntity.ok(Map.of(
            "message", "User provisioned successfully. Welcome email sent.",
            "userId", newUser.getId()
        ));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable String id, @RequestBody Map<String, String> request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String statusStr = request.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "status is required"));
        }

        User user = userOpt.get();
        String targetStatus = statusStr.toUpperCase();

        if ("ACTIVE".equals(targetStatus)) {
            // If user never logged in or was never active, return to PENDING instead of ACTIVE
            if (user.getLastLogin() == null && user.getLastActive() == null) {
                user.setStatus(UserStatus.PENDING);
            } else {
                user.setStatus(UserStatus.ACTIVE);
            }
        } else {
            try {
                user.setStatus(UserStatus.valueOf(targetStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid status value: " + statusStr));
            }
        }

        userRepository.save(user);

        // Broadcast status change via WebSocket so IT Manager dashboard updates in real-time
        messagingTemplate.convertAndSend("/topic/user-status",
            Map.of(
                "userId",    user.getId(),
                "status",    user.getStatus().name(),
                "email",     user.getEmail(),
                "lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : ""
            ));

        return ResponseEntity.ok(Map.of(
            "message", "User status updated successfully",
            "newStatus", user.getStatus()
        ));
    }

    /** Delete a user (IT Manager only) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }
        String requesterEmail = auth.getPrincipal().toString();
        Optional<User> requesterOpt = userRepository.findByEmail(requesterEmail);
        if (requesterOpt.isEmpty() || requesterOpt.get().getRole() != com.example.usermicroservice.model.Role.IT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied – IT Manager only"));
        }
        // Prevent self-deletion
        if (requesterOpt.get().getId().equals(id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete your own account"));
        }
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        userRepository.deleteById(id);

        // Broadcast DELETED status so the user is auto-logged out if they are online
        messagingTemplate.convertAndSend("/topic/user-status",
            Map.of(
                "userId", id,
                "status", "DELETED"
            ));

        return ResponseEntity.ok(Map.of("message", "User access revoked successfully"));
    }

    /** Update user role (IT Manager only) */
    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable String id, @RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }
        String requesterEmail = auth.getPrincipal().toString();
        Optional<User> requesterOpt = userRepository.findByEmail(requesterEmail);
        if (requesterOpt.isEmpty() || requesterOpt.get().getRole() != com.example.usermicroservice.model.Role.IT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied – IT Manager only"));
        }
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        com.example.usermicroservice.model.Role newRole;
        try {
            newRole = com.example.usermicroservice.model.Role.valueOf(request.get("role"));
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role"));
        }
        User user = userOpt.get();
        user.setRole(newRole);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
    }

    /** Resend welcome/invitation email (IT Manager only) */
    @PostMapping("/{id}/resend-invitation")
    public ResponseEntity<?> resendInvitation(@PathVariable String id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }
        String requesterEmail = auth.getPrincipal().toString();
        Optional<User> requesterOpt = userRepository.findByEmail(requesterEmail);
        if (requesterOpt.isEmpty() || requesterOpt.get().getRole() != com.example.usermicroservice.model.Role.IT_MANAGER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Access denied – IT Manager only"));
        }
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }
        User user = userOpt.get();
        // Refresh token for 7 more days
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);
        try {
            String setPasswordLink = frontendUrl + "/login?token=" + token + "&mode=invitation";
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), setPasswordLink);
            return ResponseEntity.ok(Map.of("message", "Invitation resent successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to send email: " + e.getMessage()));
        }
    }
}
