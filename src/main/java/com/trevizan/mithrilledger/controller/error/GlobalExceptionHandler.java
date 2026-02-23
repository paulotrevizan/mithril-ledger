package com.trevizan.mithrilledger.controller.error;

import com.trevizan.mithrilledger.exception.domain.InsufficientBalanceException;
import com.trevizan.mithrilledger.exception.domain.WalletNotFoundException;
import com.trevizan.mithrilledger.exception.infrastructure.ExchangeInvalidResponseException;
import com.trevizan.mithrilledger.exception.infrastructure.ExchangeServiceUnavailableException;

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
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(
        InsufficientBalanceException ex,
        HttpServletRequest request
    ) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(ExchangeServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleExchangeServiceUnavailableException(
        ExchangeServiceUnavailableException ex,
        HttpServletRequest request
    ) {
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    @ExceptionHandler(ExchangeInvalidResponseException.class)
    public ResponseEntity<ErrorResponse> handleExchangeInvalidResponseException(
        ExchangeInvalidResponseException ex,
        HttpServletRequest request
    ) {
        return buildErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
        Exception ex,
        HttpStatus status,
        HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }

}
