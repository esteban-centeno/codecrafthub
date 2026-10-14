package com.codecrafthub.courseapi.service;

import com.codecrafthub.courseapi.exception.CourseNotFoundException;
import com.codecrafthub.courseapi.exception.CourseStorageException;
import com.codecrafthub.courseapi.model.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service layer responsible for reading and writing courses.json.
 */
@Service
public class CourseService {

    private static final String FILE_NAME = "courses.json";

    private final Path filePath;
    private final ObjectMapper objectMapper;

    public CourseService() {
        this.filePath = Paths.get(FILE_NAME);

        /*
         * Configure Jackson for Java date/time types such as LocalDate and Instant.
         */
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        createFileIfItDoesNotExist();
    }

    /**
     * Creates courses.json with an empty JSON array if it does not exist.
     */
    private void createFileIfItDoesNotExist() {
        try {
            if (Files.notExists(filePath)) {
                Files.writeString(
                        filePath,
                        "[]",
                        StandardCharsets.UTF_8
                );
            }
        } catch (IOException exception) {
            throw new CourseStorageException(
                    "Unable to create " + FILE_NAME,
                    exception
            );
        }
    }

    /**
     * Reads all courses from courses.json.
     */
    private synchronized List<Course> readCourses() {
        try {
            createFileIfItDoesNotExist();

            String json = Files.readString(
                    filePath,
                    StandardCharsets.UTF_8
            );

            if (json.isBlank()) {
                return new ArrayList<>();
            }

            List<Course> courses = objectMapper.readValue(
                    json,
                    new TypeReference<List<Course>>() {
                    }
            );

            return courses == null ? new ArrayList<>() : courses;

        } catch (IOException exception) {
            throw new CourseStorageException(
                    "Unable to read " + FILE_NAME,
                    exception
            );
        }
    }

    /**
     * Writes all courses to courses.json.
     *
     * A temporary file is written first so that the original file is not
     * partially overwritten if an error occurs during writing.
     */
    private synchronized void writeCourses(List<Course> courses) {
        Path absoluteFilePath = filePath.toAbsolutePath();
        Path parentDirectory = absoluteFilePath.getParent();
        Path temporaryFile = parentDirectory.resolve(FILE_NAME + ".tmp");

        try {
            String json = objectMapper.writeValueAsString(courses);

            Files.writeString(
                    temporaryFile,
                    json,
                    StandardCharsets.UTF_8
            );

            try {
                Files.move(
                        temporaryFile,
                        absoluteFilePath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException exception) {
                // Some operating systems do not support atomic moves.
                Files.move(
                        temporaryFile,
                        absoluteFilePath,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

        } catch (IOException exception) {
            throw new CourseStorageException(
                    "Unable to write " + FILE_NAME,
                    exception
            );
        }
    }

    /**
     * Returns all courses.
     */
    public List<Course> getAllCourses() {
        return readCourses();
    }

    /**
     * Returns one course by ID.
     */
    public Course getCourseById(Long id) {
        return readCourses()
                .stream()
                .filter(course -> course.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                "Course with id " + id + " was not found"
                        )
                );
    }

    /**
     * Adds a new course.
     *
     * IDs begin at 1. Deleted IDs are not reused.
     */
    public synchronized Course createCourse(Course course) {
        List<Course> courses = readCourses();

        long nextId = courses
                .stream()
                .map(Course::getId)
                .filter(id -> id != null)
                .max(Comparator.naturalOrder())
                .orElse(0L) + 1;

        course.setId(nextId);
        course.setCreatedAt(Instant.now());

        courses.add(course);
        writeCourses(courses);

        return course;
    }

    /**
     * Updates an existing course.
     *
     * The ID and created_at values cannot be changed by an update.
     */
    public synchronized Course updateCourse(Long id, Course updatedCourse) {
        List<Course> courses = readCourses();

        Course existingCourse = courses
                .stream()
                .filter(course -> course.getId().equals(id))
                .findFirst()
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                "Course with id " + id + " was not found"
                        )
                );

        existingCourse.setName(updatedCourse.getName());
        existingCourse.setDescription(updatedCourse.getDescription());
        existingCourse.setTargetDate(updatedCourse.getTargetDate());
        existingCourse.setStatus(updatedCourse.getStatus());

        writeCourses(courses);

        return existingCourse;
    }

    /**
     * Deletes a course by ID.
     */
    public synchronized void deleteCourse(Long id) {
        List<Course> courses = readCourses();

        boolean removed = courses.removeIf(
                course -> course.getId().equals(id)
        );

        if (!removed) {
            throw new CourseNotFoundException(
                    "Course with id " + id + " was not found"
            );
        }

        writeCourses(courses);
    }
}