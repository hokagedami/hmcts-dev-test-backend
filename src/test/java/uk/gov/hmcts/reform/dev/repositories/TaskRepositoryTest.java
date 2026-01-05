package uk.gov.hmcts.reform.dev.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskPriority;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    private Task pendingTask;
    private Task inProgressTask;
    private Task completedTask;
    private Task deletedTask;
    private Task overdueTask;
    private Task highPriorityTask;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        taskRepository.deleteAll();
        entityManager.flush();

        // Create test tasks
        pendingTask = createTask("Pending Task", "Description 1", TaskStatus.PENDING,
            TaskPriority.MEDIUM, LocalDateTime.now().plusDays(5), false);

        inProgressTask = createTask("In Progress Task", "Description 2", TaskStatus.IN_PROGRESS,
            TaskPriority.LOW, LocalDateTime.now().plusDays(3), false);

        completedTask = createTask("Completed Task", "Description 3", TaskStatus.COMPLETED,
            TaskPriority.HIGH, LocalDateTime.now().plusDays(1), false);

        deletedTask = createTask("Deleted Task", "Description 4", TaskStatus.PENDING,
            TaskPriority.MEDIUM, LocalDateTime.now().plusDays(2), true);

        overdueTask = createTask("Overdue Task", "Description 5", TaskStatus.PENDING,
            TaskPriority.HIGH, LocalDateTime.now().minusDays(2), false);

        highPriorityTask = createTask("High Priority Task", "Description 6", TaskStatus.IN_PROGRESS,
            TaskPriority.HIGH, LocalDateTime.now().plusDays(1), false);

        entityManager.flush();
    }

    private Task createTask(String title, String description, TaskStatus status,
                            TaskPriority priority, LocalDateTime dueDateTime, boolean deleted) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setPriority(priority);
        task.setDueDateTime(dueDateTime);
        task.setDeleted(deleted);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return entityManager.persist(task);
    }

    @Nested
    @DisplayName("findByIdAndDeletedFalse")
    class FindByIdAndDeletedFalse {

        @Test
        @DisplayName("Should find task by ID when not deleted")
        void shouldFindTaskById() {
            Optional<Task> result = taskRepository.findByIdAndDeletedFalse(pendingTask.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Pending Task");
        }

        @Test
        @DisplayName("Should return empty when task is deleted")
        void shouldReturnEmptyWhenDeleted() {
            Optional<Task> result = taskRepository.findByIdAndDeletedFalse(deletedTask.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when task does not exist")
        void shouldReturnEmptyWhenNotExists() {
            Optional<Task> result = taskRepository.findByIdAndDeletedFalse(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDeletedFalse")
    class FindByDeletedFalse {

        @Test
        @DisplayName("Should find all non-deleted tasks with pagination")
        void shouldFindAllNonDeletedTasksWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByDeletedFalse(pageable);

            assertThat(result.getContent()).hasSize(5);
            assertThat(result.getContent())
                .extracting(Task::getTitle)
                .doesNotContain("Deleted Task");
        }

        @Test
        @DisplayName("Should return correct page size")
        void shouldReturnCorrectPageSize() {
            Pageable pageable = PageRequest.of(0, 2);

            Page<Task> result = taskRepository.findByDeletedFalse(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should find all non-deleted tasks as list")
        void shouldFindAllNonDeletedTasksAsList() {
            List<Task> result = taskRepository.findByDeletedFalse();

            assertThat(result).hasSize(5);
            assertThat(result)
                .extracting(Task::getTitle)
                .doesNotContain("Deleted Task");
        }
    }

    @Nested
    @DisplayName("existsByIdAndDeletedFalse")
    class ExistsByIdAndDeletedFalse {

        @Test
        @DisplayName("Should return true when task exists and not deleted")
        void shouldReturnTrueWhenExists() {
            boolean result = taskRepository.existsByIdAndDeletedFalse(pendingTask.getId());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when task is deleted")
        void shouldReturnFalseWhenDeleted() {
            boolean result = taskRepository.existsByIdAndDeletedFalse(deletedTask.getId());

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when task does not exist")
        void shouldReturnFalseWhenNotExists() {
            boolean result = taskRepository.existsByIdAndDeletedFalse(999L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("findByStatusAndDeletedFalse")
    class FindByStatusAndDeletedFalse {

        @Test
        @DisplayName("Should find tasks by PENDING status")
        void shouldFindPendingTasks() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByStatusAndDeletedFalse(TaskStatus.PENDING, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                .extracting(Task::getStatus)
                .containsOnly(TaskStatus.PENDING);
        }

        @Test
        @DisplayName("Should find tasks by IN_PROGRESS status")
        void shouldFindInProgressTasks() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByStatusAndDeletedFalse(TaskStatus.IN_PROGRESS, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                .extracting(Task::getStatus)
                .containsOnly(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should find tasks by COMPLETED status")
        void shouldFindCompletedTasks() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByStatusAndDeletedFalse(TaskStatus.COMPLETED, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Completed Task");
        }

        @Test
        @DisplayName("Should not include deleted tasks when filtering by status")
        void shouldNotIncludeDeletedTasks() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByStatusAndDeletedFalse(TaskStatus.PENDING, pageable);

            assertThat(result.getContent())
                .extracting(Task::getTitle)
                .doesNotContain("Deleted Task");
        }
    }

    @Nested
    @DisplayName("findByPriorityAndDeletedFalse")
    class FindByPriorityAndDeletedFalse {

        @Test
        @DisplayName("Should find tasks by HIGH priority")
        void shouldFindHighPriorityTasks() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByPriorityAndDeletedFalse(TaskPriority.HIGH, pageable);

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent())
                .extracting(Task::getPriority)
                .containsOnly(TaskPriority.HIGH);
        }

        @Test
        @DisplayName("Should find tasks by MEDIUM priority")
        void shouldFindMediumPriorityTasks() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByPriorityAndDeletedFalse(TaskPriority.MEDIUM, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Pending Task");
        }

        @Test
        @DisplayName("Should find tasks by LOW priority")
        void shouldFindLowPriorityTasks() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByPriorityAndDeletedFalse(TaskPriority.LOW, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("In Progress Task");
        }
    }

    @Nested
    @DisplayName("findByStatusAndPriorityAndDeletedFalse")
    class FindByStatusAndPriorityAndDeletedFalse {

        @Test
        @DisplayName("Should find tasks by status and priority")
        void shouldFindByStatusAndPriority() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByStatusAndPriorityAndDeletedFalse(
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("High Priority Task");
        }

        @Test
        @DisplayName("Should return empty when no match for status and priority")
        void shouldReturnEmptyWhenNoMatch() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByStatusAndPriorityAndDeletedFalse(
                TaskStatus.COMPLETED, TaskPriority.LOW, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByTitleContainingIgnoreCaseAndDeletedFalse")
    class FindByTitleContainingIgnoreCaseAndDeletedFalse {

        @Test
        @DisplayName("Should find tasks by title substring case-insensitive")
        void shouldFindByTitleSubstring() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByTitleContainingIgnoreCaseAndDeletedFalse(
                "progress", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("In Progress Task");
        }

        @Test
        @DisplayName("Should find tasks with uppercase search")
        void shouldFindWithUppercaseSearch() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByTitleContainingIgnoreCaseAndDeletedFalse(
                "PENDING", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Pending Task");
        }

        @Test
        @DisplayName("Should find multiple tasks matching search term")
        void shouldFindMultipleMatches() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByTitleContainingIgnoreCaseAndDeletedFalse(
                "Task", pageable);

            assertThat(result.getContent()).hasSize(5);
        }

        @Test
        @DisplayName("Should not include deleted tasks in search results")
        void shouldNotIncludeDeletedInSearch() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findByTitleContainingIgnoreCaseAndDeletedFalse(
                "Deleted", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findOverdueTasks")
    class FindOverdueTasks {

        @Test
        @DisplayName("Should find overdue tasks")
        void shouldFindOverdueTasks() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findOverdueTasks(LocalDateTime.now(), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Overdue Task");
        }

        @Test
        @DisplayName("Should not include completed tasks as overdue")
        void shouldNotIncludeCompletedTasks() {
            // Create a completed task that is past due
            Task completedOverdue = createTask("Completed Overdue", "Desc",
                TaskStatus.COMPLETED, TaskPriority.HIGH, LocalDateTime.now().minusDays(5), false);
            entityManager.flush();

            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> result = taskRepository.findOverdueTasks(LocalDateTime.now(), pageable);

            assertThat(result.getContent())
                .extracting(Task::getTitle)
                .doesNotContain("Completed Overdue");
        }

        @Test
        @DisplayName("Should not include deleted tasks as overdue")
        void shouldNotIncludeDeletedTasks() {
            // Create a deleted overdue task
            Task deletedOverdue = createTask("Deleted Overdue", "Desc",
                TaskStatus.PENDING, TaskPriority.HIGH, LocalDateTime.now().minusDays(5), true);
            entityManager.flush();

            Pageable pageable = PageRequest.of(0, 10);
            Page<Task> result = taskRepository.findOverdueTasks(LocalDateTime.now(), pageable);

            assertThat(result.getContent())
                .extracting(Task::getTitle)
                .doesNotContain("Deleted Overdue");
        }
    }

    @Nested
    @DisplayName("findByDueDateTimeBeforeAndDeletedFalse")
    class FindByDueDateTimeBeforeAndDeletedFalse {

        @Test
        @DisplayName("Should find tasks due before given date")
        void shouldFindTasksDueBefore() {
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime cutoff = LocalDateTime.now().plusDays(2);

            Page<Task> result = taskRepository.findByDueDateTimeBeforeAndDeletedFalse(cutoff, pageable);

            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent())
                .allMatch(task -> task.getDueDateTime().isBefore(cutoff));
        }

        @Test
        @DisplayName("Should not include deleted tasks")
        void shouldNotIncludeDeleted() {
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime cutoff = LocalDateTime.now().plusDays(10);

            Page<Task> result = taskRepository.findByDueDateTimeBeforeAndDeletedFalse(cutoff, pageable);

            assertThat(result.getContent())
                .extracting(Task::getTitle)
                .doesNotContain("Deleted Task");
        }
    }

    @Nested
    @DisplayName("findByDueDateTimeAfterAndDeletedFalse")
    class FindByDueDateTimeAfterAndDeletedFalse {

        @Test
        @DisplayName("Should find tasks due after given date")
        void shouldFindTasksDueAfter() {
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime cutoff = LocalDateTime.now().plusDays(2);

            Page<Task> result = taskRepository.findByDueDateTimeAfterAndDeletedFalse(cutoff, pageable);

            assertThat(result.getContent()).isNotEmpty();
            assertThat(result.getContent())
                .allMatch(task -> task.getDueDateTime().isAfter(cutoff));
        }

        @Test
        @DisplayName("Should return empty when no tasks due after date")
        void shouldReturnEmptyWhenNoTasksAfter() {
            Pageable pageable = PageRequest.of(0, 10);
            LocalDateTime cutoff = LocalDateTime.now().plusDays(100);

            Page<Task> result = taskRepository.findByDueDateTimeAfterAndDeletedFalse(cutoff, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findWithFilters")
    class FindWithFilters {

        @Test
        @DisplayName("Should find all non-deleted tasks when no filters applied")
        void shouldFindAllWhenNoFilters() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findWithFilters(null, null, null, pageable);

            assertThat(result.getContent()).hasSize(5);
        }

        @Test
        @DisplayName("Should filter by status only")
        void shouldFilterByStatusOnly() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findWithFilters(TaskStatus.PENDING, null, null, pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                .extracting(Task::getStatus)
                .containsOnly(TaskStatus.PENDING);
        }

        @Test
        @DisplayName("Should filter by priority only")
        void shouldFilterByPriorityOnly() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findWithFilters(null, TaskPriority.HIGH, null, pageable);

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent())
                .extracting(Task::getPriority)
                .containsOnly(TaskPriority.HIGH);
        }

        @Test
        @DisplayName("Should filter by search term only")
        void shouldFilterBySearchOnly() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findWithFilters(null, null, "Overdue", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Overdue Task");
        }

        @Test
        @DisplayName("Should filter by status and priority")
        void shouldFilterByStatusAndPriority() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findWithFilters(
                TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("High Priority Task");
        }

        @Test
        @DisplayName("Should filter by status, priority, and search")
        void shouldFilterByAllCriteria() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findWithFilters(
                TaskStatus.PENDING, TaskPriority.HIGH, "Overdue", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Overdue Task");
        }

        @Test
        @DisplayName("Should return empty when no match")
        void shouldReturnEmptyWhenNoMatch() {
            Pageable pageable = PageRequest.of(0, 10);

            Page<Task> result = taskRepository.findWithFilters(
                TaskStatus.COMPLETED, TaskPriority.LOW, "NonExistent", pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should support pagination with filters")
        void shouldSupportPaginationWithFilters() {
            Pageable pageable = PageRequest.of(0, 1);

            Page<Task> result = taskRepository.findWithFilters(null, TaskPriority.HIGH, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should support sorting with filters")
        void shouldSupportSortingWithFilters() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "title"));

            Page<Task> result = taskRepository.findWithFilters(null, null, null, pageable);

            List<String> titles = result.getContent().stream()
                .map(Task::getTitle)
                .toList();
            assertThat(titles).isSorted();
        }
    }

    @Nested
    @DisplayName("findByIdInAndDeletedFalse")
    class FindByIdInAndDeletedFalse {

        @Test
        @DisplayName("Should find tasks by list of IDs")
        void shouldFindTasksByIds() {
            List<Long> ids = List.of(pendingTask.getId(), inProgressTask.getId());

            List<Task> result = taskRepository.findByIdInAndDeletedFalse(ids);

            assertThat(result).hasSize(2);
            assertThat(result)
                .extracting(Task::getId)
                .containsExactlyInAnyOrder(pendingTask.getId(), inProgressTask.getId());
        }

        @Test
        @DisplayName("Should not include deleted tasks")
        void shouldNotIncludeDeletedTasks() {
            List<Long> ids = List.of(pendingTask.getId(), deletedTask.getId());

            List<Task> result = taskRepository.findByIdInAndDeletedFalse(ids);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(pendingTask.getId());
        }

        @Test
        @DisplayName("Should return empty for non-existent IDs")
        void shouldReturnEmptyForNonExistentIds() {
            List<Long> ids = List.of(998L, 999L);

            List<Task> result = taskRepository.findByIdInAndDeletedFalse(ids);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty for empty ID list")
        void shouldReturnEmptyForEmptyIdList() {
            List<Long> ids = List.of();

            List<Task> result = taskRepository.findByIdInAndDeletedFalse(ids);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find all valid IDs from mixed list")
        void shouldFindAllValidIdsFromMixedList() {
            List<Long> ids = List.of(
                pendingTask.getId(),
                deletedTask.getId(),
                999L,
                completedTask.getId()
            );

            List<Task> result = taskRepository.findByIdInAndDeletedFalse(ids);

            assertThat(result).hasSize(2);
            assertThat(result)
                .extracting(Task::getId)
                .containsExactlyInAnyOrder(pendingTask.getId(), completedTask.getId());
        }
    }
}
