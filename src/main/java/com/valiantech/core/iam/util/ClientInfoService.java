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
