# CodeCraftHub

A lightweight REST API for managing online courses, built with Spring Boot 3.x and Java 17. All data is persisted
locally in a JSON file — no database required.

---

## Features

- Full CRUD for courses (create, read, update, delete)
- Course statistics endpoint (totals and breakdown by status)
- File-based persistence via `courses.json` (auto-created on first run)
- Input validation with descriptive error messages
- ISO-8601 date serialisation

---

## Tech Stack

| Layer         | Technology                            |
|---------------|---------------------------------------|
| Language      | Java 17                               |
| Framework     | Spring Boot 3.2.x (Spring Web)        |
| Serialisation | Jackson (via spring-boot-starter-web) |
| Validation    | Jakarta Bean Validation (JSR-380)     |
| Build tool    | Maven                                 |

---

## Prerequisites

- Java 17 or later (`java -version`)
- Maven 3.8 or later (`mvn -version`)

---

## Installation

```bash
git clone <repository-url>
cd codecrafthub
mvn install -DskipTests
```

---

## Running the Application

**Development (Maven plugin)**

```bash
mvn spring-boot:run
```

**Production (JAR)**

```bash
mvn package -DskipTests
java -jar target/codecrafthub-0.0.1-SNAPSHOT.jar
```

The server starts on **http://localhost:8080** by default.

To change the port, edit `src/main/resources/application.properties`:

```properties
server.port=9090
```

---

## Data Model

| JSON field    | Java type       | Notes                                                         |
|---------------|-----------------|---------------------------------------------------------------|
| `id`          | `Long`          | Auto-assigned on creation, read-only                          |
| `name`        | `String`        | Required                                                      |
| `description` | `String`        | Required                                                      |
| `target_date` | `LocalDate`     | Required, format `yyyy-MM-dd`                                 |
| `status`      | `String`        | Required, one of: `Not Started` · `In Progress` · `Completed` |
| `created_at`  | `LocalDateTime` | Auto-set on creation, ISO-8601, read-only                     |

---

## API Reference

### Base URL

```
http://localhost:8080/api/courses
```

---

### Create a course

```
POST /api/courses
```

**Request body**

```json
{
  "name": "Spring Boot Fundamentals",
  "description": "Learn REST API development with Spring Boot 3.x",
  "target_date": "2026-09-01",
  "status": "Not Started"
}
```

**Response — 201 Created**

```json
{
  "id": 1,
  "name": "Spring Boot Fundamentals",
  "description": "Learn REST API development with Spring Boot 3.x",
  "target_date": "2026-09-01",
  "status": "Not Started",
  "created_at": "2026-04-29T10:00:00"
}
```

```bash
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -d '{"name":"Spring Boot Fundamentals","description":"Learn REST API development with Spring Boot 3.x","target_date":"2026-09-01","status":"Not Started"}'
```

---

### List all courses

```
GET /api/courses
```

**Response — 200 OK**

```json
[
  {
    "id": 1,
    "name": "Spring Boot Fundamentals",
    "description": "Learn REST API development with Spring Boot 3.x",
    "target_date": "2026-09-01",
    "status": "Not Started",
    "created_at": "2026-04-29T10:00:00"
  }
]
```

```bash
curl http://localhost:8080/api/courses
```

---

### Get a course by ID

```
GET /api/courses/{id}
```

**Response — 200 OK**

```json
{
  "id": 1,
  "name": "Spring Boot Fundamentals",
  "description": "Learn REST API development with Spring Boot 3.x",
  "target_date": "2026-09-01",
  "status": "Not Started",
  "created_at": "2026-04-29T10:00:00"
}
```

```bash
curl http://localhost:8080/api/courses/1
```

---

### Update a course

```
PUT /api/courses/{id}
```

All fields are replaced. Provide the full course body.

**Request body**

```json
{
  "name": "Spring Boot Fundamentals",
  "description": "Learn REST API development with Spring Boot 3.x",
  "target_date": "2026-09-01",
  "status": "In Progress"
}
```

**Response — 200 OK** — returns the updated course.

```bash
curl -X PUT http://localhost:8080/api/courses/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Spring Boot Fundamentals","description":"Learn REST API development with Spring Boot 3.x","target_date":"2026-09-01","status":"In Progress"}'
```

---

### Delete a course

```
DELETE /api/courses/{id}
```

**Response — 204 No Content**

```bash
curl -X DELETE http://localhost:8080/api/courses/1
```

---

### Course statistics

```
GET /api/courses/stats
```

**Response — 200 OK**

```json
{
  "total": 3,
  "by_status": {
    "Not Started": 1,
    "In Progress": 1,
    "Completed": 1
  }
}
```

```bash
curl http://localhost:8080/api/courses/stats
```

---

## Error Responses

All errors return JSON with an `error` field (and optionally `details` for validation failures).

| HTTP Status                 | Trigger                                                 |
|-----------------------------|---------------------------------------------------------|
| `400 Bad Request`           | Missing/blank required field, or invalid `status` value |
| `404 Not Found`             | Course ID does not exist                                |
| `500 Internal Server Error` | `courses.json` cannot be read or written                |

**Validation error example**

```json
{
  "error": "Validation failed",
  "details": {
    "name": "name is required",
    "status": "status must be one of: Not Started, In Progress, Completed"
  }
}
```

**Not found example**

```json
{
  "error": "Course not found with id: 99"
}
```

---

## Project Structure

```
codecrafthub/
├── pom.xml
├── courses.json                          ← auto-created on first run
└── src/main/
    ├── java/com/codecrafthub/
    │   ├── CodeCraftHubApplication.java  ← entry point
    │   ├── model/
    │   │   └── Course.java               ← data model + Jackson annotations
    │   ├── service/
    │   │   └── CourseService.java        ← business logic + file I/O
    │   └── controller/
    │       └── CourseController.java     ← REST endpoints + error handlers
    └── resources/
        └── application.properties
```

---

## Troubleshooting

**Port 8080 is already in use**

```
Web server failed to start. Port 8080 was already in use.
```

Change the port in `application.properties` (`server.port=9090`) or stop the process that holds 8080:

```bash
# macOS / Linux
lsof -i :8080
kill -9 <PID>
```

---

**`courses.json` permission denied**

```
Internal server error: Failed to write courses.json
```

The application writes `courses.json` in its working directory. Make sure the process has write permission there, or run
from a directory where you have write access:

```bash
cd /tmp && java -jar /path/to/codecrafthub-0.0.1-SNAPSHOT.jar
```

---

**`courses.json` contains invalid JSON**

```
Internal server error: Failed to read courses.json
```

The file may have been corrupted by a manual edit. Either fix the JSON or delete the file — the application will create
a fresh empty one on the next write.

---

**`java.lang.UnsupportedClassVersionError`**

```
UnsupportedClassVersionError: com/codecrafthub/CodeCraftHubApplication
```

The JAR was compiled with Java 17 but is being run with an older JVM. Verify your runtime:

```bash
java -version   # must be 17 or later
```

---

**`mvn` command not found**
Install Maven from https://maven.apache.org/download.cgi or via a package manager:

```bash
# macOS
brew install maven

# Ubuntu / Debian
sudo apt install maven
```
