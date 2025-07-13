package com.valiantech.core.iam.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio utilitario para extraer información relevante del cliente HTTP,
 * como IP, agente de usuario (User-Agent) y cookies en formato JSON.
 * <ul>
 *   <li>Soporta cabeceras estándar como X-Forwarded-For para detección real de IP tras proxy/reverse proxy.</li>
 *   <li>Convierte las cookies a una lista serializada en JSON para trazabilidad o auditoría.</li>
 *   <li>Si la serialización de cookies falla, retorna null y deja trazado en log.</li>
 * </ul>
 * <b>Notas:</b>
 * <ul>
 *   <li>El servicio está diseñado para usarse en capas de auditoría, seguridad y logging.</li>
 *   <li>Depende de {@link HttpServletRequest} que debe estar correctamente inyectado en contexto web.</li>
 * </ul>
 * @author Ian Cardenas
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ClientInfoService {

    private final HttpServletRequest request;

    public String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public String getUserAgent() {
        return request.getHeader("User-Agent");
    }

    public String getCookies() {
        Cookie[] cookies = request.getCookies();
        List<Map<String, String>> cookieList = new ArrayList<>();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                Map<String, String> c = new HashMap<>();
                c.put("name", cookie.getName());
                c.put("value", cookie.getValue());
                cookieList.add(c);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(cookieList);
        } catch (JsonProcessingException e) {
            log.warn("Cannot write values as string cookies");
            return null;
        }
    }
}
