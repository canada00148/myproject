#!/bin/bash

# 端口映射驗證測試腳本

echo "=== 端口映射驗證測試 ==="

# 測試1: 正確的端口映射
echo "1. 測試正確的端口映射配置..."
cp config.properties config_backup.properties
java -jar target/nio-port-forward-1.0.0.jar &
PID1=$!
sleep 3

if kill -0 $PID1 2>/dev/null; then
    echo "✅ 正確配置啟動成功"
    kill $PID1
    sleep 2
else
    echo "❌ 正確配置啟動失敗"
fi

# 測試2: 端口數量不一致
echo "2. 測試端口數量不一致的錯誤處理..."
cp config_error_test.properties config.properties
java -jar target/nio-port-forward-1.0.0.jar 2>&1 | grep -q "數量不一致"
if [ $? -eq 0 ]; then
    echo "✅ 端口數量不一致檢查正常"
else
    echo "❌ 端口數量不一致檢查失敗"
fi

# 測試3: 端口格式錯誤
echo "3. 測試端口格式錯誤..."
echo "port.mappings=8080=8080,2011" > config.properties
java -jar target/nio-port-forward-1.0.0.jar 2>&1 | grep -q "格式錯誤"
if [ $? -eq 0 ]; then
    echo "✅ 端口格式錯誤檢查正常"
else
    echo "❌ 端口格式錯誤檢查失敗"
fi

# 測試4: 監聽端口重複
echo "4. 測試監聽端口重複..."
echo "port.mappings=8080=8080,8080=8081" > config.properties
java -jar target/nio-port-forward-1.0.0.jar 2>&1 | grep -q "重複"
if [ $? -eq 0 ]; then
    echo "✅ 監聽端口重複檢查正常"
else
    echo "❌ 監聽端口重複檢查失敗"
fi

# 恢復原始配置
cp config_backup.properties config.properties
rm config_backup.properties

echo "=== 測試完成 ==="