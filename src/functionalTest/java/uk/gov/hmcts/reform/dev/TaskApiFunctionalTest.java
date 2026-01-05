package uk.gov.hmcts.reform.dev;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskApiFunctionalTest {

    private static final String API_BASE = "/api/v1/tasks";

    @LocalServerPort
    private int port;

    private static Long createdTaskId;
    private static Long secondTaskId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/tasks - Should create a new task with priority")
    void createTask_Success() {
        String dueDateTime = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Functional Test Task",
                    "description": "Task created during functional testing",
                    "dueDateTime": "%s",
                    "priority": "HIGH"
                }
                """.formatted(dueDateTime))
            .when()
            .post(API_BASE)
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("message", equalTo("Task created successfully"))
            .body("data.id", notNullValue())
            .body("data.title", equalTo("Functional Test Task"))
            .body("data.description", equalTo("Task created during functional testing"))
            .body("data.status", equalTo("PENDING"))
            .body("data.priority", equalTo("HIGH"))
            .body("data.overdue", equalTo(false))
            .body("data.createdAt", notNullValue())
            .body("data.updatedAt", notNullValue())
            .body("timestamp", notNullValue())
            .extract().response();

        createdTaskId = response.jsonPath().getLong("data.id");
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/v1/tasks/{id} - Should retrieve the created task")
    void getTaskById_Success() {
        given()
            .when()
            .get(API_BASE + "/{id}", createdTaskId)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.id", equalTo(createdTaskId.intValue()))
            .body("data.title", equalTo("Functional Test Task"))
            .body("data.status", equalTo("PENDING"))
            .body("data.priority", equalTo("HIGH"))
            .body("data.overdue", equalTo(false));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/v1/tasks - Should return page containing created task")
    void getAllTasks_Success() {
        given()
            .when()
            .get(API_BASE)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.items", hasSize(greaterThan(0)))
            .body("data.totalElements", greaterThan(0));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/v1/tasks - Should filter by priority")
    void getAllTasks_FilterByPriority() {
        given()
            .queryParam("priority", "HIGH")
            .when()
            .get(API_BASE)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.items[0].priority", equalTo("HIGH"));
    }

    @Test
    @Order(5)
    @DisplayName("PUT /api/v1/tasks/{id} - Should update task fully")
    void updateTask_Success() {
        String dueDateTime = LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Updated Task Title",
                    "description": "Updated description",
                    "dueDateTime": "%s",
                    "status": "IN_PROGRESS",
                    "priority": "URGENT"
                }
                """.formatted(dueDateTime))
            .when()
            .put(API_BASE + "/{id}", createdTaskId)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.title", equalTo("Updated Task Title"))
            .body("data.status", equalTo("IN_PROGRESS"))
            .body("data.priority", equalTo("URGENT"));
    }

    @Test
    @Order(6)
    @DisplayName("PATCH /api/v1/tasks/{id}/status - Should update task status to CANCELLED")
    void updateTaskStatus_ToCancelled() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "status": "CANCELLED"
                }
                """)
            .when()
            .patch(API_BASE + "/{id}/status", createdTaskId)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("CANCELLED"));
    }

    @Test
    @Order(7)
    @DisplayName("PATCH /api/v1/tasks/{id}/status - Should update task status to COMPLETED")
    void updateTaskStatus_ToCompleted() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "status": "COMPLETED"
                }
                """)
            .when()
            .patch(API_BASE + "/{id}/status", createdTaskId)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.status", equalTo("COMPLETED"));
    }

    @Test
    @Order(8)
    @DisplayName("POST /api/v1/tasks/bulk - Should create multiple tasks")
    void createBulkTasks_Success() {
        String dueDateTime1 = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String dueDateTime2 = LocalDateTime.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                [
                    {
                        "title": "Bulk Task 1",
                        "description": "First bulk task",
                        "dueDateTime": "%s",
                        "priority": "LOW"
                    },
                    {
                        "title": "Bulk Task 2",
                        "description": "Second bulk task",
                        "dueDateTime": "%s",
                        "priority": "MEDIUM"
                    }
                ]
                """.formatted(dueDateTime1, dueDateTime2))
            .when()
            .post(API_BASE + "/bulk")
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data", hasSize(2))
            .body("data[0].title", equalTo("Bulk Task 1"))
            .body("data[1].title", equalTo("Bulk Task 2"))
            .extract().response();

        secondTaskId = response.jsonPath().getLong("data[0].id");
    }

    @Test
    @Order(9)
    @DisplayName("PATCH /api/v1/tasks/bulk/status - Should update multiple task statuses")
    void updateBulkStatus_Success() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "ids": [%d, %d],
                    "status": "IN_PROGRESS"
                }
                """.formatted(createdTaskId, secondTaskId))
            .when()
            .patch(API_BASE + "/bulk/status")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.affected", equalTo(2))
            .body("data.requested", equalTo(2));
    }

    @Test
    @Order(10)
    @DisplayName("DELETE /api/v1/tasks/{id} - Should soft delete the task")
    void deleteTask_Success() {
        given()
            .when()
            .delete(API_BASE + "/{id}", createdTaskId)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("message", equalTo("Task deleted successfully"));
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/v1/tasks/{id} - Should return 404 for soft-deleted task")
    void getDeletedTask_NotFound() {
        given()
            .when()
            .get(API_BASE + "/{id}", createdTaskId)
            .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("message", equalTo("Task not found with id: " + createdTaskId))
            .body("error.type", equalTo("NOT_FOUND"));
    }

    @Test
    @Order(12)
    @DisplayName("DELETE /api/v1/tasks/bulk - Should bulk delete tasks")
    void deleteBulkTasks_Success() {
        // Create tasks to delete
        String dueDateTime = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                [
                    {"title": "Delete Me 1", "dueDateTime": "%s"},
                    {"title": "Delete Me 2", "dueDateTime": "%s"}
                ]
                """.formatted(dueDateTime, dueDateTime))
            .when()
            .post(API_BASE + "/bulk")
            .then()
            .statusCode(201)
            .extract().response();

        Long id1 = response.jsonPath().getLong("data[0].id");
        Long id2 = response.jsonPath().getLong("data[1].id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "ids": [%d, %d]
                }
                """.formatted(id1, id2))
            .when()
            .delete(API_BASE + "/bulk")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.affected", equalTo(2))
            .body("data.requested", equalTo(2));
    }

    @Test
    @Order(13)
    @DisplayName("POST /api/v1/tasks - Should return 400 when title is empty")
    void createTask_EmptyTitle_ValidationError() {
        String dueDateTime = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "",
                    "description": "Description",
                    "dueDateTime": "%s"
                }
                """.formatted(dueDateTime))
            .when()
            .post(API_BASE)
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.type", equalTo("VALIDATION_ERROR"))
            .body("error.fieldErrors.title", notNullValue());
    }

    @Test
    @Order(14)
    @DisplayName("POST /api/v1/tasks - Should return 400 when dueDateTime is missing")
    void createTask_MissingDueDateTime_ValidationError() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Test Task",
                    "description": "Description"
                }
                """)
            .when()
            .post(API_BASE)
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("error.type", equalTo("VALIDATION_ERROR"))
            .body("error.fieldErrors.dueDateTime", notNullValue());
    }

    @Test
    @Order(15)
    @DisplayName("POST /api/v1/tasks - Should create task with default MEDIUM priority")
    void createTask_DefaultPriority() {
        String dueDateTime = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Task Without Priority",
                    "dueDateTime": "%s"
                }
                """.formatted(dueDateTime))
            .when()
            .post(API_BASE)
            .then()
            .statusCode(201)
            .body("success", equalTo(true))
            .body("data.title", equalTo("Task Without Priority"))
            .body("data.priority", equalTo("MEDIUM"))
            .extract().response();

        // Clean up
        Long taskId = response.jsonPath().getLong("data.id");
        given().delete(API_BASE + "/{id}", taskId);
    }

    @Test
    @Order(16)
    @DisplayName("GET /api/v1/tasks/{id} - Should return 404 for non-existent task")
    void getTaskById_NonExistent_NotFound() {
        given()
            .when()
            .get(API_BASE + "/{id}", 99999)
            .then()
            .statusCode(404)
            .body("success", equalTo(false))
            .body("message", equalTo("Task not found with id: 99999"))
            .body("error.type", equalTo("NOT_FOUND"));
    }

    @Test
    @Order(17)
    @DisplayName("DELETE /api/v1/tasks/{id} - Should return 404 for non-existent task")
    void deleteTask_NonExistent_NotFound() {
        given()
            .when()
            .delete(API_BASE + "/{id}", 99999)
            .then()
            .statusCode(404)
            .body("success", equalTo(false));
    }

    @Test
    @Order(18)
    @DisplayName("PATCH /api/v1/tasks/{id}/status - Should return 404 for non-existent task")
    void updateStatus_NonExistent_NotFound() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "status": "COMPLETED"
                }
                """)
            .when()
            .patch(API_BASE + "/{id}/status", 99999)
            .then()
            .statusCode(404)
            .body("success", equalTo(false));
    }

    @Test
    @Order(19)
    @DisplayName("GET /api/v1/tasks - Should support pagination")
    void getAllTasks_Pagination() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 5)
            .when()
            .get(API_BASE)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.size", equalTo(5))
            .body("data.page", equalTo(0));
    }

    @Test
    @Order(20)
    @DisplayName("GET /api/v1/tasks - Should search by title")
    void getAllTasks_SearchByTitle() {
        // Create a task with specific title
        String dueDateTime = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Response response = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Searchable Unique Title",
                    "dueDateTime": "%s"
                }
                """.formatted(dueDateTime))
            .when()
            .post(API_BASE)
            .then()
            .statusCode(201)
            .extract().response();

        Long taskId = response.jsonPath().getLong("data.id");

        // Search for it
        given()
            .queryParam("search", "Searchable")
            .when()
            .get(API_BASE)
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("data.items[0].title", equalTo("Searchable Unique Title"));

        // Clean up
        given().delete(API_BASE + "/{id}", taskId);
    }
}
