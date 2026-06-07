package com.example.itmanagermicroservice.service;

import com.example.itmanagermicroservice.client.EmployeeClient;
import com.example.itmanagermicroservice.client.TaskClient;
import com.example.itmanagermicroservice.client.UserClient;
import com.example.itmanagermicroservice.dto.EmployeeDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ItManagerService {

    @Autowired
    private EmployeeClient employeeClient;

    @Autowired
    private UserClient userClient;

    @Autowired
    private TaskClient taskClient;

    // ── User Management ───────────────────────────────────────────────────────

    public List<EmployeeDTO> getAllEmployees() {
        return employeeClient.getAllEmployees();
    }

    public List<Map<String, Object>> getAllUsers() {
        return userClient.getAllUsers();
    }

    public Map<String, Object> provisionUser(Map<String, Object> request) {
        return userClient.provisionUser(request);
    }

    public Map<String, Object> updateUserStatus(String id, Map<String, String> request) {
        return userClient.updateUserStatus(id, request);
    }

    public Map<String, Object> updateUserRole(String id, Map<String, String> request) {
        return userClient.updateUserRole(id, request);
    }

    public Map<String, Object> resendInvitation(String id) {
        return userClient.resendInvitation(id);
    }

    public void deleteUser(String id) {
        userClient.deleteUser(id);
    }

    // ── Task Management ───────────────────────────────────────────────────────

    public Map<String, Object> assignTask(Map<String, Object> request) {
        return taskClient.assignTask(request);
    }

    public List<Map<String, Object>> getAllTasks() {
        return taskClient.getAllTasks();
    }

    public List<Map<String, Object>> getTasksAssignedByManager(String managerId) {
        return taskClient.getTasksAssignedByManager(managerId);
    }

    public List<Map<String, Object>> getTasksAssignedToUser(String userId) {
        return taskClient.getTasksAssignedToUser(userId);
    }

    public Map<String, Object> updateTask(String id, Map<String, Object> task) {
        return taskClient.updateTask(id, task);
    }

    public Map<String, Object> updateTaskStatus(String id, String status) {
        return taskClient.updateTaskStatus(id, status);
    }

    public void deleteTask(String id) {
        taskClient.deleteTask(id);
    }
}

