# Agent Platform

AI自动化平台 - 统一管理和调度Claude命令执行

## 功能特性

- **任务管理**: 创建、编辑、删除、启用/禁用自动化任务
- **Cron调度**: 基于Cron表达式的时间驱动调度
- **Webhook触发**: 通过HTTP请求触发任务执行
- **Claude CLI集成**: 自动化执行Claude命令并捕获输出
- **执行历史**: 完整的执行记录和日志追踪
- **任务回滚**: 支持配置版本管理和回滚
- **自动重试**: 失败任务自动重试机制
- **统计分析**: 任务统计、执行统计、性能分析
- **桌面端Web界面**: 可视化的管理界面

## 技术栈

- **后端**: Java 17, Spring Boot 3, MyBatis-Plus
- **数据库**: SQLite (WAL模式)
- **前端**: Thymeleaf + Alpine.js + TailwindCSS
- **构建**: Maven

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- Claude CLI 已安装并配置

### 构建

```bash
mvn clean package
```

### 启动

**Linux/macOS:**
```bash
./start.sh
```

**Windows:**
```cmd
start.bat
```

**直接运行JAR:**
```bash
java -Xms512m -Xmx1g -jar agent-platform-starter/target/agent-platform-starter-1.0.0-SNAPSHOT.jar
```

### 访问

- 管理界面: http://localhost:8080
- API文档: http://localhost:8080/api/tasks

## 配置说明

### JVM参数

```
-Xms512m          # 初始堆内存512MB
-Xmx1g            # 最大堆内存1GB
-XX:+UseG1GC      # G1垃圾收集器
-XX:MaxGCPauseMillis=200  # GC暂停时间200ms
```

### application.yml配置

```yaml
app:
  data-dir: ./data              # 数据目录
  execution:
    max-concurrent: 10          # 最大并发执行数
    default-timeout-seconds: 300 # 默认超时时间
  claude:
    cli-path: claude            # Claude CLI路径
    check-interval-minutes: 5   # CLI检查间隔
```

## API接口

### 任务管理
- `GET /api/tasks` - 获取所有任务
- `POST /api/tasks` - 创建任务
- `PUT /api/tasks/{id}` - 更新任务
- `DELETE /api/tasks/{id}` - 删除任务
- `POST /api/tasks/{id}/enable` - 启用任务
- `POST /api/tasks/{id}/disable` - 禁用任务
- `POST /api/tasks/{id}/execute` - 手动执行

### 版本管理
- `GET /api/tasks/{id}/versions` - 版本历史
- `POST /api/tasks/{id}/rollback?version=N` - 回滚

### 执行管理
- `GET /api/executions` - 执行历史
- `GET /api/executions/{id}` - 执行详情
- `POST /api/executions/{id}/retry` - 重试

### 统计分析
- `GET /api/stats/overview` - 总览统计
- `GET /api/stats/tasks` - 任务统计
- `GET /api/stats/executions` - 执行统计
- `GET /api/stats/history?days=7` - 历史趋势
- `GET /api/stats/performance` - 性能统计

### 系统管理
- `GET /api/system/health` - 健康检查
- `GET /api/system/resources` - 资源监控
- `POST /api/system/backup` - 数据备份

### Webhook
- `POST /webhook/{path}` - Webhook触发

## 数据目录

```
data/
└── agent-platform.db    # SQLite数据库文件
logs/
└── agent-platform.log   # 应用日志
```

## 测试

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定模块测试
mvn test -pl agent-platform-core
mvn test -pl agent-platform-executor
mvn test -pl agent-platform-web
mvn test -pl agent-platform-starter

# 运行性能测试
mvn test -pl agent-platform-starter -Dtest="com.agentplatform.starter.performance.*"

# 运行端到端测试
mvn test -pl agent-platform-starter -Dtest="com.agentplatform.starter.e2e.*"

# 生成测试覆盖率报告
mvn test jacoco:report
```

### 测试覆盖率

测试覆盖率报告生成在各模块的 `target/site/jacoco/index.html`。

### 测试结构

```
src/test/java/
├── com/agentplatform/core/          # 核心模块测试
│   ├── mapper/                      # Mapper单元测试
│   └── service/impl/                # 服务层单元测试
├── com/agentplatform/executor/      # 执行器模块测试
│   ├── handler/                     # 处理器测试
│   ├── monitor/                     # 监控测试
│   └── service/                     # 服务测试
├── com/agentplatform/web/           # Web模块测试
│   └── controller/                  # 控制器集成测试
└── com/agentplatform/starter/       # 启动器模块测试
    ├── e2e/                         # 端到端测试
    └── performance/                 # 性能测试
```

### 测试环境配置

- **单元测试**: 使用H2内存数据库，无需外部依赖
- **集成测试**: 使用H2内存数据库 + Spring Boot测试上下文
- **端到端测试**: 使用完整Spring Boot应用 + TestRestTemplate
- **性能测试**: 测试并发、内存、磁盘、API响应时间

## 常见问题

**Q: Claude CLI不可用怎么办？**
A: 确保Claude CLI已安装并在PATH中，运行 `claude --version` 检查。

**Q: 如何修改端口？**
A: 在application.yml中修改 `server.port` 配置。

**Q: 数据库在哪里？**
A: 默认在 `./data/agent-platform.db`，可通过 `app.data-dir` 配置修改。
