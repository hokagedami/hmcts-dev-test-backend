package uk.gov.hmcts.reform.dev.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskPriority;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.models.dto.ApiResponse;
import uk.gov.hmcts.reform.dev.models.dto.BulkDeleteRequest;
import uk.gov.hmcts.reform.dev.models.dto.BulkStatusUpdateRequest;
import uk.gov.hmcts.reform.dev.models.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.models.dto.PagedData;
import uk.gov.hmcts.reform.dev.models.dto.TaskResponse;
import uk.gov.hmcts.reform.dev.models.dto.UpdateStatusRequest;
import uk.gov.hmcts.reform.dev.models.dto.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.services.TaskService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Task Management", description = "APIs for managing tasks")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create a new task")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "Task created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody CreateTaskRequest request) {
        log.info("Creating new task with title: '{}', priority: {}, dueDateTime: {}",
            request.getTitle(), request.getPriority(), request.getDueDateTime());
        log.debug("Full create task request: {}", request);

        Task task = taskService.createTask(request);

        log.info("Task created successfully with ID: {}", task.getId());
        MDC.put("taskId", String.valueOf(task.getId()));

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(TaskResponse.fromEntity(task), "Task created successfully"));
    }

    @Operation(summary = "Create multiple tasks")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "Tasks created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid request body")
    })
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> createTasks(
            @Valid @RequestBody List<CreateTaskRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            log.warn("Bulk create rejected: empty or null request list");
            throw new IllegalArgumentException("At least one task is required");
        }
        log.info("Creating {} tasks in bulk operation", requests.size());
        log.debug("Bulk create request details: titles={}",
            requests.stream().map(CreateTaskRequest::getTitle).toList());

        List<Task> tasks = taskService.createTasks(requests);
        List<TaskResponse> responses = tasks.stream()
            .map(TaskResponse::fromEntity)
            .toList();

        log.info("Bulk create completed: {} tasks created successfully", tasks.size());
        log.debug("Created task IDs: {}", tasks.stream().map(Task::getId).toList());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(responses,
                String.format("%d task(s) created successfully", responses.size())));
    }

    @Operation(summary = "Get a task by ID")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Task found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Task not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        log.info("Fetching task by ID: {}", id);
        MDC.put("taskId", String.valueOf(id));

        Task task = taskService.getTaskById(id);

        log.info("Task found: ID={}, title='{}', status={}, priority={}",
            task.getId(), task.getTitle(), task.getStatus(), task.getPriority());
        log.debug("Full task details: {}", task);

        return ResponseEntity.ok(ApiResponse.success(TaskResponse.fromEntity(task), "Task retrieved successfully"));
    }

    @Operation(summary = "Get all tasks with optional filtering, pagination and sorting")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200", description = "Page of tasks")
    @GetMapping
    public ResponseEntity<ApiResponse<PagedData<TaskResponse>>> getAllTasks(
            @Parameter(description = "Filter by status") @RequestParam(required = false) TaskStatus status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) TaskPriority priority,
            @Parameter(description = "Search in title") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Fetching tasks with filters - status: {}, priority: {}, search: '{}', page: {}, size: {}, sort: {}",
            status, priority, search, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<TaskResponse> tasks = taskService.getTasksWithFilters(status, priority, search, pageable);

        log.info("Tasks retrieved: {} items on page {}/{}, total: {}",
            tasks.getNumberOfElements(), tasks.getNumber() + 1, tasks.getTotalPages(), tasks.getTotalElements());
        log.debug("Task IDs on this page: {}", tasks.getContent().stream().map(TaskResponse::getId).toList());

        return ResponseEntity.ok(ApiResponse.success(PagedData.from(tasks), "Tasks retrieved successfully"));
    }

    @Operation(summary = "Get overdue tasks")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200", description = "Page of overdue tasks")
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<PagedData<TaskResponse>>> getOverdueTasks(
            @PageableDefault(size = 20, sort = "dueDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Fetching overdue tasks - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<TaskResponse> tasks = taskService.getOverdueTasks(pageable);

        log.info("Overdue tasks retrieved: {} items, total overdue: {}",
            tasks.getNumberOfElements(), tasks.getTotalElements());
        if (tasks.getTotalElements() > 0) {
            log.warn("There are {} overdue tasks requiring attention", tasks.getTotalElements());
        }

        return ResponseEntity.ok(ApiResponse.success(PagedData.from(tasks), "Overdue tasks retrieved successfully"));
    }

    @Operation(summary = "Update a task completely")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Task updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid request body"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Task not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        log.info("Updating task ID: {} with new values - title: '{}', status: {}, priority: {}",
            id, request.getTitle(), request.getStatus(), request.getPriority());
        log.debug("Full update request for task {}: {}", id, request);
        MDC.put("taskId", String.valueOf(id));

        Task task = taskService.updateTask(id, request);

        log.info("Task {} updated successfully", id);
        log.debug("Updated task state: {}", task);

        return ResponseEntity.ok(ApiResponse.success(TaskResponse.fromEntity(task), "Task updated successfully"));
    }

    @Operation(summary = "Update task status")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Task status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid status"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Task not found")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        log.info("Updating status for task ID: {} to {}", id, request.getStatus());
        MDC.put("taskId", String.valueOf(id));

        Task task = taskService.updateTaskStatus(id, request.getStatus());

        log.info("Task {} status updated from {} to {}", id, task.getStatus(), request.getStatus());

        return ResponseEntity.ok(
            ApiResponse.success(TaskResponse.fromEntity(task), "Task status updated successfully"));
    }

    @Operation(summary = "Update status for multiple tasks")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Tasks status updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid request body")
    })
    @PatchMapping("/bulk/status")
    public ResponseEntity<ApiResponse<BulkOperationResult>> updateTasksStatus(
            @Valid @RequestBody BulkStatusUpdateRequest request) {
        log.info("Bulk status update: updating {} tasks to status {}",
            request.getIds().size(), request.getStatus());
        log.debug("Task IDs for bulk status update: {}", request.getIds());

        int count = taskService.updateTasksStatus(request.getIds(), request.getStatus());
        BulkOperationResult result = new BulkOperationResult(count, request.getIds().size());

        log.info("Bulk status update completed: {}/{} tasks updated to {}",
            count, request.getIds().size(), request.getStatus());
        if (count < request.getIds().size()) {
            log.warn("Bulk status update: {} tasks were not found or already deleted",
                request.getIds().size() - count);
        }

        return ResponseEntity.ok(ApiResponse.success(result,
            String.format("%d task(s) status updated successfully", count)));
    }

    @Operation(summary = "Delete a task (soft delete)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Task deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Task not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @Parameter(description = "Task ID") @PathVariable Long id) {
        log.info("Deleting task ID: {} (soft delete)", id);
        MDC.put("taskId", String.valueOf(id));

        taskService.deleteTask(id);

        log.info("Task {} soft deleted successfully", id);

        return ResponseEntity.ok(ApiResponse.deleted("Task deleted successfully"));
    }

    @Operation(summary = "Delete multiple tasks (soft delete)")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Tasks deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid request body")
    })
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkOperationResult>> deleteTasks(
            @Valid @RequestBody BulkDeleteRequest request) {
        log.info("Bulk delete: deleting {} tasks (soft delete)", request.getIds().size());
        log.debug("Task IDs for bulk delete: {}", request.getIds());

        int count = taskService.deleteTasks(request.getIds());
        BulkOperationResult result = new BulkOperationResult(count, request.getIds().size());

        log.info("Bulk delete completed: {}/{} tasks deleted", count, request.getIds().size());
        if (count < request.getIds().size()) {
            log.warn("Bulk delete: {} tasks were not found or already deleted",
                request.getIds().size() - count);
        }

        return ResponseEntity.ok(ApiResponse.success(result,
            String.format("%d task(s) deleted successfully", count)));
    }

    public record BulkOperationResult(int affected, int requested) {}
}
