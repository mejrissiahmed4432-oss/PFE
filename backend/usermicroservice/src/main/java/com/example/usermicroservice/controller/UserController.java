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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.example.usermicroservice.model.User;
import com.example.usermicroservice.repository.UserRepository;
import com.example.usermicroservice.service.EmailService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = { "http://localhost:4200", "http://localhost" }) // Allow Angular dev server and Docker Nginx
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        if (userOpt.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), userOpt.get().getPassword())) {
            User user = userOpt.get();
            String token = jwtUtils.generateToken(user.getEmail());
            return ResponseEntity.ok(new LoginResponse(
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getPhoto(),
                    token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password"));
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

        // Update password and clear token
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(encodedPassword);
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
        System.out.println("Password successfully updated and hashed for user: " + user.getEmail());

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
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

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
