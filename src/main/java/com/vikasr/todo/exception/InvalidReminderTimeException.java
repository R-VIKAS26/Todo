package com.vikasr.todo.exception;

public class InvalidReminderTimeException extends RuntimeException {
    public InvalidReminderTimeException(String message) {
        super(message);
    }
}
