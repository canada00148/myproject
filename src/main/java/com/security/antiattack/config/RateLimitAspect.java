package com.security.antiattack.config;

import com.security.antiattack.annotation.RateLimit;
import com.security.antiattack.service.RateLimitService;
import com.security.antiattack.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 速率限制切面，實現基於註解的限流
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Around("@annotation(com.security.antiattack.annotation.RateLimit)")
    public Object aroundRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        // 獲取請求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        
        HttpServletRequest request = attributes.getRequest();
        
        // 獲取註解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        
        // 構建限流鍵
        String key = buildRateLimitKey(request, rateLimit, method);
        
        // 檢查限流
        if (!rateLimitService.allowRequest(key, rateLimit.maxRequests(), rateLimit.timeWindow())) {
            throw new RateLimitExceededException("Rate limit exceeded for: " + key);
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 構建限流鍵
     */
    private String buildRateLimitKey(HttpServletRequest request, RateLimit rateLimit, Method method) {
        StringBuilder keyBuilder = new StringBuilder("method:");
        
        // 添加自定義鍵
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key());
        } else {
            keyBuilder.append(method.getDeclaringClass().getSimpleName())
                     .append(".")
                     .append(method.getName());
        }
        
        // 基於IP限流
        if (rateLimit.limitByIp()) {
            String ip = IpUtils.getClientIp(request);
            keyBuilder.append(":ip:").append(ip);
        }
        
        // 基於用戶限流
        if (rateLimit.limitByUser()) {
            String userId = getUserId(request);
            if (userId != null) {
                keyBuilder.append(":user:").append(userId);
            }
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 獲取用戶ID（需要根據實際情況實現）
     */
    private String getUserId(HttpServletRequest request) {
        // 這裡可以從session、JWT token或其他地方獲取用戶ID
        // 示例實現
        Object userId = request.getSession().getAttribute("userId");
        return userId != null ? userId.toString() : null;
    }
    
    /**
     * 速率限制異常
     */
    public static class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}