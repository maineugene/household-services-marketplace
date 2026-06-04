package com.zhukovskiy.platform.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String entityName, Object identifier) {
        super(entityName + " не найден(а): " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
