package com.codecrafthub.courseapi.exception;

/**
 * Thrown when courses.json cannot be read or written.
 */
public class CourseStorageException extends RuntimeException {

    public CourseStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}