# Java NIO 端口轉發服務器使用說明

## 概述

這是一個功能強大的Java NIO端口轉發服務器，支持多重端口映射、Proxy Protocol V1解析、抗CC攻擊等安全功能。

## 主要功能

### 1. 多重端口映射
支持多種端口配置格式：
- 單個端口：`2000`
- 端口範圍：`2011-2020`
- 混合配置：`2000,2011-2020,8080`

### 2. Proxy Protocol V1 支持
- 自動檢測和解析Proxy Protocol V1頭部
- 提取真實客戶端IP地址
- 支持TCP4和TCP6協議

### 3. 安全防護功能
- **抗CC攻擊**：限制每IP最大連接數和連接速率
- **封包大小檢查**：丟棄異常大小的封包
- **TCP Ping檢測**：檢測和防禦TCP層ping攻擊
- **連接超時管理**：自動清理空閒連接

## 安裝和配置

### 1. 環境要求
- Java 8 或更高版本
- Maven 3.6 或更高版本

### 2. 編譯項目
```bash
mvn clean package
```

### 3. 配置文件
編輯 `config.properties` 文件：

```properties
# 端口映射配置
listen.ports=2000,2011-2020,8080

# 遠程服務器配置
remote.host=127.0.0.1
# 1:1端口映射：監聽端口對應遠程端口

# 安全配置
security.max_packet_size=8192
security.min_packet_size=1
security.connection_timeout=30000
security.read_timeout=10000
security.write_timeout=10000

# 抗CC攻擊配置
security.max_connections_per_ip=100
security.connection_rate_limit=10
security.rate_limit_window=60000

# TCP層抗攻擊配置
security.tcp_ping_detection=true
security.tcp_ping_timeout=5000
security.max_idle_time=300000

# 日誌配置
logging.level=INFO
logging.file=portforward.log
```

## 運行方式

### 1. 直接運行
```bash
java -jar target/nio-port-forward-1.0.0.jar
```

### 2. 使用啟動腳本
```bash
./start.sh
```

### 3. 停止服務器
```bash
./stop.sh
```

## 配置參數說明

### 端口映射配置
- `port.mappings`: 本地監聽端口配置
  - 支持單個端口：`2000`
  - 支持端口範圍：`2011-2020`
  - 支持混合配置：`2000,2011-2020,8080`

### 遠程服務器配置
- `remote.host`: 遠程服務器IP地址
- `remote.port`: 遠程服務器端口

### 安全配置
- `security.max_packet_size`: 最大封包大小（字節）
- `security.min_packet_size`: 最小封包大小（字節）
- `security.connection_timeout`: 連接超時時間（毫秒）
- `security.read_timeout`: 讀取超時時間（毫秒）
- `security.write_timeout`: 寫入超時時間（毫秒）

### 抗CC攻擊配置
- `security.max_connections_per_ip`: 每IP最大連接數
- `security.connection_rate_limit`: 連接速率限制（每秒）
- `security.rate_limit_window`: 速率限制窗口時間（毫秒）

### TCP層抗攻擊配置
- `security.tcp_ping_detection`: 是否啟用TCP ping檢測
- `security.tcp_ping_timeout`: TCP ping超時時間（毫秒）
- `security.max_idle_time`: 最大空閒時間（毫秒）

## 日誌和監控

### 日誌文件
- 默認日誌文件：`logs/portforward.log`
- 日誌輪轉：按天輪轉，保留30天

### 日誌級別
- `INFO`: 一般信息
- `WARN`: 警告信息
- `ERROR`: 錯誤信息
- `DEBUG`: 調試信息

## 性能優化

### JVM參數建議
```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication -jar target/nio-port-forward-1.0.0.jar
```

### 系統參數調整
```bash
# 增加文件描述符限制
ulimit -n 65536

# 調整TCP參數
echo 'net.core.somaxconn = 65535' >> /etc/sysctl.conf
echo 'net.ipv4.tcp_max_syn_backlog = 65535' >> /etc/sysctl.conf
sysctl -p
```

## 故障排除

### 常見問題

1. **端口被佔用**
   ```
   錯誤: Address already in use
   解決方案: 檢查端口是否被其他程序佔用，或修改配置文件中的端口
   ```

2. **連接超時**
   ```
   錯誤: Connection timeout
   解決方案: 檢查遠程服務器是否可達，或調整超時參數
   ```

3. **內存不足**
   ```
   錯誤: OutOfMemoryError
   解決方案: 增加JVM堆內存大小
   ```

### 調試模式
設置日誌級別為DEBUG以獲取詳細信息：
```properties
logging.level=DEBUG
```

## 安全建議

1. **防火牆配置**
   - 只開放必要的端口
   - 限制訪問IP範圍

2. **監控和告警**
   - 監控連接數和流量
   - 設置異常告警

3. **定期維護**
   - 定期檢查日誌
   - 更新安全配置

## 技術架構

### 核心組件
- `Config`: 配置管理
- `PortForwardServer`: 主服務器
- `ConnectionHandler`: 連接處理
- `SecurityManager`: 安全管理
- `ProxyProtocolParser`: Proxy Protocol解析

### 技術特點
- 基於Java NIO的非阻塞I/O
- 多線程處理連接
- 高效的緩衝區管理
- 支持大量並發連接

## 版本歷史

- v1.0.0: 初始版本，支持基本端口轉發和安全功能