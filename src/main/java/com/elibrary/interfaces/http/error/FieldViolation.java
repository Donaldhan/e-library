package com.elibrary.interfaces.http.error;

public record FieldViolation(String field, String message) {
}
