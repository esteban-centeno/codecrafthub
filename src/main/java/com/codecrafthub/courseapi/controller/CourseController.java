package com.codecrafthub.courseapi.controller;

import com.codecrafthub.courseapi.model.Course;
import com.codecrafthub.courseapi.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller providing CRUD endpoints for courses.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    /**
     * Adds a new course.
     *
     * POST /api/courses
     */
    @PostMapping
    public ResponseEntity<Course> createCourse(
            @Valid @RequestBody Course course
    ) {
        Course createdCourse = courseService.createCourse(course);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCourse);
    }

    /**
     * Gets all courses.
     *
     * GET /api/courses
     */
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    /**
     * Gets one course by ID.
     *
     * GET /api/courses/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    /**
     * Updates an existing course.
     *
     * PUT /api/courses/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody Course course
    ) {
        return ResponseEntity.ok(
                courseService.updateCourse(id, course)
        );
    }

    /**
     * Deletes an existing course.
     *
     * DELETE /api/courses/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long id
    ) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}