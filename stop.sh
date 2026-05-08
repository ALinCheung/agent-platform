#!/bin/bash
# 关闭占用8080端口的进程 (Linux/macOS)

PID=$(lsof -ti:8080)
if [ -n "$PID" ]; then
    echo "关闭进程 PID: $PID"
    kill -9 $PID
else
    echo "未找到占用8080端口的进程"
fi