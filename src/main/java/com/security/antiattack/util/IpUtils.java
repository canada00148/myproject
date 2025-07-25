package com.security.antiattack.util;

import javax.servlet.http.HttpServletRequest;

/**
 * IP地址工具類
 */
public class IpUtils {
    
    private static final String[] IP_HEADERS = {
        "X-Real-IP",
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_CLIENT_IP",
        "HTTP_X_FORWARDED_FOR"
    };
    
    /**
     * 獲取客戶端真實IP地址
     * @param request HTTP請求
     * @return 客戶端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = null;
        
        // 嘗試從各種請求頭中獲取IP
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }
        
        // 如果沒有找到，使用RemoteAddr
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 處理多個IP的情況（經過多個代理）
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String strIp : ips) {
                if (isValidIp(strIp)) {
                    ip = strIp;
                    break;
                }
            }
        }
        
        return ip != null ? ip.trim() : "unknown";
    }
    
    /**
     * 驗證IP地址是否有效
     * @param ip IP地址
     * @return 是否有效
     */
    private static boolean isValidIp(String ip) {
        return ip != null && 
               !ip.isEmpty() && 
               !"unknown".equalsIgnoreCase(ip) &&
               !"0:0:0:0:0:0:0:1".equals(ip) && // IPv6 localhost
               !"::1".equals(ip); // IPv6 localhost short form
    }
    
    /**
     * 判斷是否為內網IP
     * @param ip IP地址
     * @return 是否為內網IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        return ip.startsWith("10.") ||
               ip.startsWith("172.16.") ||
               ip.startsWith("172.17.") ||
               ip.startsWith("172.18.") ||
               ip.startsWith("172.19.") ||
               ip.startsWith("172.20.") ||
               ip.startsWith("172.21.") ||
               ip.startsWith("172.22.") ||
               ip.startsWith("172.23.") ||
               ip.startsWith("172.24.") ||
               ip.startsWith("172.25.") ||
               ip.startsWith("172.26.") ||
               ip.startsWith("172.27.") ||
               ip.startsWith("172.28.") ||
               ip.startsWith("172.29.") ||
               ip.startsWith("172.30.") ||
               ip.startsWith("172.31.") ||
               ip.startsWith("192.168.") ||
               ip.equals("127.0.0.1") ||
               ip.equals("localhost");
    }
}