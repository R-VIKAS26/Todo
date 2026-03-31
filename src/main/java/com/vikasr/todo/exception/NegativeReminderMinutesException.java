package com.vikasr.todo.exception;

public class NegativeReminderMinutesException extends RuntimeException {
    public NegativeReminderMinutesException(String message) {
        super(message);
    }
}
