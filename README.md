# HMCTS Dev Test Backend

A Spring Boot backend application for HMCTS case management system, featuring a comprehensive Task Management API with structured responses.

## Technology Stack

- **Java 24** with Spring Boot 3.5.8
- **Gradle 9.2.1** for build automation
- **Spring Data JPA** with H2 in-memory database
- **Jakarta Bean Validation** for request validation
- **Lombok** for reducing boilerplate
- **OpenAPI/Swagger** for API documentation
- **JUnit 5 + Mockito** for testing

## Getting Started

### Prerequisites

- Java 24 or later
- Gradle 9.2.1 (wrapper included)

### Build and Run

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application starts on **http://localhost:4000**

### API Documentation

- **Swagger UI**: http://localhost:4000/swagger-ui.html
- **OpenAPI JSON**: http://localhost:4000/v3/api-docs

## Task Management API

Base URL: `/api/v1/tasks`

### Structured Response Format

All API responses follow a consistent structure:

**Success Response:**
```json
{
    "success": true,
    "message": "Operation completed successfully",
    "data": { ... },
    "timestamp": "2026-01-05T10:30:00"
}
```

**Error Response:**
```json
{
    "success": false,
    "message": "Error description",
    "error": {
        "code": 400,
        "type": "VALIDATION_ERROR",
        "fieldErrors": { ... }
    },
    "timestamp": "2026-01-05T10:30:00"
}
```

### Task Model

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated unique identifier |
| title | String | Required, task title |
| description | String | Optional, task description |
| status | Enum | PENDING, IN_PROGRESS, COMPLETED, CANCELLED |
| priority | Enum | LOW, MEDIUM (default), HIGH, URGENT |
| dueDateTime | LocalDateTime | Required, must be in the future |
| createdAt | LocalDateTime | Auto-generated |
| updatedAt | LocalDateTime | Auto-updated |
| overdue | Boolean | Computed field, true if past due and not completed |

### Endpoints

#### Create Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/tasks` | Create a single task |
| POST | `/api/v1/tasks/bulk` | Create multiple tasks |

**Create Task Request:**
```json
{
    "title": "Complete documentation",
    "description": "Write API documentation",
    "dueDateTime": "2026-01-15T17:00:00",
    "priority": "HIGH"
}
```

**Create Task Response:**
```json
{
    "success": true,
    "message": "Task created successfully",
    "data": {
        "id": 1,
        "title": "Complete documentation",
        "description": "Write API documentation",
        "status": "PENDING",
        "priority": "HIGH",
        "dueDateTime": "2026-01-15T17:00:00",
        "createdAt": "2026-01-05T10:30:00",
        "updatedAt": "2026-01-05T10:30:00",
        "overdue": false
    },
    "timestamp": "2026-01-05T10:30:00"
}
```

#### Read Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/tasks` | Get all tasks (paginated) |
| GET | `/api/v1/tasks/{id}` | Get task by ID |
| GET | `/api/v1/tasks/overdue` | Get overdue tasks |

**Query Parameters for GET /api/v1/tasks:**

| Parameter | Type | Description |
|-----------|------|-------------|
| status | String | Filter by status (PENDING, IN_PROGRESS, COMPLETED, CANCELLED) |
| priority | String | Filter by priority (LOW, MEDIUM, HIGH, URGENT) |
| search | String | Search in title (case-insensitive) |
| page | Integer | Page number (0-indexed, default: 0) |
| size | Integer | Page size (default: 20) |
| sort | String | Sort field and direction (e.g., `title,asc`) |

**Paginated Response:**
```json
{
    "success": true,
    "message": "Tasks retrieved successfully",
    "data": {
        "items": [...],
        "page": 0,
        "size": 20,
        "totalElements": 50,
        "totalPages": 3,
        "first": true,
        "last": false,
        "hasNext": true,
        "hasPrevious": false
    },
    "timestamp": "2026-01-05T10:30:00"
}
```

