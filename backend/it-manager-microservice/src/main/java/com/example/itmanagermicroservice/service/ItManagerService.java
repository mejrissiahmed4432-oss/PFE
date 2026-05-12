package com.example.itmanagermicroservice.service;

import com.example.itmanagermicroservice.client.EmployeeClient;
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
}
