package com.security.antiattack.filter;

import com.security.antiattack.service.RateLimitService;
import com.security.antiattack.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 抗CC攻擊過濾器
 */
@Slf4j
@Component
@Order(1)
public class AntiCCFilter implements Filter {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Value("${security.cc.max-requests-per-minute:60}")
    private int maxRequestsPerMinute;
    
    @Value("${security.cc.max-requests-per-second:10}")
    private int maxRequestsPerSecond;
    
    @Value("${security.cc.enabled:true}")
    private boolean enabled;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientIp = IpUtils.getClientIp(httpRequest);
        String requestUri = httpRequest.getRequestURI();
        
        // 基於IP的分鐘級限流
        String minuteKey = "cc:minute:" + clientIp;
        if (!rateLimitService.allowRequest(minuteKey, maxRequestsPerMinute, 60)) {
            handleRateLimitExceeded(httpResponse, clientIp, "Minute rate limit exceeded");
            return;
        }
        
        // 基於IP的秒級限流（防止突發流量）
        String secondKey = "cc:second:" + clientIp;
        if (!rateLimitService.allowRequest(secondKey, maxRequestsPerSecond, 1)) {
            handleRateLimitExceeded(httpResponse, clientIp, "Second rate limit exceeded");
            return;
        }
        
        // 基於IP+URI的限流（防止針對特定接口的攻擊）
        String uriKey = "cc:uri:" + clientIp + ":" + requestUri;
        if (!rateLimitService.allowRequest(uriKey, 30, 60)) {
            handleRateLimitExceeded(httpResponse, clientIp, "URI rate limit exceeded for: " + requestUri);
            return;
        }
        
        // 使用令牌桶算法進行更平滑的限流
        String tokenBucketKey = "token:" + clientIp;
        if (!rateLimitService.tryAcquireWithTokenBucket(tokenBucketKey, 2.0)) {
            handleRateLimitExceeded(httpResponse, clientIp, "Token bucket rate limit exceeded");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * 處理限流超出的情況
     */
    private void handleRateLimitExceeded(HttpServletResponse response, String ip, String reason) 
            throws IOException {
        log.warn("Rate limit exceeded for IP: {}, reason: {}", ip, reason);
        
        response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
    }
}