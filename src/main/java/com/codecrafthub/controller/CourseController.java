package com.codecrafthub.controller;

import com.codecrafthub.model.Course;
import com.codecrafthub.service.CourseService;
import com.codecrafthub.service.CourseService.CourseNotFoundException;
import com.codecrafthub.service.CourseService.FileOperationException;
import com.codecrafthub.service.CourseService.InvalidStatusException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // POST /api/courses — create a new course
    @PostMapping
    public ResponseEntity<Course> createCourse(@Valid @RequestBody Course course) {
        Course created = courseService.create(course);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/courses — list all courses
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.findAll());
    }

    // GET /api/courses/stats — course statistics
    // Declared before /{id} so Spring does not interpret "stats" as an ID
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCourseStats() {
        return ResponseEntity.ok(courseService.getStats());
    }

    // GET /api/courses/{id} — get single course
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.findById(id));
    }

    // PUT /api/courses/{id} — replace an existing course
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id,
        @Valid @RequestBody Course course) {
        return ResponseEntity.ok(courseService.update(id, course));
    }

    // DELETE /api/courses/{id} — remove a course
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- Error handling

    // 404 when a course ID does not exist
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    // 400 when status value is not in the allowed enum
    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<Map<String, String>> handleInvalidStatus(InvalidStatusException ex) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", ex.getMessage()));
    }

    // 400 for @Valid bean validation failures (missing / blank required fields)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value",
                // keep first message when a field has multiple violations
                (a, b) -> a));
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Validation failed", "details", fieldErrors));
    }

    // 500 for file read/write failures
    @ExceptionHandler(FileOperationException.class)
    public ResponseEntity<Map<String, String>> handleFileError(FileOperationException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Internal server error: " + ex.getMessage()));
    }
}
