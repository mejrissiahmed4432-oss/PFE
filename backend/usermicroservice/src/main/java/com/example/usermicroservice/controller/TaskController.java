package com.example.usermicroservice.controller;

import com.example.usermicroservice.dto.TaskAssignRequest;
import com.example.usermicroservice.model.Task;
import com.example.usermicroservice.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/tasks")
@CrossOrigin(origins = "*") // the gateway also handles cors, but just in case
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Task>> getTasksByUser(@PathVariable String userId) {
        return ResponseEntity.ok(taskService.getTasksByUser(userId));
    }

    /**
     * Get tasks where a user is in the assignedUserIds list (used by assigned user's dashboard).
     */
    @GetMapping("/assigned-to/{userId}")
    public ResponseEntity<List<Task>> getTasksAssignedToUser(@PathVariable String userId) {
        return ResponseEntity.ok(taskService.getTasksAssignedToUser(userId));
    }

    /**
     * Get all tasks assigned by a specific IT Manager.
     */
    @GetMapping("/assigned-by/{managerId}")
    public ResponseEntity<List<Task>> getTasksAssignedByManager(@PathVariable String managerId) {
        return ResponseEntity.ok(taskService.getTasksAssignedByManager(managerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable String id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * IT Manager Task Assignment: creates a task and assigns it to multiple users.
     * Status is initialized as "Pending". Notifications are sent to all assignees and the manager.
     */
    @PostMapping("/assign")
    public ResponseEntity<Task> assignTask(@RequestBody TaskAssignRequest request) {
        try {
            return ResponseEntity.ok(taskService.assignTask(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task) {
        return ResponseEntity.ok(taskService.createTask(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable String id, @RequestBody Task task) {
        try {
            return ResponseEntity.ok(taskService.updateTask(id, task));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable String id, @RequestParam String status) {
        try {
            return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}

