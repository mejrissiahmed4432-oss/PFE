package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Task;
import com.example.usermicroservice.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Optional<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    public Task createTask(Task task) {
        task.prePersist();
        return taskRepository.save(task);
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
                    task.setOriginalDueDate(updatedTask.getOriginalDueDate());
                    task.preUpdate();
                    return taskRepository.save(task);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    public Task updateTaskStatus(String id, String status) {
        return taskRepository.findById(id)
                .map(task -> {
                    task.setStatus(status);
                    task.preUpdate();
                    return taskRepository.save(task);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
    }

    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }
}
