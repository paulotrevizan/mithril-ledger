package com.trevizan.mithrilledger.infrastructure.idempotency;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class CachedRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!(request instanceof ContentCachingRequestWrapper)) {
            ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, 1024 * 1024);
            filterChain.doFilter(wrapped, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

}
