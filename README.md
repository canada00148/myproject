# Java NIO 端口轉發服務器

一個功能強大的Java NIO端口轉發服務器，支持多重端口映射、Proxy Protocol V1解析、抗CC攻擊等安全功能。

## 功能特性

### 端口映射
- 支持單個端口：`2000`
- 支持端口範圍：`2011-2020`
- 支持混合配置：`2000,2011-2020,8080`
- **1:1端口映射**：監聽端口與遠程端口一一對應
- 自動去重和排序

### Proxy Protocol V1 支持
- 自動檢測和解析Proxy Protocol V1頭部
- 提取真實客戶端IP地址
- 支持TCP4和TCP6協議

### 安全防護
- **抗CC攻擊**：限制每IP最大連接數和連接速率
- **封包大小檢查**：丟棄異常大小的封包
- **TCP Ping檢測**：檢測和防禦TCP層ping攻擊
- **連接超時管理**：自動清理空閒連接

### 高性能
- 基於Java NIO的非阻塞I/O
- 多線程處理連接
- 高效的緩衝區管理
- 支持大量並發連接

## 快速開始

### 1. 編譯項目

```bash
mvn clean package
```

### 2. 配置服務器

編輯 `config.properties` 文件：

```properties
# 端口映射配置
port.mappings=2000,2011-2020,8080

# 遠程服務器配置
remote.host=127.0.0.1
remote.port=80

# 安全配置
security.max_packet_size=8192
security.min_packet_size=1
security.max_connections_per_ip=100
security.connection_rate_limit=10
security.rate_limit_window=60000
security.tcp_ping_detection=true
security.tcp_ping_timeout=5000
security.max_idle_time=300000
```

### 3. 運行服務器

```bash
java -jar target/nio-port-forward-1.0.0.jar
```

或者使用Maven運行：

```bash
mvn exec:java -Dexec.mainClass="com.portforward.Main"
```

## 配置說明

### 端口映射格式

| 格式 | 示例 | 說明 |
|------|------|------|
| 單個端口 | `2000` | 監聽單個端口 |
| 端口範圍 | `2011-2020` | 監聽端口範圍 |
| 混合配置 | `2000,2011-2020,8080` | 支持多種格式混合 |

### 安全配置參數

| 參數 | 默認值 | 說明 |
|------|--------|------|
| `security.max_packet_size` | 8192 | 最大封包大小（字節） |
| `security.min_packet_size` | 1 | 最小封包大小（字節） |
| `security.max_connections_per_ip` | 100 | 每IP最大連接數 |
| `security.connection_rate_limit` | 10 | 連接速率限制（次/窗口） |
| `security.rate_limit_window` | 60000 | 速率限制窗口（毫秒） |
| `security.tcp_ping_detection` | true | 啟用TCP ping檢測 |
| `security.tcp_ping_timeout` | 5000 | TCP ping超時時間（毫秒） |
| `security.max_idle_time` | 300000 | 最大空閒時間（毫秒） |

## 架構設計

### 核心組件

1. **Config** - 配置管理
   - 解析properties文件
   - 端口映射解析
   - 安全參數管理

2. **ProxyProtocolParser** - Proxy Protocol解析
   - 檢測Proxy Protocol V1
   - 解析客戶端真實IP
   - 跳過協議頭部

3. **SecurityManager** - 安全管理
   - 連接數量限制
   - 速率限制
   - 封包大小檢查
   - TCP ping檢測

4. **ConnectionHandler** - 連接處理
   - 數據轉發
   - 連接生命週期管理
   - 超時檢測

5. **PortForwardServer** - 服務器主體
   - 端口監聽
   - 連接接受
   - 事件循環

### 工作流程

1. 服務器啟動，綁定配置的端口
2. 客戶端連接，進行安全檢查
3. 解析Proxy Protocol（如果存在）
4. 建立到遠程服務器的連接
5. 創建連接處理器進行數據轉發
6. 監控連接狀態，處理異常情況

## 日誌配置

服務器使用SLF4J + Logback進行日誌記錄，配置文件位於 `src/main/resources/logback.xml`。

日誌級別可以在配置文件中調整：

```xml
<root level="INFO">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</root>
```

## 性能優化

### 系統調優建議

1. **增加文件描述符限制**
   ```bash
   ulimit -n 65536
   ```

2. **調整TCP參數**
   ```bash
   # 增加TCP緩衝區大小
   sysctl -w net.core.rmem_max=16777216
   sysctl -w net.core.wmem_max=16777216
   ```

3. **JVM調優**
   ```bash
   java -Xms2g -Xmx2g -XX:+UseG1GC -jar nio-port-forward-1.0.0.jar
   ```

## 故障排除

### 常見問題

1. **端口綁定失敗**
   - 檢查端口是否被佔用
   - 確認有足夠權限綁定端口

2. **連接被拒絕**
   - 檢查遠程服務器是否可達
   - 確認防火牆設置

3. **性能問題**
   - 調整緩衝區大小
   - 增加線程池大小
   - 檢查系統資源使用情況

### 調試模式

啟用DEBUG日誌級別以獲取詳細信息：

```xml
<root level="DEBUG">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</root>
```

## 開發

### 項目結構

```
src/main/java/com/portforward/
├── config/          # 配置管理
├── protocol/        # 協議解析
├── security/        # 安全防護
├── handler/         # 連接處理
├── server/          # 服務器主體
└── Main.java        # 主程序
```

### 構建依賴

- Java 8+
- Maven 3.6+
- SLF4J + Logback

## 許可證

MIT License

## 貢獻

歡迎提交Issue和Pull Request！