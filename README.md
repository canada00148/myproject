# Java 抗CC攻擊與封包異常檢測系統

這是一個基於Spring Boot的安全防護系統，提供抗CC攻擊和封包異常檢測功能。

## 功能特性

### 1. 抗CC攻擊
- **多層次限流**：支持秒級、分鐘級限流
- **多維度限流**：基於IP、用戶、URI等多個維度
- **多種算法**：支持計數器和令牌桶算法
- **分佈式支持**：可選Redis實現分佈式限流
- **註解支持**：通過`@RateLimit`註解靈活配置限流規則

### 2. 封包異常檢測
- **SQL注入檢測**：檢測常見的SQL注入模式
- **XSS攻擊檢測**：檢測跨站腳本攻擊
- **路徑遍歷檢測**：防止目錄遍歷攻擊
- **請求頭驗證**：檢查異常請求頭
- **請求大小限制**：防止大文件攻擊
- **User-Agent檢測**：識別可疑的爬蟲和掃描器

## 快速開始

### 環境要求
- Java 11+
- Maven 3.6+
- Redis（可選，用於分佈式限流）

### 安裝運行

1. 克隆項目
```bash
git clone <repository-url>
cd anti-attack-system
```

2. 編譯項目
```bash
mvn clean package
```

3. 運行應用
```bash
java -jar target/anti-attack-system-1.0-SNAPSHOT.jar
```

或使用Maven直接運行：
```bash
mvn spring-boot:run
```

## 使用方法

### 1. 全局保護
所有請求都會自動經過過濾器保護，無需額外配置。

### 2. 使用限流註解
```java
@RestController
public class YourController {
    
    // 每個IP每分鐘最多10次請求
    @GetMapping("/api/limited")
    @RateLimit(maxRequests = 10, timeWindow = 60, limitByIp = true)
    public String limitedEndpoint() {
        return "Success";
    }
    
    // 基於用戶限流，每小時100次
    @GetMapping("/api/user-limited")
    @RateLimit(maxRequests = 100, timeWindow = 3600, limitByUser = true)
    public String userLimitedEndpoint() {
        return "Success";
    }
}
```

### 3. 配置說明

在`application.yml`中配置：

```yaml
security:
  # CC攻擊防護配置
  cc:
    enabled: true                    # 是否啟用CC防護
    max-requests-per-minute: 60      # 每分鐘最大請求數
    max-requests-per-second: 10      # 每秒最大請求數
  
  # 封包驗證配置
  packet-validation:
    enabled: true                    # 是否啟用封包驗證
    block-on-error: true            # 檢測到異常是否阻斷請求
```

## API測試

### 測試正常請求
```bash
curl http://localhost:8080/api/hello
```

### 測試限流
```bash
# 快速發送多個請求測試限流
for i in {1..20}; do curl http://localhost:8080/api/limited; done
```

### 測試SQL注入檢測
```bash
# 這個請求會被阻斷
curl "http://localhost:8080/api/test-validation?param1=test' OR '1'='1"
```

### 測試XSS檢測
```bash
# 這個請求會被阻斷
curl "http://localhost:8080/api/test-validation?param1=<script>alert('xss')</script>"
```

## 架構設計

### 核心組件

1. **過濾器層**
   - `AntiCCFilter`：CC攻擊防護過濾器
   - `PacketValidationFilter`：封包驗證過濾器

2. **服務層**
   - `RateLimitService`：限流服務
   - `PacketValidationService`：封包驗證服務

3. **切面層**
   - `RateLimitAspect`：限流註解切面

4. **工具類**
   - `IpUtils`：IP地址處理工具

## 擴展性

### 添加新的驗證規則
在`PacketValidationService`中添加新的驗證方法：

```java
private void validateCustomRule(HttpServletRequest request, ValidationResult result) {
    // 自定義驗證邏輯
}
```

### 自定義限流策略
實現自定義的限流算法或策略：

```java
@Service
public class CustomRateLimitService extends RateLimitService {
    // 實現自定義限流邏輯
}
```

## 性能優化建議

1. **使用Redis**：在分佈式環境下使用Redis進行限流
2. **調整緩存大小**：根據實際流量調整本地緩存大小
3. **異步日誌**：使用異步日誌減少I/O開銷
4. **合理設置限流閾值**：根據服務器性能設置合適的限流值

## 監控和日誌

系統會記錄所有的攻擊嘗試和異常請求：
- 日誌文件位置：`logs/anti-attack.log`
- 可以集成ELK或其他日誌分析系統進行監控

## 注意事項

1. 在生產環境中，建議使用Redis進行分佈式限流
2. 根據實際業務調整限流閾值
3. 定期檢查日誌，分析攻擊模式
4. 保持攻擊模式庫的更新

## License

MIT License