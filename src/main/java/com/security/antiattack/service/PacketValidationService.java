package com.security.antiattack.service;

import com.security.antiattack.model.RequestInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 封包驗證服務，檢測異常請求
 */
@Slf4j
@Service
public class PacketValidationService {
    
    // 常見的SQL注入模式
    private static final List<Pattern> SQL_INJECTION_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\b(union|select|insert|update|delete|drop|create)\\b.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*[';].*--.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*\\b(or|and)\\b.*=.*", Pattern.CASE_INSENSITIVE)
    );
    
    // XSS攻擊模式
    private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
        Pattern.compile(".*<script.*>.*</script>.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*javascript:.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*on\\w+\\s*=.*", Pattern.CASE_INSENSITIVE)
    );
    
    // 路徑遍歷模式
    private static final List<Pattern> PATH_TRAVERSAL_PATTERNS = Arrays.asList(
        Pattern.compile(".*\\.\\./.*"),
        Pattern.compile(".*\\.\\\\.*")
    );
    
    // 異常User-Agent
    private static final List<String> SUSPICIOUS_USER_AGENTS = Arrays.asList(
        "sqlmap", "nikto", "scanner", "spider", "bot", "crawler", "scraper"
    );
    
    /**
     * 驗證請求是否合法
     * @param request HTTP請求
     * @return 驗證結果
     */
    public ValidationResult validateRequest(HttpServletRequest request) {
        ValidationResult result = new ValidationResult();
        
        // 檢查請求頭
        validateHeaders(request, result);
        
        // 檢查請求參數
        validateParameters(request, result);
        
        // 檢查請求路徑
        validatePath(request, result);
        
        // 檢查User-Agent
        validateUserAgent(request, result);
        
        // 檢查請求大小
        validateRequestSize(request, result);
        
        return result;
    }
    
    /**
     * 驗證請求頭
     */
    private void validateHeaders(HttpServletRequest request, ValidationResult result) {
        // 檢查必要的請求頭
        if (request.getHeader("Host") == null) {
            result.addError("Missing Host header");
        }
        
        // 檢查Content-Length是否異常
        String contentLength = request.getHeader("Content-Length");
        if (contentLength != null) {
            try {
                long length = Long.parseLong(contentLength);
                if (length > 10 * 1024 * 1024) { // 10MB
                    result.addError("Content-Length too large: " + length);
                }
            } catch (NumberFormatException e) {
                result.addError("Invalid Content-Length: " + contentLength);
            }
        }
        
        // 檢查異常的請求頭
        if (request.getHeader("X-Forwarded-For") != null) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff.split(",").length > 5) {
                result.addError("Too many proxies in X-Forwarded-For");
            }
        }
    }
    
    /**
     * 驗證請求參數
     */
    private void validateParameters(HttpServletRequest request, ValidationResult result) {
        Map<String, String[]> parameters = request.getParameterMap();
        
        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            
            // 檢查參數名
            if (containsSuspiciousPattern(paramName)) {
                result.addError("Suspicious parameter name: " + paramName);
            }
            
            // 檢查參數值
            for (String value : paramValues) {
                if (value != null) {
                    // SQL注入檢測
                    if (containsSqlInjection(value)) {
                        result.addError("Possible SQL injection in parameter: " + paramName);
                    }
                    
                    // XSS檢測
                    if (containsXss(value)) {
                        result.addError("Possible XSS in parameter: " + paramName);
                    }
                    
                    // 檢查參數長度
                    if (value.length() > 1000) {
                        result.addError("Parameter too long: " + paramName);
                    }
                }
            }
        }
    }
    
    /**
     * 驗證請求路徑
     */
    private void validatePath(HttpServletRequest request, ValidationResult result) {
        String path = request.getRequestURI();
        
        // 路徑遍歷檢測
        for (Pattern pattern : PATH_TRAVERSAL_PATTERNS) {
            if (pattern.matcher(path).matches()) {
                result.addError("Path traversal attempt detected");
                break;
            }
        }
        
        // 檢查路徑長度
        if (path.length() > 255) {
            result.addError("Request path too long");
        }
        
        // 檢查非法字符
        if (path.contains("\0") || path.contains("\n") || path.contains("\r")) {
            result.addError("Invalid characters in request path");
        }
    }
    
    /**
     * 驗證User-Agent
     */
    private void validateUserAgent(HttpServletRequest request, ValidationResult result) {
        String userAgent = request.getHeader("User-Agent");
        
        if (userAgent == null || userAgent.isEmpty()) {
            result.addWarning("Missing User-Agent");
            return;
        }
        
        // 檢查可疑的User-Agent
        String lowerUserAgent = userAgent.toLowerCase();
        for (String suspicious : SUSPICIOUS_USER_AGENTS) {
            if (lowerUserAgent.contains(suspicious)) {
                result.addError("Suspicious User-Agent: " + userAgent);
                break;
            }
        }
    }
    
    /**
     * 驗證請求大小
     */
    private void validateRequestSize(HttpServletRequest request, ValidationResult result) {
        int contentLength = request.getContentLength();
        
        // 檢查異常大的請求
        if (contentLength > 50 * 1024 * 1024) { // 50MB
            result.addError("Request body too large: " + contentLength);
        }
        
        // 檢查GET請求是否帶有body
        if ("GET".equalsIgnoreCase(request.getMethod()) && contentLength > 0) {
            result.addWarning("GET request with body");
        }
    }
    
    /**
     * 檢查是否包含可疑模式
     */
    private boolean containsSuspiciousPattern(String input) {
        if (input == null) return false;
        
        // 檢查常見的攻擊模式
        return input.contains("../") || 
               input.contains("..\\") ||
               input.contains("<script") ||
               input.contains("javascript:") ||
               input.length() > 100;
    }
    
    /**
     * 檢查SQL注入
     */
    private boolean containsSqlInjection(String input) {
        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 檢查XSS
     */
    private boolean containsXss(String input) {
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 驗證結果類
     */
    public static class ValidationResult {
        private boolean valid = true;
        private final List<String> errors = new java.util.ArrayList<>();
        private final List<String> warnings = new java.util.ArrayList<>();
        
        public void addError(String error) {
            this.valid = false;
            this.errors.add(error);
            log.error("Validation error: {}", error);
        }
        
        public void addWarning(String warning) {
            this.warnings.add(warning);
            log.warn("Validation warning: {}", warning);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
    }
}