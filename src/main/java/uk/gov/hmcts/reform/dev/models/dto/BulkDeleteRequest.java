package uk.gov.hmcts.reform.dev.models.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BulkDeleteRequest {

    @NotEmpty(message = "Task IDs list cannot be empty")
    private List<Long> ids;
}
