package com.security.antiattack.annotation;

import java.lang.annotation.*;

/**
 * 速率限制註解，用於標記需要進行速率限制的方法或類
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * 時間窗口內允許的最大請求數
     */
    int maxRequests() default 100;
    
    /**
     * 時間窗口大小（秒）
     */
    int timeWindow() default 60;
    
    /**
     * 是否基於IP進行限制
     */
    boolean limitByIp() default true;
    
    /**
     * 是否基於用戶進行限制
     */
    boolean limitByUser() default false;
    
    /**
     * 自定義限制鍵
     */
    String key() default "";
}