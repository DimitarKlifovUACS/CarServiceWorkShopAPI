package com.example.CarService.exception;

// The request itself is invalid (e.g. an unknown filter value): returned as 400 Bad Request
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
