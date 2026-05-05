#!/bin/bash
# Agent Platform 启动脚本 (Linux/macOS)

JVM_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JAR_FILE="agent-platform-starter/target/agent-platform-starter-1.0.0-SNAPSHOT.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "错误: JAR文件不存在，请先执行 mvn clean package"
    exit 1
fi

echo "启动 Agent Platform..."
java $JVM_OPTS -jar "$JAR_FILE" "$@"
