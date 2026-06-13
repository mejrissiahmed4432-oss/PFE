package com.example.itmanagermicroservice.controller;

import com.example.itmanagermicroservice.config.BlockchainTraceable;
import com.example.itmanagermicroservice.dto.EmployeeDTO;
import com.example.itmanagermicroservice.service.ItManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/it-manager")
public class ItManagerController {

    @Autowired
    private ItManagerService itManagerService;

    // ── User Management ───────────────────────────────────────────────────────

    @GetMapping("/employees")
    public List<EmployeeDTO> getAllEmployees() {
        return itManagerService.getAllEmployees();
    }

    @GetMapping("/users")
    public List<Map<String, Object>> getAllUsers() {
        return itManagerService.getAllUsers();
    }

   
    @PostMapping("/provision")
    public Map<String, Object> provisionUser(@RequestBody Map<String, Object> request) {
        return itManagerService.provisionUser(request);
    }


    @PutMapping("/users/{id}/status")
    public Map<String, Object> updateUserStatus(@PathVariable String id, @RequestBody Map<String, String> request) {
        return itManagerService.updateUserStatus(id, request);
    }


    @PutMapping("/users/{id}/role")
    public Map<String, Object> updateUserRole(@PathVariable String id, @RequestBody Map<String, String> request) {
        return itManagerService.updateUserRole(id, request);
    }

    @PostMapping("/users/{id}/resend-invitation")
    public Map<String, Object> resendInvitation(@PathVariable String id) {
        return itManagerService.resendInvitation(id);
    }


    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        itManagerService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    // ── Task Management ───────────────────────────────────────────────────────

    /**
     * Create and assign a task to one or more users.
     * Status is initialized as "To Do". Notifications are sent automatically.
     */
    @BlockchainTraceable(action = "Assign task")
    @PostMapping("/tasks/assign")
    public ResponseEntity<?> assignTask(@RequestBody Map<String, Object> request) {
        try {
            return ResponseEntity.ok(itManagerService.assignTask(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all tasks (IT Manager overview).
     */
    @GetMapping("/tasks")
    public List<Map<String, Object>> getAllTasks() {
        return itManagerService.getAllTasks();
    }

    /**
     * Get tasks created by a specific IT Manager.
     */
    @GetMapping("/tasks/assigned-by/{managerId}")
    public List<Map<String, Object>> getTasksAssignedByManager(@PathVariable String managerId) {
        return itManagerService.getTasksAssignedByManager(managerId);
    }

    /**
     * Get tasks assigned to a specific user.
     */
    @GetMapping("/tasks/assigned-to/{userId}")
    public List<Map<String, Object>> getTasksAssignedToUser(@PathVariable String userId) {
        return itManagerService.getTasksAssignedToUser(userId);
    }

    /**
     * Update a task (change title, description, priority, due date, assignees, etc.).
     */
    @PutMapping("/tasks/{id}")
    public ResponseEntity<?> updateTask(@PathVariable String id, @RequestBody Map<String, Object> task) {
        try {
            return ResponseEntity.ok(itManagerService.updateTask(id, task));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update task status (e.g., "To Do" → "In Progress" → "Done").
     */
    @PatchMapping("/tasks/{id}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable String id, @RequestParam String status) {
        try {
            return ResponseEntity.ok(itManagerService.updateTaskStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a task (notifies all assigned users before deletion).
     */
    @BlockchainTraceable(action = "Delete Assigned task")
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable String id) {
        itManagerService.deleteTask(id);
        return ResponseEntity.ok().build();
    }
}
