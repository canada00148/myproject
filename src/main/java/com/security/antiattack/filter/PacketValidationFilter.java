package com.security.antiattack.filter;

import com.security.antiattack.service.PacketValidationService;
import com.security.antiattack.service.PacketValidationService.ValidationResult;
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
 * 封包驗證過濾器，檢測異常請求
 */
@Slf4j
@Component
@Order(2)
public class PacketValidationFilter implements Filter {
    
    @Autowired
    private PacketValidationService validationService;
    
    @Value("${security.packet-validation.enabled:true}")
    private boolean enabled;
    
    @Value("${security.packet-validation.block-on-error:true}")
    private boolean blockOnError;
    
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
        
        try {
            // 驗證請求
            ValidationResult validationResult = validationService.validateRequest(httpRequest);
            
            if (!validationResult.isValid()) {
                log.error("Invalid request from IP: {}, errors: {}", 
                    clientIp, validationResult.getErrors());
                
                if (blockOnError) {
                    handleInvalidRequest(httpResponse, validationResult);
                    return;
                }
            }
            
            // 記錄警告但繼續處理
            if (!validationResult.getWarnings().isEmpty()) {
                log.warn("Request warnings from IP: {}, warnings: {}", 
                    clientIp, validationResult.getWarnings());
            }
            
        } catch (Exception e) {
            log.error("Error validating request from IP: {}", clientIp, e);
            if (blockOnError) {
                handleValidationError(httpResponse);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * 處理無效請求
     */
    private void handleInvalidRequest(HttpServletResponse response, ValidationResult result) 
            throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");
        
        // 不要暴露太多錯誤信息給潛在攻擊者
        String message = "{\"error\":\"Invalid request\"}";
        
        // 在開發環境可以返回詳細錯誤
        if (log.isDebugEnabled()) {
            message = String.format("{\"error\":\"Invalid request\",\"details\":%s}", 
                result.getErrors());
        }
        
        response.getWriter().write(message);
    }
    
    /**
     * 處理驗證錯誤
     */
    private void handleValidationError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Request validation error\"}");
    }
}