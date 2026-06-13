package com.example.usermicroservice.service;

import com.example.usermicroservice.dto.TaskAssignRequest;
import com.example.usermicroservice.model.Task;
import com.example.usermicroservice.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;
    private final TaskAlertScheduler taskAlertScheduler;

    public TaskService(TaskRepository taskRepository,
                       NotificationService notificationService,
                       TaskAlertScheduler taskAlertScheduler) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
        this.taskAlertScheduler = taskAlertScheduler;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getTasksByUser(String userId) {
        List<Task> createdTasks = taskRepository.findByUserId(userId);
        List<Task> assignedTasksMulti = taskRepository.findByAssignedUserIdsContaining(userId);
        
        java.util.Map<String, Task> taskMap = new java.util.LinkedHashMap<>();
        for (Task t : createdTasks) { taskMap.put(t.getId(), t); }
        for (Task t : assignedTasksMulti) { taskMap.put(t.getId(), t); }
        
        return new java.util.ArrayList<>(taskMap.values());
    }

    /**
     * Get all tasks where this user is in the assignedUserIds list (multi-assignment).
     */
    public List<Task> getTasksAssignedToUser(String userId) {
        return taskRepository.findByAssignedUserIdsContaining(userId);
    }

    /**
     * Get all tasks assigned by this IT Manager.
     */
    public List<Task> getTasksAssignedByManager(String managerId) {
        return taskRepository.findByAssignedByUserId(managerId);
    }

    public Optional<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    /**
     * IT Manager Task Assignment: creates a task with status "To Do" and
     * assigns it to multiple users, then notifies each user and the manager.
     */
    public Task assignTask(TaskAssignRequest request) {
        if (request.getAssignedUserIds() == null || request.getAssignedUserIds().isEmpty()) {
            throw new IllegalArgumentException("At least one user must be assigned to the task.");
        }

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setType(request.getType());
        task.setDueDate(request.getDueDate());
        task.setAssignedByUserId(request.getAssignedByUserId());
        task.setAssignedUserIds(request.getAssignedUserIds());
        task.setStatus("Pending");
        task.setOriginalDueDate(request.getDueDate());
        task.prePersist();

        Task saved = taskRepository.save(task);

        // Notify each assigned user individually
        for (String userId : saved.getAssignedUserIds()) {
            notificationService.createNotification(
                    "New Task Assigned",
                    "You have been assigned a new task: '" + saved.getTitle() + "'. Priority: " + saved.getPriority() + ". Due: " + saved.getDueDate(),
                    "INFO", "TASK", saved.getId(), userId, null);
        }

        // Notify the IT Manager that the task was created and assigned successfully
        if (saved.getAssignedByUserId() != null && !saved.getAssignedByUserId().isEmpty()) {
            int count = saved.getAssignedUserIds().size();
            notificationService.createNotification(
                    "Task Assigned Successfully",
                    "Task '" + saved.getTitle() + "' has been assigned to " + count + " user(s) with status 'Pending'.",
                    "SUCCESS", "TASK", saved.getId(), saved.getAssignedByUserId(), null);
        }

        taskAlertScheduler.syncTaskOverdueAlerts(saved);
        return saved;
    }

    public Task createTask(Task task) {
        task.prePersist();
        Task saved = taskRepository.save(task);

        if (saved.getAssignedTo() != null && !saved.getAssignedTo().isEmpty()) {
            notificationService.createNotification(
                    "New Schedule Task",
                    "A new task '" + saved.getTitle() + "' has been assigned to you.",
                    "INFO", "SCHEDULE", saved.getId(), saved.getAssignedTo(), null);
        } else {
            notificationService.createNotification(
                    "New Unassigned Task",
                    "A new task '" + saved.getTitle() + "' was created and needs assignment.",
                    "INFO", "SCHEDULE", saved.getId(), null, "TECHNICIAN");
        }

        taskAlertScheduler.syncTaskOverdueAlerts(saved);
        return saved;
    }

    public Task updateTask(String id, Task updatedTask) {
        return taskRepository.findById(id)
                .map(task -> {
                    task.setTitle(updatedTask.getTitle());
                    task.setDescription(updatedTask.getDescription());
                    task.setType(updatedTask.getType());
                    task.setPriority(updatedTask.getPriority());
                    task.setStatus(updatedTask.getStatus());
                    task.setDueDate(updatedTask.getDueDate());
                    task.setAssignedTo(updatedTask.getAssignedTo());

                    List<String> oldAssignees = task.getAssignedUserIds() != null ? new java.util.ArrayList<>(task.getAssignedUserIds()) : new java.util.ArrayList<>();
                    task.setAssignedUserIds(updatedTask.getAssignedUserIds());

                    if (updatedTask.getOriginalDueDate() != null) {
                        task.setOriginalDueDate(updatedTask.getOriginalDueDate());
                    }
                    task.preUpdate();
                    Task saved = taskRepository.save(task);

                    if (saved.getAssignedTo() != null && !saved.getAssignedTo().isEmpty()) {
                        notificationService.createNotification(
                                "Task Updated",
                                "The task '" + saved.getTitle() + "' has been updated.",
                                "INFO", "SCHEDULE", saved.getId(), saved.getAssignedTo(), null);
                    }

                    if (saved.getAssignedUserIds() != null) {
                        for (String userId : saved.getAssignedUserIds()) {
                            if (!oldAssignees.contains(userId)) {
                                notificationService.createNotification(
                                        "New Task Assigned",
                                        "You have been assigned a new task: '" + saved.getTitle() + "'. Due: " + saved.getDueDate(),
                                        "INFO", "TASK", saved.getId(), userId, null);
                            } else {
                                notificationService.createNotification(
                                        "Task Updated",
                                        "The task '" + saved.getTitle() + "' has been updated.",
                                        "INFO", "TASK", saved.getId(), userId, null);
                            }
                        }
                    }
                    taskAlertScheduler.syncTaskOverdueAlerts(saved);
                    return saved;
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    public Task updateTaskStatus(String id, String status) {
        return taskRepository.findById(id)
                .map(task -> {
                    task.setStatus(status);
                    task.preUpdate();
                    Task saved = taskRepository.save(task);

                    // Notify the IT Manager who assigned the task
                    if (saved.getAssignedByUserId() != null && !saved.getAssignedByUserId().isEmpty()) {
                        notificationService.createNotification(
                                "Task Status Updated",
                                "Task '" + saved.getTitle() + "' status changed to: " + status,
                                "INFO", "TASK", saved.getId(), saved.getAssignedByUserId(), null);
                    }

                    // Notify assigned users (legacy single user + multi)
                    if (saved.getAssignedTo() != null && !saved.getAssignedTo().isEmpty()) {
                        notificationService.createNotification(
                                "Task Status Changed",
                                "The status of task '" + saved.getTitle() + "' is now: " + status,
                                "SUCCESS", "SCHEDULE", saved.getId(), saved.getAssignedTo(), null);
                    }
                    taskAlertScheduler.syncTaskOverdueAlerts(saved);
                    return saved;
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    public void deleteTask(String id) {
        taskRepository.findById(id).ifPresent(task -> {
            task.setStatus("Cancelled");
            taskAlertScheduler.syncTaskOverdueAlerts(task);

            // Notify multi-assigned users
            if (task.getAssignedUserIds() != null && !task.getAssignedUserIds().isEmpty()) {
                for (String userId : task.getAssignedUserIds()) {
                    notificationService.createNotification(
                            "Task Cancelled",
                            "The task '" + task.getTitle() + "' has been cancelled.",
                            "INFO", "TASK", id, userId, null);
                }
            } else if (task.getAssignedTo() != null && !task.getAssignedTo().isEmpty()) {
                notificationService.createNotification(
                        "Task Cancelled",
                        "The task '" + task.getTitle() + "' has been cancelled and removed from your schedule.",
                        "INFO", "SCHEDULE", id, task.getAssignedTo(), null);
            }
        });
        taskRepository.deleteById(id);
    }
}

