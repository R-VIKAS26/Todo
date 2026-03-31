package com.vikasr.todo.exception;

public class ResetTokenExpiredException extends RuntimeException {
    public ResetTokenExpiredException(String message) {
        super(message);
    }
}
