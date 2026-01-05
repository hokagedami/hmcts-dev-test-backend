package uk.gov.hmcts.reform.dev.models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private ErrorDetails error;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetails {
        private int code;
        private String type;
        private Map<String, String> fieldErrors;
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message("Operation completed successfully")
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message("Resource created successfully")
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static ApiResponse<Void> deleted() {
        return ApiResponse.<Void>builder()
            .success(true)
            .message("Resource deleted successfully")
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static ApiResponse<Void> deleted(String message) {
        return ApiResponse.<Void>builder()
            .success(true)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> error(int code, String type, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .error(ErrorDetails.builder()
                .code(code)
                .type(type)
                .build())
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> error(int code, String type, String message,
                                           Map<String, String> fieldErrors) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .error(ErrorDetails.builder()
                .code(code)
                .type(type)
                .fieldErrors(fieldErrors)
                .build())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
