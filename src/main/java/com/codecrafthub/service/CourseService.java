package com.codecrafthub.service;

import com.codecrafthub.model.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CourseService {

    private static final String FILE_PATH = "courses.json";
    private static final Set<String> VALID_STATUSES = Set.of("Not Started", "In Progress", "Completed");

    private final ObjectMapper objectMapper;

    // Tracks the highest ID so far to generate the next one without gaps
    private final AtomicLong idCounter = new AtomicLong(0);

    public CourseService() {
        this.objectMapper = new ObjectMapper();
        // Required to serialise/deserialise LocalDate and LocalDateTime
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.findAndRegisterModules();

        // Initialise counter from existing data so IDs never repeat after restart
        List<Course> existing = readAll();
        existing.stream()
            .mapToLong(Course::getId)
            .max()
            .ifPresent(idCounter::set);
    }

    // ------------------------------------------------------------------ CRUD

    public Course create(Course course) {
        validateStatus(course.getStatus());

        List<Course> courses = readAll();
        course.setId(idCounter.incrementAndGet());
        course.setCreatedAt(LocalDateTime.now());
        courses.add(course);
        writeAll(courses);
        return course;
    }

    public List<Course> findAll() {
        return readAll();
    }

    public Course findById(Long id) {
        return readAll().stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new CourseNotFoundException(id));
    }

    public Course update(Long id, Course updated) {
        validateStatus(updated.getStatus());

        List<Course> courses = readAll();
        Course existing = courses.stream()
            .filter(c -> c.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new CourseNotFoundException(id));

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setTargetDate(updated.getTargetDate());
        existing.setStatus(updated.getStatus());
        // createdAt is immutable after creation

        writeAll(courses);
        return existing;
    }

    public Map<String, Object> getStats() {
        List<Course> courses = readAll();

        Map<String, Long> byStatus = courses.stream()
            .collect(Collectors.groupingBy(Course::getStatus, Collectors.counting()));

        // Ensure all three statuses are always present in the response, even if zero
        for (String status : VALID_STATUSES) {
            byStatus.putIfAbsent(status, 0L);
        }

        return Map.of(
            "total", (long) courses.size(),
            "by_status", byStatus
        );
    }

    public void delete(Long id) {
        List<Course> courses = readAll();
        boolean removed = courses.removeIf(c -> c.getId().equals(id));
        if (!removed) {
            throw new CourseNotFoundException(id);
        }
        writeAll(courses);
    }

    // -------------------------------------------------------- File I/O helpers

    private List<Course> readAll() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(file, new TypeReference<List<Course>>() {
            });
        } catch (IOException e) {
            throw new FileOperationException("Failed to read " + FILE_PATH, e);
        }
    }

    private void writeAll(List<Course> courses) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), courses);
        } catch (IOException e) {
            throw new FileOperationException("Failed to write " + FILE_PATH, e);
        }
    }

    // -------------------------------------------------------- Validation

    private void validateStatus(String status) {
        if (status == null || !VALID_STATUSES.contains(status)) {
            throw new InvalidStatusException(status);
        }
    }

    // -------------------------------------------------------- Inner exceptions
    // Kept in the same file for brevity; move to separate files in larger projects.

    public static class CourseNotFoundException extends RuntimeException {

        public CourseNotFoundException(Long id) {
            super("Course not found with id: " + id);
        }
    }

    public static class InvalidStatusException extends RuntimeException {

        public InvalidStatusException(String status) {
            super("Invalid status value: '" + status + "'. Allowed: Not Started, In Progress, Completed");
        }
    }

    public static class FileOperationException extends RuntimeException {

        public FileOperationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
