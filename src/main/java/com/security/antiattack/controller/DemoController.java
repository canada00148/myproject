package com.security.antiattack.controller;

import com.security.antiattack.annotation.RateLimit;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例控制器，展示如何使用限流和防護功能
 */
@RestController
@RequestMapping("/api")
public class DemoController {
    
    /**
     * 普通接口，會被全局過濾器保護
     */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello World!");
        response.put("status", "success");
        return response;
    }
    
    /**
     * 使用註解進行更嚴格的限流
     * 每個IP每分鐘最多10次請求
     */
    @GetMapping("/limited")
    @RateLimit(maxRequests = 10, timeWindow = 60, limitByIp = true)
    public Map<String, String> limitedEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a rate-limited endpoint");
        response.put("status", "success");
        return response;
    }
    
    /**
     * 搜索接口，限制更嚴格
     * 每個IP每分鐘最多5次搜索
     */
    @PostMapping("/search")
    @RateLimit(maxRequests = 5, timeWindow = 60, limitByIp = true, key = "search")
    public Map<String, Object> search(@RequestBody Map<String, String> searchQuery) {
        Map<String, Object> response = new HashMap<>();
        response.put("query", searchQuery.get("q"));
        response.put("results", "Search results here...");
        response.put("status", "success");
        return response;
    }
    
    /**
     * 用戶特定接口，基於用戶ID限流
     * 每個用戶每小時最多100次請求
     */
    @GetMapping("/user/profile")
    @RateLimit(maxRequests = 100, timeWindow = 3600, limitByUser = true, limitByIp = false)
    public Map<String, Object> userProfile() {
        Map<String, Object> response = new HashMap<>();
        response.put("username", "demo_user");
        response.put("email", "user@example.com");
        response.put("status", "success");
        return response;
    }
    
    /**
     * 測試封包驗證的接口
     */
    @PostMapping("/test-validation")
    public Map<String, String> testValidation(
            @RequestParam(required = false) String param1,
            @RequestParam(required = false) String param2) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Request validated successfully");
        response.put("param1", param1);
        response.put("param2", param2);
        return response;
    }
}