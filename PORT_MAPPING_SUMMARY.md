# 端口映射功能總結

## 🎯 實現目標

根據您的要求，我們實現了以下功能：

1. **Java 8 語法兼容性** ✅
2. **1:1端口映射** ✅
3. **保持配置文件順序** ✅
4. **端口數量一致性檢查** ✅

## 📋 核心功能

### 1. 端口映射格式

**新格式：** `監聽端口=遠程端口`

```properties
# 正確格式示例
port.mappings=8080=8080,2011=2011,2012=2012,2013=2013,2014=2014,2015=2015,2016=2016,2017=2017,2018=2018,2019=2019,2020=2020
```

### 2. 順序保持

**重要特性：** 完全按照配置文件中的順序，不進行任何排序

```properties
# 配置文件順序
port.mappings=2020=2020,2019=2019,2018=2018,2017=2017,2016=2016,2015=2015,2014=2014,2013=2013,2012=2012,2011=2011,8080=8080

# 實際監聽順序（完全一致）
監聽端口: [2020, 2019, 2018, 2017, 2016, 2015, 2014, 2013, 2012, 2011, 8080]
遠程端口: [2020, 2019, 2018, 2017, 2016, 2015, 2014, 2013, 2012, 2011, 8080]
```

### 3. 數量一致性檢查

**檢查機制：** 確保監聽端口和遠程端口數量完全一致

```java
// 檢查端口數量一致性
if (listenPorts.size() != remotePorts.size()) {
    throw new IllegalArgumentException("監聽端口和遠程端口數量不一致: " + 
        "監聽端口=" + listenPorts.size() + ", 遠程端口=" + remotePorts.size());
}
```

## 🔧 技術實現

### 配置解析

```java
private void parsePortMappings(String portMappings) {
    listenPorts = new ArrayList<>();
    remotePorts = new ArrayList<>();
    
    String[] mappings = portMappings.split(",");
    
    for (String mapping : mappings) {
        // 解析 監聽端口=遠程端口 格式
        String[] parts = mapping.split("=");
        if (parts.length != 2) {
            throw new IllegalArgumentException("端口映射格式錯誤: " + mapping);
        }
        
        int listenPort = parseSinglePort(parts[0].trim());
        int remotePort = parseSinglePort(parts[1].trim());
        
        // 按照配置文件順序添加，不進行任何排序
        listenPorts.add(listenPort);
        remotePorts.add(remotePort);
    }
    
    // 只檢查端口數量一致性
    if (listenPorts.size() != remotePorts.size()) {
        throw new IllegalArgumentException("監聽端口和遠程端口數量不一致");
    }
}
```

### 端口映射查找

```java
private int getRemotePort(int listenPort) {
    List<Integer> listenPorts = config.getListenPorts();
    List<Integer> remotePorts = config.getRemotePorts();
    
    for (int i = 0; i < listenPorts.size(); i++) {
        if (listenPorts.get(i) == listenPort) {
            return remotePorts.get(i);
        }
    }
    return -1; // 找不到對應的遠程端口
}
```

## ✅ 驗證結果

### 測試1: 順序保持
```properties
配置: 2020=2020,2019=2019,2018=2018,...,8080=8080
結果: [2020, 2019, 2018, 2017, 2016, 2015, 2014, 2013, 2012, 2011, 8080]
```

### 測試2: 數量一致性
- ✅ 正確配置：11個監聽端口 = 11個遠程端口
- ✅ 錯誤配置：會拋出異常並停止啟動

### 測試3: 1:1映射
- ✅ 監聽端口8080 → 遠程端口8080
- ✅ 監聽端口2011 → 遠程端口2011
- ✅ 監聽端口2020 → 遠程端口2020

## 🚀 使用方法

### 1. 配置端口映射
```properties
# 基本配置
port.mappings=8080=8080,2011=2011,2012=2012

# 複雜配置
port.mappings=2020=2020,2019=2019,2018=2018,2017=2017,2016=2016,2015=2015,2014=2014,2013=2013,2012=2012,2011=2011,8080=8080
```

### 2. 啟動服務器
```bash
java -jar target/nio-port-forward-1.0.0.jar
```

### 3. 查看映射結果
```
監聽端口: [8080, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020]
遠程端口: [8080, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020]
```

## ⚠️ 注意事項

1. **順序重要**：配置文件中的順序就是實際映射順序
2. **數量必須一致**：監聽端口和遠程端口數量必須完全相同
3. **格式嚴格**：必須使用 `監聽端口=遠程端口` 格式
4. **端口範圍**：端口號必須在1-65535範圍內

## 🎉 總結

現在您的Java NIO端口轉發程序完全符合要求：

- ✅ **Java 8語法**：完全兼容Java 8
- ✅ **1:1端口映射**：監聽端口與遠程端口一一對應
- ✅ **保持順序**：完全按照配置文件順序，不排序
- ✅ **數量檢查**：確保監聽端口和遠程端口數量一致
- ✅ **錯誤處理**：配置錯誤時會給出明確的錯誤信息

端口映射現在完全按照您的設定工作，不會有任何順序錯亂的問題！