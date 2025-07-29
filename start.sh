#!/bin/bash

# Java NIO 端口轉發服務器啟動腳本

# 設置JVM參數
JVM_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:+UseStringDeduplication"

# 設置日誌目錄
LOG_DIR="logs"
mkdir -p $LOG_DIR

# 檢查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "8" ]; then
    echo "錯誤: 需要Java 8或更高版本，當前版本: $JAVA_VERSION"
    exit 1
fi

# 檢查配置文件
if [ ! -f "config.properties" ]; then
    echo "錯誤: 找不到配置文件 config.properties"
    exit 1
fi

# 檢查JAR文件
JAR_FILE="target/nio-port-forward-1.0.0.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "錯誤: 找不到JAR文件 $JAR_FILE"
    echo "請先運行: mvn clean package"
    exit 1
fi

echo "啟動Java NIO端口轉發服務器..."
echo "JVM參數: $JVM_OPTS"
echo "日誌目錄: $LOG_DIR"

# 啟動服務器
java $JVM_OPTS -jar $JAR_FILE 2>&1 | tee $LOG_DIR/startup.log