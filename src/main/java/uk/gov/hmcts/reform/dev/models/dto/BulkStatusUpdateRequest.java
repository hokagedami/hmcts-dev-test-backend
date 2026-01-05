package uk.gov.hmcts.reform.dev.models.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BulkStatusUpdateRequest {

    @NotEmpty(message = "Task IDs list cannot be empty")
    private List<Long> ids;

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
