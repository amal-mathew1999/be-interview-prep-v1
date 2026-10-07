package com.interview.prep.service;

import com.interview.prep.dto.TaskRequest;
import com.interview.prep.dto.TaskResponse;
import com.interview.prep.exception.TaskNotFoundException;
import com.interview.prep.model.Task;
import com.interview.prep.model.TaskStatus;
import com.interview.prep.repository.TaskRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Nested
    class CreateTask {

        @Test
        void withAllFields_savesAndReturnsTask() {
            TaskRequest request = TaskRequest.builder()
                    .title("New Task")
                    .description("Description")
                    .status(TaskStatus.IN_PROGRESS)
                    .dueDate(LocalDate.now().plusDays(5))
                    .build();

            Task saved = Task.builder()
                    .id(1L)
                    .title("New Task")
                    .description("Description")
                    .status(TaskStatus.IN_PROGRESS)
                    .dueDate(LocalDate.now().plusDays(5))
                    .createdDate(LocalDateTime.now())
                    .build();

            when(taskRepository.save(any(Task.class))).thenReturn(saved);

            TaskResponse response = taskService.createTask(request);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("New Task");
            assertThat(response.getDescription()).isEqualTo("Description");
            assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(response.getDueDate()).isEqualTo(LocalDate.now().plusDays(5));
            assertThat(response.getCreatedDate()).isNotNull();
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        void withNullStatus_defaultsToTodo() {
            TaskRequest request = TaskRequest.builder()
                    .title("No Status")
                    .build();

            Task saved = Task.builder()
                    .id(1L)
                    .title("No Status")
                    .status(TaskStatus.TODO)
                    .createdDate(LocalDateTime.now())
                    .build();

            when(taskRepository.save(any(Task.class))).thenReturn(saved);

            TaskResponse response = taskService.createTask(request);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void withNullDescription_savesWithoutDescription() {
            TaskRequest request = TaskRequest.builder()
                    .title("No Description")
                    .build();

            Task saved = Task.builder()
                    .id(1L)
                    .title("No Description")
                    .status(TaskStatus.TODO)
                    .createdDate(LocalDateTime.now())
                    .build();

            when(taskRepository.save(any(Task.class))).thenReturn(saved);

            TaskResponse response = taskService.createTask(request);

            assertThat(response.getDescription()).isNull();
        }
    }

    @Nested
    class GetAllTasks {

        @Test
        void withNoFilter_returnsAll() {
            List<Task> tasks = List.of(
                    buildTask(1L, "Task 1", TaskStatus.TODO),
                    buildTask(2L, "Task 2", TaskStatus.DONE));

            when(taskRepository.findAll()).thenReturn(tasks);

            List<TaskResponse> result = taskService.getAllTasks(null);

            assertThat(result).hasSize(2);
            verify(taskRepository).findAll();
            verify(taskRepository, never()).findByStatus(any());
        }

        @Test
        void withStatusFilter_returnsFiltered() {
            List<Task> tasks = List.of(
                    buildTask(1L, "Todo Task", TaskStatus.TODO));

            when(taskRepository.findByStatus(TaskStatus.TODO)).thenReturn(tasks);

            List<TaskResponse> result = taskService.getAllTasks(TaskStatus.TODO);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(TaskStatus.TODO);
            verify(taskRepository).findByStatus(TaskStatus.TODO);
            verify(taskRepository, never()).findAll();
        }

        @Test
        void withNoTasks_returnsEmptyList() {
            when(taskRepository.findAll()).thenReturn(Collections.emptyList());

            List<TaskResponse> result = taskService.getAllTasks(null);

            assertThat(result).isEmpty();
        }

        @Test
        void withStatusFilter_noMatch_returnsEmptyList() {
            when(taskRepository.findByStatus(TaskStatus.DONE)).thenReturn(Collections.emptyList());

            List<TaskResponse> result = taskService.getAllTasks(TaskStatus.DONE);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetTaskById {

        @Test
        void withExistingId_returnsTask() {
            Task task = buildTask(1L, "Found", TaskStatus.TODO);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskById(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("Found");
        }

        @Test
        void withNonExistingId_throwsTaskNotFoundException() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(999L))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessage("Task not found with id: 999");
        }
    }

    @Nested
    class UpdateTask {

        @Test
        void withAllFields_updatesAndReturns() {
            Task existing = buildTask(1L, "Old Title", TaskStatus.TODO);
            existing.setDescription("Old desc");

            when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskRequest request = TaskRequest.builder()
                    .title("New Title")
                    .description("New desc")
                    .status(TaskStatus.DONE)
                    .dueDate(LocalDate.now().plusDays(10))
                    .build();

            TaskResponse response = taskService.updateTask(1L, request);

            assertThat(response.getTitle()).isEqualTo("New Title");
            assertThat(response.getDescription()).isEqualTo("New desc");
            assertThat(response.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(response.getDueDate()).isEqualTo(LocalDate.now().plusDays(10));
        }

        @Test
        void withNullStatus_preservesExistingStatus() {
            Task existing = buildTask(1L, "Keep Status", TaskStatus.IN_PROGRESS);
            when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskRequest request = TaskRequest.builder()
                    .title("Updated")
                    .build();

            TaskResponse response = taskService.updateTask(1L, request);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        void withNullDescription_clearsDescription() {
            Task existing = buildTask(1L, "Title", TaskStatus.TODO);
            existing.setDescription("Has description");

            when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskRequest request = TaskRequest.builder()
                    .title("Title")
                    .description(null)
                    .build();

            TaskResponse response = taskService.updateTask(1L, request);

            assertThat(response.getDescription()).isNull();
        }

        @Test
        void withNullDueDate_clearsDueDate() {
            Task existing = buildTask(1L, "Title", TaskStatus.TODO);
            existing.setDueDate(LocalDate.now().plusDays(5));

            when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            TaskRequest request = TaskRequest.builder()
                    .title("Title")
                    .dueDate(null)
                    .build();

            TaskResponse response = taskService.updateTask(1L, request);

            assertThat(response.getDueDate()).isNull();
        }

        @Test
        void withNonExistingId_throwsTaskNotFoundException() {
            when(taskRepository.findById(999L)).thenReturn(Optional.empty());

            TaskRequest request = TaskRequest.builder()
                    .title("Update")
                    .build();

            assertThatThrownBy(() -> taskService.updateTask(999L, request))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessage("Task not found with id: 999");

            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteTask {

        @Test
        void withExistingId_deletesTask() {
            when(taskRepository.existsById(1L)).thenReturn(true);

            taskService.deleteTask(1L);

            verify(taskRepository).deleteById(1L);
        }

        @Test
        void withNonExistingId_throwsTaskNotFoundException() {
            when(taskRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> taskService.deleteTask(999L))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessage("Task not found with id: 999");

            verify(taskRepository, never()).deleteById(any());
        }
    }

    private Task buildTask(Long id, String title, TaskStatus status) {
        return Task.builder()
                .id(id)
                .title(title)
                .status(status)
                .createdDate(LocalDateTime.now())
                .build();
    }
}
