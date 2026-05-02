package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Task;
import com.example.usermicroservice.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getTasksByUser(String userId) {
        return taskRepository.findByUserId(userId);
    }

    public Optional<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    public Task createTask(Task task) {
        task.prePersist();
        Task saved = taskRepository.save(task);

        if (saved.getAssignedTo() != null && !saved.getAssignedTo().isEmpty()) {
            notificationService.createNotification(
                "New Schedule Task",
                "A new task '" + saved.getTitle() + "' has been assigned to you.",
                "INFO", "SCHEDULE", saved.getId(), saved.getAssignedTo(), null
            );
        } else {
            notificationService.createNotification(
                "New Unassigned Task",
                "A new task '" + saved.getTitle() + "' was created and needs assignment.",
                "INFO", "SCHEDULE", saved.getId(), null, "TECHNICIAN"
            );
        }

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
                    task.setUserId(updatedTask.getUserId());
                    task.setOriginalDueDate(updatedTask.getOriginalDueDate());
                    task.preUpdate();
                    Task saved = taskRepository.save(task);

                    if (saved.getAssignedTo() != null && !saved.getAssignedTo().isEmpty()) {
                        notificationService.createNotification(
                            "Task Updated",
                            "The task '" + saved.getTitle() + "' has been updated.",
                            "INFO", "SCHEDULE", saved.getId(), saved.getAssignedTo(), null
                        );
                    }
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

                    if (saved.getAssignedTo() != null && !saved.getAssignedTo().isEmpty()) {
                        notificationService.createNotification(
                            "Task Status Changed",
                            "The status of task '" + saved.getTitle() + "' is now: " + status,
                            "SUCCESS", "SCHEDULE", saved.getId(), saved.getAssignedTo(), null
                        );
                    }
                    return saved;
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    public void deleteTask(String id) {
        taskRepository.findById(id).ifPresent(task -> {
            if (task.getAssignedTo() != null && !task.getAssignedTo().isEmpty()) {
                notificationService.createNotification(
                    "Task Cancelled",
                    "The task '" + task.getTitle() + "' has been cancelled and removed from your schedule.",
                    "INFO", "SCHEDULE", id, task.getAssignedTo(), null
                );
            }
        });
        taskRepository.deleteById(id);
    }
}
