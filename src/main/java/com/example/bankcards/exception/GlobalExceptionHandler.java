package com.example.bankcards.exception;

import com.example.bankcards.validators.AuthDetailsValidator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.Errors;
import org.springframework.validation.SimpleErrors;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<?> handleBadCredentialsException(BadCredentialsException ex) {
        Errors errors = new SimpleErrors(ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthDetailsValidator.badCredentialsError(errors));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

}