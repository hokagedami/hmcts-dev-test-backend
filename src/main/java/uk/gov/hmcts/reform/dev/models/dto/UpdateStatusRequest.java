package uk.gov.hmcts.reform.dev.models.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.dev.models.TaskStatus;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
