package com.interview.prep.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.prep.dto.TaskRequest;
import com.interview.prep.model.TaskStatus;
import com.interview.prep.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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

    // ========== CREATE ==========

    @Nested
    class CreateTask {

        @Test
        void withAllFields_returns201WithFullResponse() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("Learn Spring Boot")
                    .description("Study REST APIs and JPA")
                    .status(TaskStatus.IN_PROGRESS)
                    .dueDate(LocalDate.now().plusDays(7))
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.title", is("Learn Spring Boot")))
                    .andExpect(jsonPath("$.description", is("Study REST APIs and JPA")))
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                    .andExpect(jsonPath("$.dueDate", is(LocalDate.now().plusDays(7).toString())))
                    .andExpect(jsonPath("$.createdDate", notNullValue()));
        }

        @Test
        void withOnlyTitle_returns201WithDefaults() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("Minimal task")
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title", is("Minimal task")))
                    .andExpect(jsonPath("$.status", is("TODO")))
                    .andExpect(jsonPath("$.description").doesNotExist())
                    .andExpect(jsonPath("$.dueDate").doesNotExist())
                    .andExpect(jsonPath("$.createdDate", notNullValue()));
        }

        @Test
        void withTodayAsDueDate_returns201() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("Due today")
                    .dueDate(LocalDate.now())
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.dueDate", is(LocalDate.now().toString())));
        }

        @Test
        void withEachStatus_returns201() throws Exception {
            for (TaskStatus s : TaskStatus.values()) {
                TaskRequest request = TaskRequest.builder()
                        .title("Task " + s.name())
                        .status(s)
                        .build();

                mockMvc.perform(post("/api/tasks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status", is(s.name())));
            }
        }

        @Test
        void withTitleExactly100Chars_returns201() throws Exception {
            String title100 = "a".repeat(100);
            TaskRequest request = TaskRequest.builder()
                    .title(title100)
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title", is(title100)));
        }

        @Test
        void withNullTitle_returns400WithFieldError() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\": \"no title\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')].message",
                            hasItem("Title is required")));
        }

        @Test
        void withBlankTitle_returns400WithFieldError() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("")
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").isNotEmpty());
        }

        @Test
        void withWhitespaceOnlyTitle_returns400() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("   ")
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").isNotEmpty());
        }

        @Test
        void withTitleOver100Chars_returns400WithFieldError() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("a".repeat(101))
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')].message",
                            hasItem("Title must not exceed 100 characters")));
        }

        @Test
        void withPastDueDate_returns400WithFieldError() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("Past task")
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'dueDate')].message",
                            hasItem("Due date cannot be in the past")));
        }

        @Test
        void withMultipleValidationErrors_returnsAllFieldErrors() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("")
                    .dueDate(LocalDate.now().minusDays(5))
                    .build();

            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.message", is("One or more fields have invalid values")))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(2)));
        }

        @Test
        void withMalformedJson_returns400() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.message", containsString("Malformed JSON")));
        }

        @Test
        void withInvalidStatusEnum_returns400WithAcceptedValues() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Test\", \"status\": \"INVALID\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.message", containsString("Accepted values")))
                    .andExpect(jsonPath("$.message", containsString("TODO")))
                    .andExpect(jsonPath("$.message", containsString("IN_PROGRESS")))
                    .andExpect(jsonPath("$.message", containsString("DONE")));
        }

        @Test
        void withInvalidDateFormat_returns400() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Test\", \"dueDate\": \"not-a-date\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")));
        }

        @Test
        void withMissingRequestBody_returns400() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")));
        }

        @Test
        void withEmptyJsonObject_returns400() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'title')]").isNotEmpty());
        }
    }

    // ========== GET ALL ==========

    @Nested
    class GetAllTasks {

        @Test
        void withNoTasks_returnsEmptyList() throws Exception {
            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void withMultipleTasks_returnsAll() throws Exception {
            createTask("Task 1", TaskStatus.TODO);
            createTask("Task 2", TaskStatus.IN_PROGRESS);
            createTask("Task 3", TaskStatus.DONE);

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        @Test
        void filterByTodo_returnsOnlyTodoTasks() throws Exception {
            createTask("Todo 1", TaskStatus.TODO);
            createTask("Todo 2", TaskStatus.TODO);
            createTask("In Progress", TaskStatus.IN_PROGRESS);
            createTask("Done", TaskStatus.DONE);

            mockMvc.perform(get("/api/tasks").param("status", "TODO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].status", is("TODO")))
                    .andExpect(jsonPath("$[1].status", is("TODO")));
        }

        @Test
        void filterByInProgress_returnsOnlyInProgressTasks() throws Exception {
            createTask("Todo", TaskStatus.TODO);
            createTask("In Progress", TaskStatus.IN_PROGRESS);

            mockMvc.perform(get("/api/tasks").param("status", "IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title", is("In Progress")));
        }

        @Test
        void filterByDone_returnsOnlyDoneTasks() throws Exception {
            createTask("Todo", TaskStatus.TODO);
            createTask("Done", TaskStatus.DONE);

            mockMvc.perform(get("/api/tasks").param("status", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title", is("Done")));
        }

        @Test
        void filterByStatus_noMatch_returnsEmptyList() throws Exception {
            createTask("Todo", TaskStatus.TODO);

            mockMvc.perform(get("/api/tasks").param("status", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void withInvalidStatusParam_returns400() throws Exception {
            mockMvc.perform(get("/api/tasks").param("status", "INVALID"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.message", containsString("Accepted values")));
        }
    }

    // ========== GET BY ID ==========

    @Nested
    class GetTaskById {

        @Test
        void withExistingId_returns200WithFullTask() throws Exception {
            Long id = createTaskAndGetId("Find me", TaskStatus.IN_PROGRESS);

            mockMvc.perform(get("/api/tasks/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id.intValue())))
                    .andExpect(jsonPath("$.title", is("Find me")))
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                    .andExpect(jsonPath("$.createdDate", notNullValue()));
        }

        @Test
        void withNonExistingId_returns404() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", is("Task not found with id: 999")))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }

        @Test
        void withStringId_returns400() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.message", containsString("Long")));
        }

        @Test
        void withNegativeId_returns404() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", -1))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType", is("NOT_FOUND")));
        }

        @Test
        void withZeroId_returns404() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", 0))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType", is("NOT_FOUND")));
        }

        @Test
        void withVeryLargeId_returns404() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", Long.MAX_VALUE))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType", is("NOT_FOUND")));
        }
    }

    // ========== UPDATE ==========

    @Nested
    class UpdateTask {

        @Test
        void withAllFields_returns200WithUpdatedTask() throws Exception {
            Long id = createTaskAndGetId("Original", TaskStatus.TODO);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("Updated Title")
                    .description("Updated description")
                    .status(TaskStatus.DONE)
                    .dueDate(LocalDate.now().plusDays(14))
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id.intValue())))
                    .andExpect(jsonPath("$.title", is("Updated Title")))
                    .andExpect(jsonPath("$.description", is("Updated description")))
                    .andExpect(jsonPath("$.status", is("DONE")))
                    .andExpect(jsonPath("$.dueDate", is(LocalDate.now().plusDays(14).toString())));
        }

        @Test
        void statusChange_todoToInProgress() throws Exception {
            Long id = createTaskAndGetId("Status change", TaskStatus.TODO);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("Status change")
                    .status(TaskStatus.IN_PROGRESS)
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
        }

        @Test
        void statusChange_inProgressToDone() throws Exception {
            Long id = createTaskAndGetId("Status change", TaskStatus.IN_PROGRESS);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("Status change")
                    .status(TaskStatus.DONE)
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("DONE")));
        }

        @Test
        void withNullStatus_preservesExistingStatus() throws Exception {
            Long id = createTaskAndGetId("Keep status", TaskStatus.IN_PROGRESS);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("Keep status updated")
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                    .andExpect(jsonPath("$.title", is("Keep status updated")));
        }

        @Test
        void clearDescription_setsToNull() throws Exception {
            Long id = createTaskAndGetId("Has desc", TaskStatus.TODO);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("Has desc")
                    .description(null)
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").doesNotExist());
        }

        @Test
        void clearDueDate_setsToNull() throws Exception {
            Long id = createTaskAndGetId("Has due date", TaskStatus.TODO);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("Has due date")
                    .dueDate(null)
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dueDate").doesNotExist());
        }

        @Test
        void preservesCreatedDate() throws Exception {
            Long id = createTaskAndGetId("Check created date", TaskStatus.TODO);

            String original = mockMvc.perform(get("/api/tasks/{id}", id))
                    .andReturn().getResponse().getContentAsString();
            String originalCreatedDate = objectMapper.readTree(original).get("createdDate").asText();

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("Updated title")
                    .status(TaskStatus.DONE)
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.createdDate", is(originalCreatedDate)));
        }

        @Test
        void withNonExistingId_returns404() throws Exception {
            TaskRequest request = TaskRequest.builder()
                    .title("Update nonexistent")
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", 999)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", is("Task not found with id: 999")));
        }

        @Test
        void withBlankTitle_returns400() throws Exception {
            Long id = createTaskAndGetId("Original", TaskStatus.TODO);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("")
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")));
        }

        @Test
        void withTitleOver100Chars_returns400() throws Exception {
            Long id = createTaskAndGetId("Original", TaskStatus.TODO);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("a".repeat(101))
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")));
        }

        @Test
        void withPastDueDate_returns400() throws Exception {
            Long id = createTaskAndGetId("Original", TaskStatus.TODO);

            TaskRequest updateRequest = TaskRequest.builder()
                    .title("Updated")
                    .dueDate(LocalDate.now().minusDays(1))
                    .build();

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'dueDate')].message",
                            hasItem("Due date cannot be in the past")));
        }

        @Test
        void withInvalidStatusInBody_returns400() throws Exception {
            Long id = createTaskAndGetId("Original", TaskStatus.TODO);

            mockMvc.perform(put("/api/tasks/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\": \"Updated\", \"status\": \"WRONG\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.message", containsString("Accepted values")));
        }

        @Test
        void withStringId_returns400() throws Exception {
            TaskRequest request = TaskRequest.builder().title("Test").build();

            mockMvc.perform(put("/api/tasks/{id}", "abc")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")));
        }
    }

    // ========== DELETE ==========

    @Nested
    class DeleteTask {

        @Test
        void withExistingId_returns204AndRemovesTask() throws Exception {
            Long id = createTaskAndGetId("Delete me", TaskStatus.TODO);

            mockMvc.perform(delete("/api/tasks/{id}", id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/tasks/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        void afterDelete_taskNotInList() throws Exception {
            Long id = createTaskAndGetId("Delete me", TaskStatus.TODO);
            createTask("Keep me", TaskStatus.TODO);

            mockMvc.perform(delete("/api/tasks/{id}", id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].title", is("Keep me")));
        }

        @Test
        void withNonExistingId_returns404() throws Exception {
            mockMvc.perform(delete("/api/tasks/{id}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", is("Task not found with id: 999")));
        }

        @Test
        void deleteSameTaskTwice_secondReturns404() throws Exception {
            Long id = createTaskAndGetId("Delete twice", TaskStatus.TODO);

            mockMvc.perform(delete("/api/tasks/{id}", id))
                    .andExpect(status().isNoContent());

            mockMvc.perform(delete("/api/tasks/{id}", id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorType", is("NOT_FOUND")));
        }

        @Test
        void withStringId_returns400() throws Exception {
            mockMvc.perform(delete("/api/tasks/{id}", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")));
        }
    }

    // ========== ERROR RESPONSE FORMAT ==========

    @Nested
    class ErrorResponseFormat {

        @Test
        void validationError_hasConsistentStructure() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[0].field", notNullValue()))
                    .andExpect(jsonPath("$.fieldErrors[0].message", notNullValue()));
        }

        @Test
        void notFoundError_hasConsistentStructure() throws Exception {
            mockMvc.perform(get("/api/tasks/{id}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.errorType", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.fieldErrors").doesNotExist());
        }

        @Test
        void malformedJsonError_hasConsistentStructure() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{bad"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp", notNullValue()))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.errorType", is("INVALID_INPUT")))
                    .andExpect(jsonPath("$.message", notNullValue()))
                    .andExpect(jsonPath("$.fieldErrors").doesNotExist());
        }

        @Test
        void unknownEndpoint_returns401WhenUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/unknown"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorType", is("UNAUTHORIZED")));
        }
    }

    // ========== HELPERS ==========

    private void createTask(String title, TaskStatus status) throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title(title)
                .status(status)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private Long createTaskAndGetId(String title, TaskStatus status) throws Exception {
        TaskRequest request = TaskRequest.builder()
                .title(title)
                .status(status)
                .dueDate(LocalDate.now().plusDays(7))
                .build();

        String response = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }
}
