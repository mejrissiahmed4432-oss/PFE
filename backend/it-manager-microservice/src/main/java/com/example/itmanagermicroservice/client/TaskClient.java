package com.example.itmanagermicroservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-microservice", contextId = "taskClient", fallback = TaskClientFallback.class)
public interface TaskClient {

    @PostMapping("/api/users/tasks/assign")
    Map<String, Object> assignTask(@RequestBody Map<String, Object> request);

    @GetMapping("/api/users/tasks")
    List<Map<String, Object>> getAllTasks();

    @GetMapping("/api/users/tasks/assigned-by/{managerId}")
    List<Map<String, Object>> getTasksAssignedByManager(@PathVariable("managerId") String managerId);

    @GetMapping("/api/users/tasks/assigned-to/{userId}")
    List<Map<String, Object>> getTasksAssignedToUser(@PathVariable("userId") String userId);

    @PutMapping("/api/users/tasks/{id}")
    Map<String, Object> updateTask(@PathVariable("id") String id, @RequestBody Map<String, Object> task);

    @PatchMapping("/api/users/tasks/{id}/status")
    Map<String, Object> updateTaskStatus(@PathVariable("id") String id, @RequestParam("status") String status);

    @DeleteMapping("/api/users/tasks/{id}")
    void deleteTask(@PathVariable("id") String id);
}
