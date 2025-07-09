package com.valiantech.core.iam.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Log4j2
@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final SensitiveFieldConfig sensitiveFieldConfig;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            loggingRequest(wrappedRequest);
            loggingResponse(wrappedRequest, wrappedResponse);
            wrappedResponse.copyBodyToResponse(); // Importante: para no romper la response
        }
    }

    private void loggingRequest(ContentCachingRequestWrapper wrappedRequest) {
        String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
        StringBuilder reqMessage = new StringBuilder();
        Map<String, String> parameters = getParameters(wrappedRequest);

        reqMessage.append("REQUEST");
        reqMessage.append(" method = [").append(wrappedRequest.getMethod()).append("]");
        reqMessage.append(" path = [").append(wrappedRequest.getRequestURI()).append("] ");
        if (!parameters.isEmpty()) {
            reqMessage.append(" parameters = [").append(parameters).append("] ");
        }
        reqMessage.append(" body = [").append(sanitizeBody(requestBody)).append("]");

        log.info(reqMessage.toString());
    }

    private void loggingResponse(ContentCachingRequestWrapper wrappedRequest, ContentCachingResponseWrapper wrappedResponse) {
        String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);

        StringBuilder respMessage = new StringBuilder();
        Map<String, String> headers = getHeaders(wrappedResponse);
        respMessage.append("RESPONSE");
        respMessage.append(" method = [").append(wrappedRequest.getMethod()).append("]");
        respMessage.append(" path = [").append(wrappedRequest.getRequestURI()).append("]");
        respMessage.append(" status = [").append(wrappedResponse.getStatus()).append("]");
        if (!headers.isEmpty()) {
            respMessage.append(" ResponseHeaders = [").append(headers).append("]");
        }
        respMessage.append(" responseBody = [").append(sanitizeBody(responseBody)).append("]");

        log.info(respMessage.toString());
    }

    private Map<String, String> getParameters(HttpServletRequest request) {
        Map<String, String> parameters = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String paramName = params.nextElement();
            String paramValue = request.getParameter(paramName);
            parameters.put(paramName, paramValue);
        }
        return parameters;
    }

    private Map<String, String> getHeaders(HttpServletResponse response) {
        Map<String, String> headers = new HashMap<>();
        Collection<String> headerNames = response.getHeaderNames();
        for (String name : headerNames) {
            headers.put(name, response.getHeader(name));
        }
        return headers;
    }

    private String sanitizeBody(String body) {
        if (body == null || body.isBlank()) return body;
        try {
            Map<String, Object> map = objectMapper.readValue(body, Map.class);
            for (String field : sensitiveFieldConfig.getSensitiveFields()) {
                map.computeIfPresent(field, (k, v) -> "*****");

            }
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            // Si no es JSON, retorna el body original (por ejemplo, multipart)
            return body;
        }
    }
}
