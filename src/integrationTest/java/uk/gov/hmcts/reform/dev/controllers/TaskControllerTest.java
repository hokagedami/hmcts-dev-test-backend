package uk.gov.hmcts.reform.dev.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.dev.exceptions.GlobalExceptionHandler;
import uk.gov.hmcts.reform.dev.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskPriority;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.models.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.models.dto.TaskResponse;
import uk.gov.hmcts.reform.dev.models.dto.UpdateStatusRequest;
import uk.gov.hmcts.reform.dev.models.dto.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.services.TaskService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    private static final String API_BASE = "/api/v1/tasks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

    @Test
    @DisplayName("POST /api/v1/tasks - Should create task successfully")
    void createTask_Success() throws Exception {
        Task task = createSampleTask();
        CreateTaskRequest request = new CreateTaskRequest("Test Task", "Description",
            LocalDateTime.now().plusDays(1), TaskPriority.MEDIUM);

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(task);

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.message", is("Task created successfully")))
            .andExpect(jsonPath("$.data.id", is(1)))
            .andExpect(jsonPath("$.data.title", is("Test Task")))
            .andExpect(jsonPath("$.data.status", is("PENDING")))
            .andExpect(jsonPath("$.data.priority", is("MEDIUM")))
            .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/tasks - Should return 400 when title is missing")
    void createTask_ValidationError() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest("", "Description",
            LocalDateTime.now().plusDays(1), TaskPriority.MEDIUM);

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.error.type", is("VALIDATION_ERROR")))
            .andExpect(jsonPath("$.error.fieldErrors.title", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} - Should return task by ID")
    void getTaskById_Success() throws Exception {
        Task task = createSampleTask();
        when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get(API_BASE + "/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.id", is(1)))
            .andExpect(jsonPath("$.data.title", is("Test Task")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} - Should return 404 when task not found")
    void getTaskById_NotFound() throws Exception {
        when(taskService.getTaskById(999L)).thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(get(API_BASE + "/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.message", is("Task not found with id: 999")))
            .andExpect(jsonPath("$.error.type", is("NOT_FOUND")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks - Should return page of tasks")
    void getAllTasks_Success() throws Exception {
        Task task1 = createSampleTask();
        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");
        task2.setStatus(TaskStatus.IN_PROGRESS);
        task2.setPriority(TaskPriority.HIGH);
        task2.setDueDateTime(LocalDateTime.now().plusDays(2));
        task2.setCreatedAt(LocalDateTime.now());
        task2.setUpdatedAt(LocalDateTime.now());

        List<TaskResponse> responses = Arrays.asList(
            TaskResponse.fromEntity(task1),
            TaskResponse.fromEntity(task2)
        );
        Page<TaskResponse> page = new PageImpl<>(responses);

        when(taskService.getTasksWithFilters(any(), any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(API_BASE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.items", hasSize(2)))
            .andExpect(jsonPath("$.data.items[0].id", is(1)))
            .andExpect(jsonPath("$.data.items[1].id", is(2)))
            .andExpect(jsonPath("$.data.totalElements", is(2)));
    }

    @Test
    @DisplayName("GET /api/v1/tasks - Should return empty page when no tasks")
    void getAllTasks_EmptyList() throws Exception {
        Page<TaskResponse> emptyPage = new PageImpl<>(List.of());
        when(taskService.getTasksWithFilters(any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get(API_BASE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.items", hasSize(0)))
            .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/tasks - Should filter by status")
    void getAllTasks_FilterByStatus() throws Exception {
        Task task = createSampleTask();
        task.setStatus(TaskStatus.COMPLETED);
        Page<TaskResponse> page = new PageImpl<>(List.of(TaskResponse.fromEntity(task)));

        when(taskService.getTasksWithFilters(eq(TaskStatus.COMPLETED), any(), any(), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get(API_BASE).param("status", "COMPLETED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.items[0].status", is("COMPLETED")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks - Should filter by priority")
    void getAllTasks_FilterByPriority() throws Exception {
        Task task = createSampleTask();
        task.setPriority(TaskPriority.HIGH);
        Page<TaskResponse> page = new PageImpl<>(List.of(TaskResponse.fromEntity(task)));

        when(taskService.getTasksWithFilters(any(), eq(TaskPriority.HIGH), any(), any(Pageable.class)))
            .thenReturn(page);

        mockMvc.perform(get(API_BASE).param("priority", "HIGH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.items[0].priority", is("HIGH")));
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{id} - Should update task fully")
    void updateTask_Success() throws Exception {
        Task updatedTask = createSampleTask();
        updatedTask.setTitle("Updated Title");
        updatedTask.setStatus(TaskStatus.IN_PROGRESS);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated description");
        request.setDueDateTime(LocalDateTime.now().plusDays(5));
        request.setStatus(TaskStatus.IN_PROGRESS);
        request.setPriority(TaskPriority.HIGH);

        when(taskService.updateTask(eq(1L), any(UpdateTaskRequest.class))).thenReturn(updatedTask);

        mockMvc.perform(put(API_BASE + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.title", is("Updated Title")));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status - Should update task status")
    void updateTaskStatus_Success() throws Exception {
        Task task = createSampleTask();
        task.setStatus(TaskStatus.COMPLETED);
        UpdateStatusRequest request = new UpdateStatusRequest(TaskStatus.COMPLETED);

        when(taskService.updateTaskStatus(eq(1L), eq(TaskStatus.COMPLETED))).thenReturn(task);

        mockMvc.perform(patch(API_BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.status", is("COMPLETED")));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status - Should update to CANCELLED")
    void updateTaskStatus_ToCancelled() throws Exception {
        Task task = createSampleTask();
        task.setStatus(TaskStatus.CANCELLED);
        UpdateStatusRequest request = new UpdateStatusRequest(TaskStatus.CANCELLED);

        when(taskService.updateTaskStatus(eq(1L), eq(TaskStatus.CANCELLED))).thenReturn(task);

        mockMvc.perform(patch(API_BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status - Should return 404 when task not found")
    void updateTaskStatus_NotFound() throws Exception {
        UpdateStatusRequest request = new UpdateStatusRequest(TaskStatus.COMPLETED);

        when(taskService.updateTaskStatus(eq(999L), any(TaskStatus.class)))
            .thenThrow(new TaskNotFoundException(999L));

        mockMvc.perform(patch(API_BASE + "/999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} - Should delete task successfully")
    void deleteTask_Success() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete(API_BASE + "/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.message", is("Task deleted successfully")));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} - Should return 404 when task not found")
    void deleteTask_NotFound() throws Exception {
        doThrow(new TaskNotFoundException(999L)).when(taskService).deleteTask(999L);

        mockMvc.perform(delete(API_BASE + "/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/tasks - Should return 400 when dueDateTime is missing")
    void createTask_MissingDueDateTime() throws Exception {
        String requestJson = "{\"title\": \"Test Task\", \"description\": \"Description\"}";

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.error.fieldErrors.dueDateTime", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/tasks - Should create task without description (optional)")
    void createTask_WithoutDescription() throws Exception {
        Task task = createSampleTask();
        task.setDescription(null);
        CreateTaskRequest request = new CreateTaskRequest("Test Task", null,
            LocalDateTime.now().plusDays(1), null);

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(task);

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.title", is("Test Task")));
    }

    @Test
    @DisplayName("POST /api/v1/tasks - Should create task with priority")
    void createTask_WithPriority() throws Exception {
        Task task = createSampleTask();
        task.setPriority(TaskPriority.URGENT);
        CreateTaskRequest request = new CreateTaskRequest("Test Task", "Description",
            LocalDateTime.now().plusDays(1), TaskPriority.URGENT);

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(task);

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.priority", is("URGENT")));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status - Should return 400 when status is missing")
    void updateTaskStatus_MissingStatus() throws Exception {
        String requestJson = "{}";

        mockMvc.perform(patch(API_BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status - Should update to IN_PROGRESS")
    void updateTaskStatus_ToInProgress() throws Exception {
        Task task = createSampleTask();
        task.setStatus(TaskStatus.IN_PROGRESS);
        UpdateStatusRequest request = new UpdateStatusRequest(TaskStatus.IN_PROGRESS);

        when(taskService.updateTaskStatus(eq(1L), eq(TaskStatus.IN_PROGRESS))).thenReturn(task);

        mockMvc.perform(patch(API_BASE + "/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("POST /api/v1/tasks - Should return 400 for invalid JSON")
    void createTask_InvalidJson() throws Exception {
        String invalidJson = "{invalid json}";

        mockMvc.perform(post(API_BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success", is(false)))
            .andExpect(jsonPath("$.error.type", is("MALFORMED_REQUEST")));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} - Should return all task fields including overdue")
    void getTaskById_ReturnsAllFields() throws Exception {
        Task task = createSampleTask();
        when(taskService.getTaskById(1L)).thenReturn(task);

        mockMvc.perform(get(API_BASE + "/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.id", is(1)))
            .andExpect(jsonPath("$.data.title", is("Test Task")))
            .andExpect(jsonPath("$.data.description", is("Test Description")))
            .andExpect(jsonPath("$.data.status", is("PENDING")))
            .andExpect(jsonPath("$.data.priority", is("MEDIUM")))
            .andExpect(jsonPath("$.data.dueDateTime", notNullValue()))
            .andExpect(jsonPath("$.data.createdAt", notNullValue()))
            .andExpect(jsonPath("$.data.updatedAt", notNullValue()))
            .andExpect(jsonPath("$.data.overdue", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/overdue - Should return overdue tasks")
    void getOverdueTasks_Success() throws Exception {
        Task overdueTask = createSampleTask();
        overdueTask.setDueDateTime(LocalDateTime.now().minusDays(1));
        Page<TaskResponse> page = new PageImpl<>(List.of(TaskResponse.fromEntity(overdueTask)));

        when(taskService.getOverdueTasks(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(API_BASE + "/overdue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.items", hasSize(1)));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/bulk - Should create multiple tasks")
    void createBulkTasks_Success() throws Exception {
        Task task1 = createSampleTask();
        Task task2 = createSampleTask();
        task2.setId(2L);
        task2.setTitle("Task 2");

        List<CreateTaskRequest> requests = List.of(
            new CreateTaskRequest("Task 1", "Desc 1", LocalDateTime.now().plusDays(1), TaskPriority.LOW),
            new CreateTaskRequest("Task 2", "Desc 2", LocalDateTime.now().plusDays(2), TaskPriority.HIGH)
        );

        when(taskService.createTasks(any())).thenReturn(List.of(task1, task2));

        mockMvc.perform(post(API_BASE + "/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("PATCH /api/v1/tasks/bulk/status - Should update multiple task statuses")
    void updateBulkStatus_Success() throws Exception {
        String requestJson = "{\"ids\": [1, 2, 3], \"status\": \"COMPLETED\"}";

        when(taskService.updateTasksStatus(any(), eq(TaskStatus.COMPLETED))).thenReturn(3);

        mockMvc.perform(patch(API_BASE + "/bulk/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.affected", is(3)))
            .andExpect(jsonPath("$.data.requested", is(3)));
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/bulk - Should delete multiple tasks")
    void deleteBulkTasks_Success() throws Exception {
        String requestJson = "{\"ids\": [1, 2, 3]}";

        when(taskService.deleteTasks(any())).thenReturn(3);

        mockMvc.perform(delete(API_BASE + "/bulk")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success", is(true)))
            .andExpect(jsonPath("$.data.affected", is(3)))
            .andExpect(jsonPath("$.data.requested", is(3)));
    }

    private Task createSampleTask() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueDateTime(LocalDateTime.now().plusDays(1));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }
}
