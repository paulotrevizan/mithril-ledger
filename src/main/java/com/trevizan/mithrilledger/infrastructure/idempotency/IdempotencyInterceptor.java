package com.trevizan.mithrilledger.infrastructure.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IdempotencyService service;

    public IdempotencyInterceptor(IdempotencyService service) {
        this.service = service;
    }

    @Override
    public boolean preHandle(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        @NotNull Object handler
    ) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        if (!method.hasMethodAnnotation(Idempotent.class)) {
            return true;
        }

        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (key == null || key.isBlank()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());;
            response.getWriter().write("Missing Idempotency-Key");
            return false;
        }

        IdempotencyState state = service.tryStart(key);

        if (state.status() == IdempotencyStatus.COMPLETED) {
            response.setStatus(state.responseStatus());
            response.setContentType("application/json");
            response.getWriter().write(state.responseBody());
            return false;
        }

        if (state.status() == IdempotencyStatus.IN_PROGRESS && state.responseBody() != null) {
            response.setStatus(HttpStatus.CONFLICT.value());
            response.getWriter().write("Request already in progress");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        @NotNull Object handler,
        Exception ex
    ) {
        if (ex != null) {
            return;
        }

        if (!(handler instanceof HandlerMethod method)) {
            return;
        }

        if (!method.hasMethodAnnotation(Idempotent.class)) {
            return;
        }

        if (!(response instanceof ContentCachingResponseWrapper res)) {
            return;
        }

        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null) {
            return;
        }

        String responseBody = new String(res.getContentAsByteArray());

        service.markCompleted(
            key,
            res.getStatus(),
            responseBody
        );
    }

}
