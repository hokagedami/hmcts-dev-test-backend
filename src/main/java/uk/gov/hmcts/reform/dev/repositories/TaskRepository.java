package uk.gov.hmcts.reform.dev.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dev.models.Task;
import uk.gov.hmcts.reform.dev.models.TaskPriority;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Find by ID excluding soft-deleted
    Optional<Task> findByIdAndDeletedFalse(Long id);

    // Find all excluding soft-deleted
    Page<Task> findByDeletedFalse(Pageable pageable);

    List<Task> findByDeletedFalse();

    // Check existence excluding soft-deleted
    boolean existsByIdAndDeletedFalse(Long id);

    // Filter by status
    Page<Task> findByStatusAndDeletedFalse(TaskStatus status, Pageable pageable);

    // Filter by priority
    Page<Task> findByPriorityAndDeletedFalse(TaskPriority priority, Pageable pageable);

    // Filter by status and priority
    Page<Task> findByStatusAndPriorityAndDeletedFalse(TaskStatus status, TaskPriority priority, Pageable pageable);

    // Search by title (case-insensitive)
    Page<Task> findByTitleContainingIgnoreCaseAndDeletedFalse(String title, Pageable pageable);

    // Find overdue tasks
    @Query("SELECT t FROM Task t WHERE t.deleted = false AND t.status != 'COMPLETED' AND t.dueDateTime < :now")
    Page<Task> findOverdueTasks(@Param("now") LocalDateTime now, Pageable pageable);

    // Find tasks due before a certain date
    Page<Task> findByDueDateTimeBeforeAndDeletedFalse(LocalDateTime dateTime, Pageable pageable);

    // Find tasks due after a certain date
    Page<Task> findByDueDateTimeAfterAndDeletedFalse(LocalDateTime dateTime, Pageable pageable);

    // Complex filter query
    @Query("SELECT t FROM Task t WHERE t.deleted = false "
           + "AND (:status IS NULL OR t.status = :status) "
           + "AND (:priority IS NULL OR t.priority = :priority) "
           + "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Task> findWithFilters(
        @Param("status") TaskStatus status,
        @Param("priority") TaskPriority priority,
        @Param("search") String search,
        Pageable pageable
    );

    // Find all by IDs excluding soft-deleted
    List<Task> findByIdInAndDeletedFalse(List<Long> ids);
}
