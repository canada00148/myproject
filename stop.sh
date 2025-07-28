#!/bin/bash

# Java NIO 端口轉發服務器停止腳本

echo "正在停止Java NIO端口轉發服務器..."

# 查找Java進程
PID=$(ps -ef | grep "nio-port-forward" | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "未找到運行中的端口轉發服務器進程"
    exit 0
fi

echo "找到進程ID: $PID"

# 發送SIGTERM信號
kill -TERM $PID

# 等待進程結束
for i in {1..30}; do
    if ! kill -0 $PID 2>/dev/null; then
        echo "服務器已成功停止"
        exit 0
    fi
    sleep 1
done

# 如果進程仍在運行，強制終止
echo "服務器未在30秒內停止，強制終止..."
kill -KILL $PID

if ! kill -0 $PID 2>/dev/null; then
    echo "服務器已強制停止"
else
    echo "無法停止服務器"
    exit 1
fi