package com.security.tcp.service;

import com.security.tcp.config.ServerConfig;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * TCP封包驗證器，檢測異常封包
 */
public class PacketValidator {
    private static final Logger logger = LoggerFactory.getLogger(PacketValidator.class);
    private static final PacketValidator INSTANCE = new PacketValidator();
    
    private final ServerConfig config = ServerConfig.getInstance();
    
    // 常見的攻擊特徵
    private static final byte[] SHELLCODE_PATTERN = {(byte)0x90, (byte)0x90, (byte)0x90, (byte)0x90}; // NOP sled
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            ".*(union|select|insert|update|delete|drop|create|alter|exec|execute).*", 
            Pattern.CASE_INSENSITIVE);
    
    // 協議特徵（可以根據實際協議調整）
    private static final int MIN_VALID_PACKET_SIZE = 1;
    private static final int MAX_VALID_PACKET_SIZE = 65536;
    
    private PacketValidator() {}
    
    public static PacketValidator getInstance() {
        return INSTANCE;
    }
    
    /**
     * 驗證封包
     * @return 驗證結果
     */
    public ValidationResult validatePacket(ByteBuf packet, String clientIp) {
        ValidationResult result = new ValidationResult();
        
        try {
            // 1. 檢查封包大小
            if (!validatePacketSize(packet, result)) {
                return result;
            }
            
            // 2. 檢查封包結構
            if (!validatePacketStructure(packet, result)) {
                return result;
            }
            
            // 3. 檢查是否包含攻擊特徵
            if (!checkForAttackPatterns(packet, result)) {
                return result;
            }
            
            // 4. 檢查協議合規性
            if (!validateProtocolCompliance(packet, result)) {
                return result;
            }
            
            result.setValid(true);
        } catch (Exception e) {
            logger.error("Error validating packet from {}: {}", clientIp, e.getMessage());
            result.setValid(false);
            result.addError("Packet validation error: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 驗證封包大小
     */
    private boolean validatePacketSize(ByteBuf packet, ValidationResult result) {
        int size = packet.readableBytes();
        
        if (size < config.getMinPacketSize()) {
            result.addError("Packet too small: " + size + " bytes");
            return false;
        }
        
        if (size > config.getMaxPacketSize()) {
            result.addError("Packet too large: " + size + " bytes");
            return false;
        }
        
        return true;
    }
    
    /**
     * 驗證封包結構
     */
    private boolean validatePacketStructure(ByteBuf packet, ValidationResult result) {
        // 檢查是否有足夠的數據
        if (packet.readableBytes() < 4) {
            // 太小，無法包含有效的頭部
            result.addWarning("Packet too small for valid header");
            return true; // 這裡返回true，讓其他檢查繼續
        }
        
        // 檢查魔術數字或協議標識（根據實際協議調整）
        packet.markReaderIndex();
        byte firstByte = packet.readByte();
        packet.resetReaderIndex();
        
        // 檢查是否為ASCII可打印字符範圍
        if (firstByte >= 0x20 && firstByte <= 0x7E) {
            // 可能是文本協議
            return true;
        } else if (firstByte == 0x00 || firstByte == 0xFF) {
            // 可能的二進制協議標記
            return true;
        } else if ((firstByte & 0xFF) > 0x7F) {
            // 高位元組，可能是二進制數據
            result.addWarning("Non-ASCII data detected");
        }
        
        return true;
    }
    
    /**
     * 檢查攻擊特徵
     */
    private boolean checkForAttackPatterns(ByteBuf packet, ValidationResult result) {
        packet.markReaderIndex();
        
        try {
            // 1. 檢查Shellcode特徵
            if (containsShellcode(packet)) {
                result.addError("Possible shellcode detected");
                result.setAttackType("SHELLCODE");
                return false;
            }
            
            // 2. 檢查緩衝區溢出特徵
            if (containsBufferOverflowPattern(packet)) {
                result.addError("Possible buffer overflow attempt detected");
                result.setAttackType("BUFFER_OVERFLOW");
                return false;
            }
            
            // 3. 如果是文本數據，檢查SQL注入
            if (isTextData(packet)) {
                String content = extractTextContent(packet);
                if (SQL_INJECTION_PATTERN.matcher(content).find()) {
                    result.addError("Possible SQL injection detected");
                    result.setAttackType("SQL_INJECTION");
                    return false;
                }
            }
            
            // 4. 檢查格式字符串漏洞
            if (containsFormatStringAttack(packet)) {
                result.addError("Possible format string attack detected");
                result.setAttackType("FORMAT_STRING");
                return false;
            }
            
        } finally {
            packet.resetReaderIndex();
        }
        
        return true;
    }
    
    /**
     * 驗證協議合規性
     */
    private boolean validateProtocolCompliance(ByteBuf packet, ValidationResult result) {
        // 這裡可以添加特定協議的驗證邏輯
        // 例如：檢查協議頭、版本號、命令碼等
        
        return true;
    }
    
    /**
     * 檢查是否包含Shellcode
     */
    private boolean containsShellcode(ByteBuf packet) {
        if (packet.readableBytes() < SHELLCODE_PATTERN.length) {
            return false;
        }
        
        // 檢查NOP sled
        int nopCount = 0;
        for (int i = 0; i < packet.readableBytes(); i++) {
            if (packet.getByte(i) == (byte)0x90) {
                nopCount++;
                if (nopCount >= 16) { // 連續16個NOP指令
                    return true;
                }
            } else {
                nopCount = 0;
            }
        }
        
        return false;
    }
    
    /**
     * 檢查緩衝區溢出模式
     */
    private boolean containsBufferOverflowPattern(ByteBuf packet) {
        // 檢查重複的模式（如AAAA...）
        if (packet.readableBytes() >= 100) {
            byte firstByte = packet.getByte(0);
            int repeatCount = 0;
            
            for (int i = 0; i < packet.readableBytes(); i++) {
                if (packet.getByte(i) == firstByte) {
                    repeatCount++;
                    if (repeatCount >= 100) {
                        return true;
                    }
                } else {
                    repeatCount = 0;
                    firstByte = packet.getByte(i);
                }
            }
        }
        
        return false;
    }
    
    /**
     * 檢查格式字符串攻擊
     */
    private boolean containsFormatStringAttack(ByteBuf packet) {
        if (!isTextData(packet)) {
            return false;
        }
        
        String content = extractTextContent(packet);
        // 檢查格式字符串特徵
        return content.contains("%x%x%x%x") || 
               content.contains("%n%n%n%n") ||
               content.contains("%s%s%s%s");
    }
    
    /**
     * 判斷是否為文本數據
     */
    private boolean isTextData(ByteBuf packet) {
        int sampleSize = Math.min(100, packet.readableBytes());
        int textCount = 0;
        
        for (int i = 0; i < sampleSize; i++) {
            byte b = packet.getByte(i);
            if ((b >= 0x20 && b <= 0x7E) || b == 0x09 || b == 0x0A || b == 0x0D) {
                textCount++;
            }
        }
        
        return textCount > sampleSize * 0.8; // 80%以上是文本字符
    }
    
    /**
     * 提取文本內容
     */
    private String extractTextContent(ByteBuf packet) {
        int length = Math.min(1000, packet.readableBytes());
        byte[] bytes = new byte[length];
        packet.getBytes(0, bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 驗證結果類
     */
    public static class ValidationResult {
        private boolean valid = false;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private String attackType = null;
        
        public void addError(String error) {
            errors.add(error);
            logger.error("Validation error: {}", error);
        }
        
        public void addWarning(String warning) {
            warnings.add(warning);
            logger.warn("Validation warning: {}", warning);
        }
        
        // Getters and setters
        public boolean isValid() {
            return valid && errors.isEmpty();
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public String getAttackType() {
            return attackType;
        }
        
        public void setAttackType(String attackType) {
            this.attackType = attackType;
        }
    }
}