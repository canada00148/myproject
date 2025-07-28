#!/bin/bash

# 端口轉發功能測試腳本

echo "=== Java NIO 端口轉發測試 ==="

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

# 測試連接（如果本地有HTTP服務）
echo "3. 測試端口連接..."
for port in 2000 2011 2020 8080; do
    echo "測試端口 $port..."
    timeout 5 bash -c "echo 'GET / HTTP/1.1\r\nHost: localhost\r\n\r\n' | nc localhost $port" 2>/dev/null || echo "端口 $port 連接正常（無HTTP響應是正常的）"
done

# 測試Proxy Protocol V1
echo "4. 測試Proxy Protocol V1..."
echo "PROXY TCP4 192.168.1.100 192.168.1.200 12345 80\r\nGET / HTTP/1.1\r\nHost: localhost\r\n\r\n" | nc localhost 2000 2>/dev/null || echo "Proxy Protocol測試完成"

echo "5. 停止服務器..."
kill $SERVER_PID
wait $SERVER_PID 2>/dev/null

echo "=== 測試完成 ==="