package com.interview.prep.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.prep.dto.TaskRequest;
import com.interview.prep.model.TaskStatus;
import com.interview.prep.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void createTask_withValidRequest_returns201() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title("Learn Spring Boot")
                .description("Study REST APIs")
                .status(TaskStatus.TODO)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title", is("Learn Spring Boot")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.createdDate").exists());
    }

    @Test
    void createTask_withBlankTitle_returns400() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title("")
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void createTask_withTitleOver100Chars_returns400() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title("a".repeat(101))
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_withPastDueDate_returns400() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title("Past task")
                .dueDate(LocalDate.now().minusDays(1))
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTask_withDefaultStatus_setsTodo() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title("No status task")
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("TODO")));
    }

    @Test
    void getAllTasks_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllTasks_returnsAllTasks() throws Exception {
        createSampleTask("Task 1");
        createSampleTask("Task 2");

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getAllTasks_filteredByStatus_returnsMatchingTasks() throws Exception {
        createSampleTask("Todo task");
        createTaskWithStatus("In progress task", TaskStatus.IN_PROGRESS);
        createTaskWithStatus("Done task", TaskStatus.DONE);

        mockMvc.perform(get("/api/tasks").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Todo task")));

        mockMvc.perform(get("/api/tasks").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("In progress task")));

        mockMvc.perform(get("/api/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Done task")));
    }

    @Test
    void getTaskById_withExistingId_returns200() throws Exception {
        String response = createSampleTask("Find me");
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Find me")));
    }

    @Test
    void getTaskById_withNonExistingId_returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task not found with id: 999"));
    }

    @Test
    void updateTask_withValidRequest_returns200() throws Exception {
        String response = createSampleTask("Original");
        Long id = objectMapper.readTree(response).get("id").asLong();

        TaskRequest updateRequest = TaskRequest.builder()
                .title("Updated")
                .description("Updated description")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(14))
                .build();

        mockMvc.perform(put("/api/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    void updateTask_withNonExistingId_returns404() throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title("Update nonexistent")
                .build();

        mockMvc.perform(put("/api/tasks/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_withExistingId_returns204() throws Exception {
        String response = createSampleTask("Delete me");
        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/tasks/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTask_withNonExistingId_returns404() throws Exception {
        mockMvc.perform(delete("/api/tasks/{id}", 999))
                .andExpect(status().isNotFound());
    }

    private String createTaskWithStatus(String title, TaskStatus status) throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title(title)
                .status(status)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        return mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private String createSampleTask(String title) throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title(title)
                .status(TaskStatus.TODO)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        return mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
