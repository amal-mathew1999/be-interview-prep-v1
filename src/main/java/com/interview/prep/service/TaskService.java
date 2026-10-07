package com.interview.prep.service;

import com.interview.prep.dto.TaskRequest;
import com.interview.prep.dto.TaskResponse;
import com.interview.prep.exception.TaskNotFoundException;
import com.interview.prep.model.Task;
import com.interview.prep.model.TaskStatus;
import com.interview.prep.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskResponse createTask(TaskRequest request) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .dueDate(request.getDueDate())
                .createdDate(LocalDateTime.now())
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

    public List<TaskResponse> getAllTasks(TaskStatus status) {
        if (status != null) {
            return taskRepository.findByStatus(status).stream()
                    .map(TaskResponse::from)
                    .toList();
        }
        return taskRepository.findAll().stream()
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskResponse.from(task);
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        task.setDueDate(request.getDueDate());

        return TaskResponse.from(taskRepository.save(task));
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }
}
