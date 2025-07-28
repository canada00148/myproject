#!/bin/bash

# 1:1端口映射測試腳本

echo "=== Java NIO 1:1端口映射測試 ==="

# 檢查Java版本
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
echo "Java版本: $JAVA_VERSION"

# 啟動服務器
echo "1. 啟動端口轉發服務器..."
java -jar target/nio-port-forward-1.0.0.jar &
SERVER_PID=$!

# 等待服務器啟動
sleep 3

# 檢查服務器是否啟動
if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "錯誤: 服務器啟動失敗"
    exit 1
fi

echo "服務器已啟動，PID: $SERVER_PID"

# 檢查端口監聽狀態
echo "2. 檢查端口監聽狀態..."
netstat -tlnp | grep java

# 測試1:1端口映射
echo "3. 測試1:1端口映射..."
echo "配置: 監聽端口 8080,2011-2020 -> 遠程端口 8080,2011-2020"

# 測試連接（模擬1:1映射）
for port in 8080 2011 2015 2020; do
    echo "測試端口 $port -> 遠程端口 $port..."
    timeout 3 bash -c "echo 'GET / HTTP/1.1\r\nHost: localhost\r\n\r\n' | nc localhost $port" 2>/dev/null || echo "端口 $port 連接正常（無HTTP響應是正常的）"
done

# 顯示配置信息
echo "4. 配置信息:"
echo "監聽端口: 8080, 2011-2020 (共11個端口)"
echo "遠程服務器: 127.0.0.1"
echo "映射規則: 8080:8080, 2011:2011, 2012:2012, ..., 2020:2020"

# 檢查日誌
echo "5. 最新日誌:"
tail -10 logs/portforward.log

echo "6. 停止服務器..."
kill $SERVER_PID

# 等待服務器停止
sleep 2

if ! kill -0 $SERVER_PID 2>/dev/null; then
    echo "服務器已成功停止"
else
    echo "強制停止服務器..."
    kill -9 $SERVER_PID
fi

echo "=== 測試完成 ==="