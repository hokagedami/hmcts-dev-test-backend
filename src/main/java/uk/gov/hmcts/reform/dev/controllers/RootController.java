package uk.gov.hmcts.reform.dev.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.dev.models.dto.ApiResponse;

import java.util.Map;

import static org.springframework.http.ResponseEntity.ok;

@Slf4j
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        log.info("Welcome endpoint accessed");
        return ok("Welcome to test-backend");
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        log.debug("Health check endpoint accessed");
        Map<String, String> healthData = Map.of("status", "UP");
        log.info("Health check: status=UP");
        return ok(ApiResponse.success(healthData, "Service is healthy"));
    }
}
