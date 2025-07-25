# TCP Layer 4 抗CC攻擊與封包異常檢測系統

這是一個基於Java 8和Netty的TCP層級（Layer 4）安全防護系統，提供抗CC攻擊和封包異常檢測功能。

## 功能特性

### 1. TCP層級CC攻擊防護
- **連接數限制**：限制單個IP的最大連接數
- **連接速率限制**：限制每秒新建連接數
- **智能限流**：使用令牌桶算法實現平滑限流
- **自動封禁**：自動封禁惡意IP一段時間
- **空閒連接清理**：自動清理長時間空閒的連接

### 2. TCP封包異常檢測
- **封包大小檢查**：檢測異常大小的封包
- **Shellcode檢測**：檢測NOP sled等shellcode特徵
- **緩衝區溢出檢測**：檢測重複模式的溢出攻擊
- **SQL注入檢測**：檢測文本協議中的SQL注入
- **格式字符串攻擊檢測**：檢測%x%x等格式字符串攻擊
- **協議合規性檢查**：可擴展的協議驗證框架

### 3. 高性能設計
- **基於Netty**：使用高性能的NIO框架
- **零拷貝**：減少內存拷貝提升性能
- **管道處理**：模塊化的處理器鏈
- **異步處理**：非阻塞的I/O操作

## 系統要求

- Java 8或更高版本
- Maven 3.6+

## 快速開始

### 1. 編譯項目
```bash
mvn clean package
```

### 2. 運行服務器
```bash
# 使用默認端口8888
java -jar target/tcp-anti-attack-system-1.0-SNAPSHOT.jar

# 指定端口
java -jar target/tcp-anti-attack-system-1.0-SNAPSHOT.jar 9999
```

### 3. 測試連接
```bash
# 使用telnet測試
telnet localhost 8888

# 使用nc (netcat)測試
echo "Hello Server" | nc localhost 8888

# 使用自定義TCP客戶端測試
```

## 配置說明

配置文件位於 `src/main/resources/server.properties`：

```properties
# CC防護設置
cc.protection.enabled=true          # 是否啟用CC防護
cc.max.connections.per.ip=10        # 每個IP最大連接數
cc.max.connections.per.second=5     # 每秒最大新建連接數
cc.ban.duration.seconds=300         # 封禁持續時間

# 封包驗證設置
packet.validation.enabled=true      # 是否啟用封包驗證
packet.max.size=65536              # 最大封包大小(字節)
packet.min.size=1                  # 最小封包大小(字節)
packet.drop.invalid=true           # 是否丟棄無效封包

# 性能設置
server.worker.threads=16           # 工作線程數
server.read.timeout.seconds=30     # 讀超時時間
server.write.timeout.seconds=30    # 寫超時時間

# 限流設置
ratelimit.global.per.second=1000.0 # 全局每秒請求數
ratelimit.per.ip.per.second=100.0  # 每個IP每秒請求數
```

## 架構設計

### 處理器鏈（Pipeline）

```
[TCP Connection]
    ↓
[IdleStateHandler] - 空閒檢測
    ↓
[ConnectionHandler] - 連接管理，CC防護
    ↓
[PacketValidationHandler] - 封包驗證
    ↓
[RateLimitHandler] - 速率限制
    ↓
[BusinessHandler] - 業務處理
    ↓
[ExceptionHandler] - 異常處理
```

### 核心組件

1. **服務層**
   - `ConnectionManager`：管理所有TCP連接
   - `PacketValidator`：驗證封包合法性
   - `RateLimitService`：提供多種限流策略

2. **處理器層**
   - `ConnectionHandler`：處理連接生命週期
   - `PacketValidationHandler`：執行封包驗證
   - `RateLimitHandler`：執行速率限制
   - `BusinessHandler`：處理業務邏輯

3. **模型層**
   - `ConnectionInfo`：存儲連接信息和統計

## 擴展開發

### 1. 自定義業務處理

修改 `BusinessHandler.java` 中的 `processBusinessLogic` 方法：

```java
private ByteBuf processBusinessLogic(ByteBuf input) {
    // 在這裡實現您的協議解析和業務邏輯
    // 例如：解析自定義協議頭、處理命令等
    
    byte[] data = new byte[input.readableBytes()];
    input.readBytes(data);
    
    // 處理數據...
    
    return Unpooled.copiedBuffer(responseData);
}
```

### 2. 添加協議驗證

在 `PacketValidator.java` 中的 `validateProtocolCompliance` 方法添加協議驗證：

```java
private boolean validateProtocolCompliance(ByteBuf packet, ValidationResult result) {
    // 檢查協議頭
    if (packet.readableBytes() < 4) {
        result.addError("Invalid protocol header");
        return false;
    }
    
    // 檢查魔術數字
    int magic = packet.getInt(0);
    if (magic != 0x12345678) {
        result.addError("Invalid magic number");
        return false;
    }
    
    return true;
}
```

### 3. 自定義攻擊檢測

添加新的攻擊模式檢測：

```java
private boolean checkCustomAttack(ByteBuf packet) {
    // 實現自定義的攻擊檢測邏輯
    return false;
}
```

## 監控和日誌

### 日誌文件
- `logs/tcp-anti-attack.log` - 主日誌文件
- `logs/attacks.log` - 攻擊事件專用日誌

### 監控指標
- 活躍連接數
- 每個IP的連接數
- 請求速率
- 攻擊檢測統計

## 性能優化建議

1. **調整線程數**：根據CPU核心數調整worker線程數
2. **優化緩衝區**：根據封包大小調整Netty緩衝區
3. **使用直接內存**：啟用Netty的直接內存緩衝區
4. **批量處理**：在高流量場景下使用批量處理

## 安全建議

1. **定期更新攻擊特徵庫**：保持攻擊檢測模式的更新
2. **監控日誌**：定期檢查攻擊日誌
3. **調整限制參數**：根據實際流量調整限流參數
4. **備份配置**：保存不同場景的配置文件

## 測試工具

### 壓力測試
```bash
# 使用Apache Bench進行TCP壓力測試
# 需要先安裝支持TCP的ab版本

# 使用自定義腳本測試
for i in {1..100}; do
    (echo "Test $i" | nc localhost 8888) &
done
```

### 攻擊模擬
```bash
# 模擬大量連接
for i in {1..20}; do
    nc localhost 8888 &
done

# 發送異常封包
echo -e "\x90\x90\x90\x90\x90\x90\x90\x90" | nc localhost 8888
```

## 故障排除

1. **連接被拒絕**：檢查是否超過IP連接限制
2. **封包被丟棄**：查看attacks.log了解原因
3. **性能問題**：調整worker線程數和緩衝區大小

## License

MIT License