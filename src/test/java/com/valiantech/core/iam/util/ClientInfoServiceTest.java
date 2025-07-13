package com.valiantech.core.iam.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientInfoServiceTest {

    @Mock HttpServletRequest request;

    @InjectMocks
    ClientInfoService service;

    @Nested
    @DisplayName("getClientIp")
    class GetClientIpTests {

        @Test
        @DisplayName("Debe retornar la IP del header X-Forwarded-For si existe")
        void shouldReturnIpFromXForwardedFor() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
            String ip = service.getClientIp();
            assertEquals("1.2.3.4", ip);
        }

        @Test
        @DisplayName("Debe retornar la IP de request.getRemoteAddr si no hay X-Forwarded-For")
        void shouldReturnIpFromRemoteAddr() {
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("9.9.9.9");
            String ip = service.getClientIp();
            assertEquals("9.9.9.9", ip);
        }

        @Test
        @DisplayName("Debe retornar la IP de getRemoteAddr si X-Forwarded-For es 'unknown'")
        void shouldReturnRemoteAddrIfXForwardedIsUnknown() {
            when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
            when(request.getRemoteAddr()).thenReturn("8.8.8.8");
            String ip = service.getClientIp();
            assertEquals("8.8.8.8", ip);
        }
    }

    @Nested
    @DisplayName("getUserAgent")
    class GetUserAgentTests {

        @Test
        @DisplayName("Debe retornar el user-agent del header")
        void shouldReturnUserAgent() {
            when(request.getHeader("User-Agent")).thenReturn("JUnit-Agent");
            String userAgent = service.getUserAgent();
            assertEquals("JUnit-Agent", userAgent);
        }
    }

    @Nested
    @DisplayName("getCookies")
    class GetCookiesTests {

        @Test
        @DisplayName("Debe retornar las cookies serializadas como JSON")
        void shouldReturnCookiesAsJson() {
            Cookie cookie1 = new Cookie("token", "abc");
            Cookie cookie2 = new Cookie("theme", "dark");
            when(request.getCookies()).thenReturn(new Cookie[]{cookie1, cookie2});

            String json = service.getCookies();

            assertTrue(json.contains("\"name\":\"token\""));
            assertTrue(json.contains("\"value\":\"abc\""));
            assertTrue(json.contains("\"name\":\"theme\""));
            assertTrue(json.contains("\"value\":\"dark\""));
        }

        @Test
        @DisplayName("Debe retornar '[]' si no hay cookies")
        void shouldReturnEmptyArrayIfNoCookies() {
            when(request.getCookies()).thenReturn(null);

            String json = service.getCookies();

            assertEquals("[]", json);
        }
    }
}
