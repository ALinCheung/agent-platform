@echo off
REM Agent Platform Startup Script (Windows)

set JVM_OPTS=-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
set JAR_FILE=agent-platform-starter\target\agent-platform-starter-1.0.0-SNAPSHOT.jar

if not exist "%JAR_FILE%" (
    echo Error: JAR file not found. Please run 'mvn clean package' first.
    exit /b 1
)

echo Starting Agent Platform...
java %JVM_OPTS% -jar "%JAR_FILE%" %*
