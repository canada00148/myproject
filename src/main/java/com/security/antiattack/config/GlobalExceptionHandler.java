package com.security.antiattack.config;

import com.security.antiattack.config.RateLimitAspect.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局異常處理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * 處理速率限制異常
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(
            RateLimitExceededException ex, HttpServletRequest request) {
        
        log.warn("Rate limit exceeded: {} - {}", request.getRequestURI(), ex.getMessage());
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Too Many Requests");
        error.put("message", "You have exceeded the rate limit. Please try again later.");
        error.put("timestamp", LocalDateTime.now());
        error.put("path", request.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }
    
    /**
     * 處理其他異常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error: {} - {}", request.getRequestURI(), ex.getMessage(), ex);
        
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Internal Server Error");
        error.put("message", "An unexpected error occurred");
        error.put("timestamp", LocalDateTime.now());
        error.put("path", request.getRequestURI());
        
        // 在開發環境可以返回詳細錯誤信息
        if (log.isDebugEnabled()) {
            error.put("debug", ex.getMessage());
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}