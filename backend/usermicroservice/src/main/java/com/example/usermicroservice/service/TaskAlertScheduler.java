package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Task;
import com.example.usermicroservice.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskAlertScheduler {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private AlertService alertService;

    @Scheduled(cron = "0 0 * * * *")
    public void checkOverdueTasks() {
        List<Task> tasks = taskRepository.findAll();
        tasks.forEach(this::syncTaskOverdueAlerts);
    }

    public void syncTaskOverdueAlerts(Task task) {
        LocalDate dueDate = parseDueDate(task.getDueDate());
        boolean overdue = dueDate != null && dueDate.isBefore(LocalDate.now()) && !isTerminalStatus(task.getStatus());

        if (overdue) {
            triggerTaskAlerts(task, dueDate);
        } else {
            resolveTaskAlerts(task);
        }
    }

    private void triggerTaskAlerts(Task task, LocalDate dueDate) {
        String title = "Task Overdue: " + task.getTitle();
        String message = "Task '" + task.getTitle() + "' was due on " + dueDate + ".";

        String managerId = task.getAssignedByUserId();
        if (isBlank(managerId)) {
            alertService.createOrUpdateAlert(
                    managerKey(task),
                    "TASK_OVERDUE",
                    "HIGH",
                    "ROLE",
                    "IT_MANAGER",
                    title,
                    message);
        } else {
            alertService.createOrUpdateAlert(
                    managerKey(task),
                    "TASK_OVERDUE",
                    "HIGH",
                    "USER",
                    managerId,
                    title,
                    message);
        }

        Set<String> assignees = assignedUsers(task);
        if (assignees.isEmpty()) {
            alertService.createOrUpdateAlert(
                    roleKey(task, "TECHNICIAN"),
                    "TASK_OVERDUE",
                    "HIGH",
                    "ROLE",
                    "TECHNICIAN",
                    title,
                    message);
            return;
        }

        assignees.forEach(userId -> alertService.createOrUpdateAlert(
                userKey(task, userId),
                "TASK_OVERDUE",
                "HIGH",
                "USER",
                userId,
                title,
                message));
    }

    private void resolveTaskAlerts(Task task) {
        alertService.resolveAlert(managerKey(task));
        alertService.resolveAlert(roleKey(task, "IT_MANAGER"));
        alertService.resolveAlert(roleKey(task, "TECHNICIAN"));
        assignedUsers(task).forEach(userId -> alertService.resolveAlert(userKey(task, userId)));
    }

    private Set<String> assignedUsers(Task task) {
        Set<String> ids = new LinkedHashSet<>();
        if (!isBlank(task.getAssignedTo())) {
            ids.add(task.getAssignedTo());
        }
        if (task.getAssignedUserIds() != null) {
            task.getAssignedUserIds().stream()
                    .filter(id -> !isBlank(id))
                    .forEach(ids::add);
        }
        return ids;
    }

    private LocalDate parseDueDate(String dueDate) {
        if (isBlank(dueDate)) {
            return null;
        }

        try {
            String datePart = dueDate.length() >= 10 ? dueDate.substring(0, 10) : dueDate;
            return LocalDate.parse(datePart);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean isTerminalStatus(String status) {
        if (status == null) {
            return false;
        }

        return status.equalsIgnoreCase("Completed")
                || status.equalsIgnoreCase("Resolved")
                || status.equalsIgnoreCase("Closed")
                || status.equalsIgnoreCase("Cancelled")
                || status.equalsIgnoreCase("Done");
    }

    private String managerKey(Task task) {
        return "TASK_OVERDUE_MANAGER_" + task.getId();
    }

    private String userKey(Task task, String userId) {
        return "TASK_OVERDUE_USER_" + task.getId() + "_" + userId;
    }

    private String roleKey(Task task, String role) {
        return "TASK_OVERDUE_ROLE_" + task.getId() + "_" + role;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
