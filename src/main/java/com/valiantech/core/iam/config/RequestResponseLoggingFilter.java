package com.valiantech.core.iam.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Envolver request y response
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Continuar el flujo
        filterChain.doFilter(wrappedRequest, wrappedResponse);

        // Leer payload del request
        String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);

        // Leer payload del response
        String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);

        logger.info("REQUEST URI : " + request.getRequestURI());
        logger.info("REQUEST BODY : " + requestBody);
        logger.info("RESPONSE STATUS : " + response.getStatus());
        logger.info("RESPONSE BODY : " + responseBody);

        // Copiar el contenido del response para que llegue al cliente
        wrappedResponse.copyBodyToResponse();
    }
}
