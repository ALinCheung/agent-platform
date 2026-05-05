@echo off
REM Agent Platform 启动脚本 (Windows)

set JVM_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
set JAR_FILE=agent-platform-starter\target\agent-platform-starter-1.0.0-SNAPSHOT.jar

if not exist "%JAR_FILE%" (
    echo 错误: JAR文件不存在，请先执行 mvn clean package
    exit /b 1
)

echo 启动 Agent Platform...
java %JVM_OPTS% -jar "%JAR_FILE%" %*
