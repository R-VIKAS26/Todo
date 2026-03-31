package com.vikasr.todo.exception;

public class SelfDependencyException extends RuntimeException {
    public SelfDependencyException(String message) {
        super(message);
    }
}
