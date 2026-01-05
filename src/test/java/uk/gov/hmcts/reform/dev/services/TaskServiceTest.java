package uk.gov.hmcts.reform.dev.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.dev.exceptions.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskPriority;
import uk.gov.hmcts.reform.dev.models.TaskStatus;
import uk.gov.hmcts.reform.dev.models.dto.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.models.dto.TaskResponse;
import uk.gov.hmcts.reform.dev.models.dto.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.repositories.TaskRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private CreateTaskRequest createTaskRequest;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setDescription("Test Description");
        task.setStatus(TaskStatus.PENDING);
        task.setPriority(TaskPriority.MEDIUM);
        task.setDueDateTime(LocalDateTime.now().plusDays(1));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setDeleted(false);

        createTaskRequest = new CreateTaskRequest();
        createTaskRequest.setTitle("Test Task");
        createTaskRequest.setDescription("Test Description");
        createTaskRequest.setDueDateTime(LocalDateTime.now().plusDays(1));
        createTaskRequest.setPriority(TaskPriority.MEDIUM);
    }

    @Test
    @DisplayName("Should create a task successfully")
    void createTask_Success() {
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.createTask(createTaskRequest);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Task");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(result.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should create task with default MEDIUM priority when not specified")
    void createTask_DefaultPriority() {
        createTaskRequest.setPriority(null);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            assertThat(savedTask.getPriority()).isEqualTo(TaskPriority.MEDIUM);
            return task;
        });

        taskService.createTask(createTaskRequest);

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should get task by ID successfully")
    void getTaskById_Success() {
        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(taskRepository).findByIdAndDeletedFalse(1L);
    }

    @Test
    @DisplayName("Should throw exception when task not found by ID")
    void getTaskById_NotFound() {
        when(taskRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(999L))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessageContaining("Task not found with id: 999");
    }

    @Test
    @DisplayName("Should get all non-deleted tasks successfully")
    void getAllTasks_Success() {
        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");
        task2.setStatus(TaskStatus.IN_PROGRESS);
        task2.setPriority(TaskPriority.HIGH);

        when(taskRepository.findByDeletedFalse()).thenReturn(Arrays.asList(task, task2));

        List<Task> result = taskService.getAllTasks();

        assertThat(result).hasSize(2);
        verify(taskRepository).findByDeletedFalse();
    }

    @Test
    @DisplayName("Should return empty list when no tasks exist")
    void getAllTasks_EmptyList() {
        when(taskRepository.findByDeletedFalse()).thenReturn(List.of());

        List<Task> result = taskService.getAllTasks();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should get tasks with filters and pagination")
    void getTasksWithFilters_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(task));

        when(taskRepository.findWithFilters(
            eq(TaskStatus.PENDING),
            eq(TaskPriority.MEDIUM),
            eq("test"),
            any(Pageable.class)
        )).thenReturn(taskPage);

        Page<TaskResponse> result = taskService.getTasksWithFilters(
            TaskStatus.PENDING,
            TaskPriority.MEDIUM,
            "test",
            pageable
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("Should get overdue tasks")
    void getOverdueTasks_Success() {
        Task overdueTask = new Task();
        overdueTask.setId(2L);
        overdueTask.setTitle("Overdue Task");
        overdueTask.setStatus(TaskStatus.PENDING);
        overdueTask.setDueDateTime(LocalDateTime.now().minusDays(1));
        overdueTask.setCreatedAt(LocalDateTime.now());
        overdueTask.setUpdatedAt(LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(overdueTask));

        when(taskRepository.findOverdueTasks(any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(taskPage);

        Page<TaskResponse> result = taskService.getOverdueTasks(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Overdue Task");
    }

    @Test
    @DisplayName("Should update task status successfully")
    void updateTaskStatus_Success() {
        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        Task result = taskService.updateTaskStatus(1L, TaskStatus.COMPLETED);

        assertThat(result).isNotNull();
        verify(taskRepository).findByIdAndDeletedFalse(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when updating status of non-existent task")
    void updateTaskStatus_NotFound() {
        when(taskRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTaskStatus(999L, TaskStatus.COMPLETED))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessageContaining("Task not found with id: 999");
    }

    @Test
    @DisplayName("Should update task fully")
    void updateTask_Success() {
        UpdateTaskRequest updateRequest = new UpdateTaskRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");
        updateRequest.setDueDateTime(LocalDateTime.now().plusDays(5));
        updateRequest.setStatus(TaskStatus.IN_PROGRESS);
        updateRequest.setPriority(TaskPriority.HIGH);

        Task updatedTask = new Task();
        updatedTask.setId(1L);
        updatedTask.setTitle("Updated Title");
        updatedTask.setDescription("Updated Description");
        updatedTask.setStatus(TaskStatus.IN_PROGRESS);
        updatedTask.setPriority(TaskPriority.HIGH);

        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        Task result = taskService.updateTask(1L, updateRequest);

        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(result.getPriority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    @DisplayName("Should soft delete task successfully")
    void deleteTask_Success() {
        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        taskService.deleteTask(1L);

        verify(taskRepository).findByIdAndDeletedFalse(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent task")
    void deleteTask_NotFound() {
        when(taskRepository.findByIdAndDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(999L))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessageContaining("Task not found with id: 999");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("Should create multiple tasks successfully")
    void createTasks_Success() {
        Task task1 = new Task();
        task1.setId(1L);
        task1.setTitle("Task 1");
        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");

        when(taskRepository.saveAll(anyList())).thenReturn(List.of(task1, task2));

        final CreateTaskRequest request1 = new CreateTaskRequest("Task 1", "Desc 1",
            LocalDateTime.now().plusDays(1), TaskPriority.LOW);
        final CreateTaskRequest request2 = new CreateTaskRequest("Task 2", "Desc 2",
            LocalDateTime.now().plusDays(2), TaskPriority.HIGH);
        List<Task> result = taskService.createTasks(List.of(request1, request2));

        assertThat(result).hasSize(2);
        verify(taskRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should bulk delete tasks successfully")
    void deleteTasks_Success() {
        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");
        task2.setDeleted(false);

        List<Long> ids = List.of(1L, 2L);
        when(taskRepository.findByIdInAndDeletedFalse(ids)).thenReturn(List.of(task, task2));
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(task, task2));

        int count = taskService.deleteTasks(ids);

        assertThat(count).isEqualTo(2);
        verify(taskRepository).findByIdInAndDeletedFalse(ids);
        verify(taskRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should bulk update task statuses successfully")
    void updateTasksStatus_Success() {
        Task task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Task 2");
        task2.setStatus(TaskStatus.PENDING);

        List<Long> ids = List.of(1L, 2L);
        when(taskRepository.findByIdInAndDeletedFalse(ids)).thenReturn(List.of(task, task2));
        when(taskRepository.saveAll(anyList())).thenReturn(List.of(task, task2));

        int count = taskService.updateTasksStatus(ids, TaskStatus.COMPLETED);

        assertThat(count).isEqualTo(2);
        verify(taskRepository).findByIdInAndDeletedFalse(ids);
        verify(taskRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should create task without description")
    void createTask_WithoutDescription() {
        createTaskRequest.setDescription(null);
        Task taskWithoutDesc = new Task();
        taskWithoutDesc.setId(1L);
        taskWithoutDesc.setTitle("Test Task");
        taskWithoutDesc.setDescription(null);
        taskWithoutDesc.setStatus(TaskStatus.PENDING);
        taskWithoutDesc.setPriority(TaskPriority.MEDIUM);
        taskWithoutDesc.setDueDateTime(LocalDateTime.now().plusDays(1));

        when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutDesc);

        Task result = taskService.createTask(createTaskRequest);

        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should update task status from PENDING to IN_PROGRESS")
    void updateTaskStatus_PendingToInProgress() {
        task.setStatus(TaskStatus.PENDING);
        Task updatedTask = new Task();
        updatedTask.setId(1L);
        updatedTask.setTitle("Test Task");
        updatedTask.setStatus(TaskStatus.IN_PROGRESS);

        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        Task result = taskService.updateTaskStatus(1L, TaskStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should update task status from IN_PROGRESS to COMPLETED")
    void updateTaskStatus_InProgressToCompleted() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        Task updatedTask = new Task();
        updatedTask.setId(1L);
        updatedTask.setTitle("Test Task");
        updatedTask.setStatus(TaskStatus.COMPLETED);

        when(taskRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(updatedTask);

        Task result = taskService.updateTaskStatus(1L, TaskStatus.COMPLETED);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should set default status as PENDING when creating task")
    void createTask_DefaultStatusIsPending() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            assertThat(savedTask.getStatus()).isEqualTo(TaskStatus.PENDING);
            return task;
        });

        taskService.createTask(createTaskRequest);

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should verify task properties are set correctly on creation")
    void createTask_VerifyAllProperties() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            assertThat(savedTask.getTitle()).isEqualTo(createTaskRequest.getTitle());
            assertThat(savedTask.getDescription()).isEqualTo(createTaskRequest.getDescription());
            assertThat(savedTask.getDueDateTime()).isEqualTo(createTaskRequest.getDueDateTime());
            assertThat(savedTask.getPriority()).isEqualTo(createTaskRequest.getPriority());
            return task;
        });

        taskService.createTask(createTaskRequest);

        verify(taskRepository).save(any(Task.class));
    }

    @Test
    @DisplayName("Should return 0 when bulk deleting empty list")
    void deleteTasks_EmptyList() {
        List<Long> emptyIds = List.of();
        when(taskRepository.findByIdInAndDeletedFalse(emptyIds)).thenReturn(List.of());
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        int count = taskService.deleteTasks(emptyIds);

        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return 0 when bulk updating empty list")
    void updateTasksStatus_EmptyList() {
        List<Long> emptyIds = List.of();
        when(taskRepository.findByIdInAndDeletedFalse(emptyIds)).thenReturn(List.of());
        when(taskRepository.saveAll(anyList())).thenReturn(List.of());

        int count = taskService.updateTasksStatus(emptyIds, TaskStatus.COMPLETED);

        assertThat(count).isEqualTo(0);
    }
}
