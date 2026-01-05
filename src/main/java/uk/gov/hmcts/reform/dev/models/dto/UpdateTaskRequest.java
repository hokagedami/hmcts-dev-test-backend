package uk.gov.hmcts.reform.dev.models.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.dev.models.TaskPriority;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateTaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Due date/time is required")
    @FutureOrPresent(message = "Due date/time must be in the present or future")
    private LocalDateTime dueDateTime;

    @NotNull(message = "Status is required")
    private TaskStatus status;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;
}
