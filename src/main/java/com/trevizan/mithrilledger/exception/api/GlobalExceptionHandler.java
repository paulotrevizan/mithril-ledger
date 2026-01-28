package com.trevizan.mithrilledger.exception.api;

import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFoundException(
        WalletNotFoundException ex,
        HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

}
