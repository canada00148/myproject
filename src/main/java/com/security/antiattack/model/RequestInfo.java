package com.security.antiattack.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 請求信息類，用於記錄和分析請求
 */
@Data
@Builder
public class RequestInfo {
    private String ip;
    private String userAgent;
    private String requestUrl;
    private String requestMethod;
    private Map<String, String> headers;
    private LocalDateTime timestamp;
    private long requestSize;
    private String sessionId;
    private String userId;
    private boolean suspicious;
    private String suspiciousReason;
}