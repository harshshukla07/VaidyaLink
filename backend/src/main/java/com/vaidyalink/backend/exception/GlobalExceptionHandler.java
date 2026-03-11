package com.vaidyalink.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return errors;
    }

    // This is for duplicate entries in the db
    @ResponseStatus(HttpStatus.CONFLICT) // 409 Status Code
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public Map<String, String> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {

        Map<String, String> error = new HashMap<>();

        error.put("error", "Email or Mobile number already in use!");

        return error;
    }


    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public Map<String, String> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {

        Map<String, String> error = new HashMap<>();

        if ("status".equals(ex.getName())) {
            error.put("error", "Invalid status provided. Accepted values are: PENDING, CONFIRMED, CANCELLED, COMPLETED.");
        } else {
            error.put("error", "Invalid value provided for parameter: " + ex.getName());
        }

        return error;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalStateException.class)
    public Map<String, String> handleBusinessRuleExceptions(IllegalStateException ex) {

        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());

        return error;
    }
}