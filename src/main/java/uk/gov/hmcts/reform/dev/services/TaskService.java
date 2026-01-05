package uk.gov.hmcts.reform.dev.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.dev.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskPriority;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.models.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.models.dto.TaskResponse;
import uk.gov.hmcts.reform.dev.models.dto.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    @Transactional
    public Task createTask(CreateTaskRequest request) {
        log.debug("Service: Creating task from request - title: '{}', priority: {}, dueDateTime: {}",
            request.getTitle(), request.getPriority(), request.getDueDateTime());

        Task task = createNewTaskObject(request);
        log.trace("Task object created before save: {}", task);

        Task savedTask = taskRepository.save(task);

        log.debug("Service: Task persisted to database with ID: {}", savedTask.getId());
        log.trace("Saved task details - createdAt: {}, updatedAt: {}",
            savedTask.getCreatedAt(), savedTask.getUpdatedAt());

        return savedTask;
    }

    @Transactional(readOnly = true)
    public Task getTaskById(Long id) {
        log.debug("Service: Looking up task by ID: {}", id);

        return taskRepository.findByIdAndDeletedFalse(id)
            .map(task -> {
                log.debug("Service: Task found - ID: {}, title: '{}', status: {}, deleted: {}",
                    task.getId(), task.getTitle(), task.getStatus(), task.isDeleted());
                return task;
            })
            .orElseThrow(() -> {
                log.warn("Service: Task not found with ID: {} (or is deleted)", id);
                return new TaskNotFoundException(id);
            });
    }

    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        log.debug("Service: Fetching all non-deleted tasks");

        List<Task> tasks = taskRepository.findByDeletedFalse();

        log.debug("Service: Retrieved {} non-deleted tasks", tasks.size());
        log.trace("Task IDs retrieved: {}", tasks.stream().map(Task::getId).toList());

        return tasks;
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksWithFilters(
            TaskStatus status,
            TaskPriority priority,
            String search,
            Pageable pageable) {
        log.debug("Service: Fetching tasks with filters - status: {}, priority: {}, search: '{}', pageable: {}",
            status, priority, search, pageable);

        Page<Task> tasks = taskRepository.findWithFilters(status, priority, search, pageable);

        log.debug("Service: Query returned {} tasks (page {}/{}, total: {})",
            tasks.getNumberOfElements(),
            tasks.getNumber() + 1,
            tasks.getTotalPages(),
            tasks.getTotalElements());

        if (tasks.isEmpty()) {
            log.debug("Service: No tasks found matching the filter criteria");
        }

        return tasks.map(TaskResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getOverdueTasks(Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Service: Fetching overdue tasks (dueDateTime before {})", now);

        Page<Task> tasks = taskRepository.findOverdueTasks(now, pageable);

        log.debug("Service: Found {} overdue tasks (total: {})",
            tasks.getNumberOfElements(), tasks.getTotalElements());

        if (tasks.getTotalElements() > 0) {
            log.info("Service: {} overdue tasks detected - earliest due: {}",
                tasks.getTotalElements(),
                tasks.getContent().isEmpty() ? "N/A" : tasks.getContent().get(0).getDueDateTime());
        }

        return tasks.map(TaskResponse::fromEntity);
    }

    @Transactional
    public Task updateTask(Long id, UpdateTaskRequest request) {
        log.debug("Service: Updating task ID: {} with request: {}", id, request);

        Task task = getTaskById(id);
        log.debug("Service: Current task state before update - title: '{}', status: {}, priority: {}",
            task.getTitle(), task.getStatus(), task.getPriority());

        final String oldTitle = task.getTitle();
        final TaskStatus oldStatus = task.getStatus();
        final TaskPriority oldPriority = task.getPriority();

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDateTime(request.getDueDateTime());
        task.setStatus(request.getStatus());
        task.setPriority(request.getPriority());

        Task updatedTask = taskRepository.save(task);

        log.debug("Service: Task {} updated - title: '{}'->'{}', status: {}->{}, priority: {}->{}",
            id, oldTitle, updatedTask.getTitle(),
            oldStatus, updatedTask.getStatus(),
            oldPriority, updatedTask.getPriority());

        return updatedTask;
    }

    @Transactional
    public Task updateTaskStatus(Long id, TaskStatus status) {
        log.debug("Service: Updating status for task ID: {} to {}", id, status);

        Task task = getTaskById(id);
        TaskStatus oldStatus = task.getStatus();

        log.debug("Service: Task {} current status: {}, new status: {}", id, oldStatus, status);

        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);

        log.info("Service: Task {} status changed from {} to {}", id, oldStatus, status);

        return updatedTask;
    }

    @Transactional
    public void deleteTask(Long id) {
        log.debug("Service: Soft deleting task ID: {}", id);

        Task task = taskRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> {
                log.warn("Service: Cannot delete - task not found with ID: {} (or already deleted)", id);
                return new TaskNotFoundException(id);
            });

        log.debug("Service: Task found for deletion - title: '{}', status: {}, createdAt: {}",
            task.getTitle(), task.getStatus(), task.getCreatedAt());

        task.setDeleted(true);
        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);

        log.info("Service: Task {} soft deleted at {}", id, task.getDeletedAt());
    }

    @Transactional
    public List<Task> createTasks(List<CreateTaskRequest> requests) {
        log.debug("Service: Creating {} tasks in bulk", requests.size());
        log.trace("Bulk create request titles: {}",
            requests.stream().map(CreateTaskRequest::getTitle).toList());

        List<Task> tasks = requests.stream().map(this::createNewTaskObject).toList();

        log.debug("Service: Persisting {} task objects to database", tasks.size());

        List<Task> savedTasks = taskRepository.saveAll(tasks);

        log.info("Service: Bulk create completed - {} tasks saved", savedTasks.size());
        log.debug("Service: Created task IDs: {}", savedTasks.stream().map(Task::getId).toList());

        return savedTasks;
    }

    @Transactional
    public int deleteTasks(List<Long> ids) {
        log.debug("Service: Bulk soft delete requested for {} task IDs: {}", ids.size(), ids);

        List<Task> tasks = taskRepository.findByIdInAndDeletedFalse(ids);

        log.debug("Service: Found {} existing non-deleted tasks out of {} requested",
            tasks.size(), ids.size());

        if (tasks.size() < ids.size()) {
            List<Long> foundIds = tasks.stream().map(Task::getId).toList();
            List<Long> notFoundIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            log.warn("Service: {} task IDs not found or already deleted: {}", notFoundIds.size(), notFoundIds);
        }

        LocalDateTime now = LocalDateTime.now();
        tasks.forEach(task -> {
            task.setDeleted(true);
            task.setDeletedAt(now);
            log.trace("Service: Marking task {} as deleted", task.getId());
        });

        taskRepository.saveAll(tasks);

        log.info("Service: Bulk delete completed - {} tasks soft deleted", tasks.size());

        return tasks.size();
    }

    @Transactional
    public int updateTasksStatus(List<Long> ids, TaskStatus status) {
        log.debug("Service: Bulk status update requested for {} task IDs to status {}",
            ids.size(), status);
        log.trace("Service: Task IDs for status update: {}", ids);

        List<Task> tasks = taskRepository.findByIdInAndDeletedFalse(ids);

        log.debug("Service: Found {} existing non-deleted tasks out of {} requested",
            tasks.size(), ids.size());

        if (tasks.size() < ids.size()) {
            List<Long> foundIds = tasks.stream().map(Task::getId).toList();
            List<Long> notFoundIds = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            log.warn("Service: {} task IDs not found or already deleted: {}", notFoundIds.size(), notFoundIds);
        }

        tasks.forEach(task -> {
            TaskStatus oldStatus = task.getStatus();
            task.setStatus(status);
            log.trace("Service: Task {} status changing from {} to {}", task.getId(), oldStatus, status);
        });

        taskRepository.saveAll(tasks);

        log.info("Service: Bulk status update completed - {} tasks updated to {}", tasks.size(), status);

        return tasks.size();
    }

    private Task createNewTaskObject(CreateTaskRequest request) {
        log.trace("Service: Building new Task object from request");

        Task task = new Task();
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDateTime(request.getDueDateTime());
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);

        log.trace("Service: Task object built - title: '{}', status: {}, priority: {}",
            task.getTitle(), task.getStatus(), task.getPriority());

        return task;
    }
}