#### Update Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/v1/tasks/{id}` | Full update of a task |
| PATCH | `/api/v1/tasks/{id}/status` | Update task status only |
| PATCH | `/api/v1/tasks/bulk/status` | Bulk update status |

**Update Status Request:**
```json
{
    "status": "CANCELLED"
}
```

**Bulk Status Update Request:**
```json
{
    "ids": [1, 2, 3],
    "status": "COMPLETED"
}
```

**Bulk Operation Response:**
```json
{
    "success": true,
    "message": "3 task(s) status updated successfully",
    "data": {
        "affected": 3,
        "requested": 3
    },
    "timestamp": "2026-01-05T10:30:00"
}
```

#### Delete Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| DELETE | `/api/v1/tasks/{id}` | Soft delete a task |
| DELETE | `/api/v1/tasks/bulk` | Bulk soft delete |

**Delete Response:**
```json
{
    "success": true,
    "message": "Task deleted successfully",
    "timestamp": "2026-01-05T10:30:00"
}
```

### Error Types

| Type | HTTP Code | Description |
|------|-----------|-------------|
| NOT_FOUND | 404 | Resource not found |
| VALIDATION_ERROR | 400 | Request validation failed |
| MALFORMED_REQUEST | 400 | Invalid JSON syntax |
| INVALID_ARGUMENT | 400 | Invalid argument value |
| INTERNAL_ERROR | 500 | Unexpected server error |

**Validation Error Response:**
```json
{
    "success": false,
    "message": "Validation failed for one or more fields",
    "error": {
        "code": 400,
        "type": "VALIDATION_ERROR",
        "fieldErrors": {
            "title": "Title is required",
            "dueDateTime": "Due date must be in the future"
        }
    },
    "timestamp": "2026-01-05T10:30:00"
}
```

**Not Found Response:**
```json
{
    "success": false,
    "message": "Task not found with id: 99",
    "error": {
        "code": 404,
        "type": "NOT_FOUND"
    },
    "timestamp": "2026-01-05T10:30:00"
}
```

## Testing

```bash
# Run all tests
./gradlew test

# Run integration tests
./gradlew integrationTest

# Run functional tests
./gradlew functionalTest

# Run all tests together
./gradlew build
```

### Test Coverage

- **Unit Tests**: TaskService business logic
- **Integration Tests**: Controller layer with MockMvc
- **Functional Tests**: End-to-end API testing with REST-Assured

## Project Structure

```
src/
├── main/java/uk/gov/hmcts/reform/dev/
│   ├── controllers/
│   │   └── TaskController.java      # REST endpoints
│   ├── services/
│   │   └── TaskService.java         # Business logic
│   ├── repositories/
│   │   └── TaskRepository.java      # Data access
│   ├── models/
│   │   ├── Task.java                # JPA entity
│   │   ├── TaskStatus.java          # Status enum
│   │   ├── TaskPriority.java        # Priority enum
│   │   └── dto/
│   │       ├── ApiResponse.java     # Structured response wrapper
│   │       ├── PagedData.java       # Pagination wrapper
│   │       └── ...                  # Request/Response DTOs
│   └── exceptions/
│       ├── TaskNotFoundException.java
│       └── GlobalExceptionHandler.java
├── test/                            # Unit tests
├── integrationTest/                 # Integration tests
└── functionalTest/                  # Functional tests
```

## Postman Collection

A complete Postman collection is available at:
```
postman/Task_API_Collection.postman_collection.json
```

Import this into Postman to test all API endpoints.

## Features

- API versioning (`/api/v1`)
- Structured response format for all endpoints
- Task status (PENDING, IN_PROGRESS, COMPLETED, CANCELLED)
- Task priority levels (LOW, MEDIUM, HIGH, URGENT)
- Pagination, sorting, and filtering
- Soft delete (tasks are marked as deleted, not physically removed)
- Bulk operations for create, update, and delete
- Overdue task tracking
- Request validation with detailed error messages
- OpenAPI/Swagger documentation
