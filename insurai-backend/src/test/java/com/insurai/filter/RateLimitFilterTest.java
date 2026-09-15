package com.insurai.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    public void setUp() {
        rateLimitFilter = new RateLimitFilter();
    }

    @Test
    public void testRateLimitExceededReturns429() throws Exception {
        String clientIp = "192.168.1.100";

        // Perform 1000 successful requests
        for (int i = 0; i < 1000; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = new MockFilterChain();

            rateLimitFilter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus(), "Request " + (i + 1) + " should pass");
        }

        // 1001st request must return 429
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(clientIp);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        rateLimitFilter.doFilter(request, response, chain);
        assertEquals(429, response.getStatus(), "1001st request should be rate limited with HTTP 429");
        assertEquals("Too many requests", response.getContentAsString());
    }

    @Test
    public void testXForwardedForHeaderUsed() throws Exception {
        String proxiedIp1 = "203.0.113.195";
        String proxiedIp2 = "198.51.100.17";

        // Consume 1000 tokens for proxiedIp1 via X-Forwarded-For
        for (int i = 0; i < 1000; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-For", proxiedIp1 + ", 10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = new MockFilterChain();

            rateLimitFilter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
        }

        // 1001st request for proxiedIp1 should return 429
        MockHttpServletRequest reqExceeded = new MockHttpServletRequest();
        reqExceeded.addHeader("X-Forwarded-For", proxiedIp1 + ", 10.0.0.1");
        MockHttpServletResponse respExceeded = new MockHttpServletResponse();
        rateLimitFilter.doFilter(reqExceeded, respExceeded, new MockFilterChain());
        assertEquals(429, respExceeded.getStatus());

        // Request for a different IP (proxiedIp2) should still succeed (independent bucket)
        MockHttpServletRequest reqIp2 = new MockHttpServletRequest();
        reqIp2.addHeader("X-Forwarded-For", proxiedIp2);
        MockHttpServletResponse respIp2 = new MockHttpServletResponse();
        rateLimitFilter.doFilter(reqIp2, respIp2, new MockFilterChain());
        assertEquals(200, respIp2.getStatus());
    }
}
