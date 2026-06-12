# TravelMind - 智能行程规划助手

基于 Java 的交互式命令行智能行程规划助手，支持用户用自然语言提出旅行需求，通过可配置大模型和高德地图 API 生成可修改、可保存、可导出的 Markdown 行程。

## 技术栈

| 技术方向 | 选型 | 说明 |
| --- | --- | --- |
| 编程语言 | Java 17 | 语法稳定，适合 Spring Boot 3.x |
| 应用框架 | Spring Boot 3.x | 管理依赖、配置、Bean 和应用生命周期 |
| CLI 交互 | JLine | 实现类似聊天窗口的命令行输入体验 |
| 数据库 | MySQL 8.x | 保存会话、行程、缓存和日志 |
| ORM | MyBatis Plus | 降低 CRUD 开发成本 |
| HTTP 客户端 | OkHttp | 调用大模型和高德地图 API |
| JSON 处理 | Jackson | 处理大模型和地图 API 的 JSON 数据 |
| 环境变量 | dotenv-java | 从 `.env` 读取敏感配置 |
| 大模型 | 小米 MiMo | 作为默认大模型供应商 |
| 地图服务 | 高德地图 API | 查询 POI、地理编码、路线和距离 |
| 导出格式 | Markdown | 实现简单，输出结果清晰可读 |
| 测试框架 | JUnit 5 + Mockito | 单元测试和外部依赖 Mock |

## 功能列表

- 支持类似聊天窗口的 CLI 交互
- 支持用户使用自然语言描述旅行需求
- 支持小米 MiMo 大模型作为默认模型
- 支持通过配置切换不同大模型供应商
- 支持接入高德地图 API 查询 POI 和路线信息
- 支持全国城市的行程规划
- 支持多轮对话修改已有行程
- 支持 MySQL 保存会话、需求、行程、缓存和调用日志
- 支持将最终行程导出为 Markdown 文件
- 支持通过 `.env` 管理敏感配置

## 环境准备

### 1. 安装 JDK 17

确保已安装 Java 17 或更高版本：

```bash
java -version
```

### 2. 安装 Maven

确保已安装 Maven：

```bash
mvn -version
```

### 3. 安装 MySQL 8.x

确保 MySQL 服务已启动并可访问。

## MySQL 初始化

执行数据库初始化脚本：

```bash
mysql -u root -p < sql/schema.sql
```

## 配置 .env

复制 `.env.example` 为 `.env` 并填写真实配置：

```bash
cp .env.example .env
```

编辑 `.env` 文件，填写以下配置：

```env
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=travelmind
MYSQL_USERNAME=root
MYSQL_PASSWORD=your_password

LLM_PROVIDER=mimo
MIMO_BASE_URL=your_mimo_base_url
MIMO_CHAT_PATH=/v1/chat/completions
MIMO_API_KEY=your_mimo_api_key
MIMO_MODEL=your_mimo_model

AMAP_API_KEY=your_amap_api_key

EXPORT_DIR=exports
```

## 启动方式

### 开发模式

```bash
mvn spring-boot:run
```

### 打包运行

```bash
mvn clean package
java -jar target/travelmind-0.0.1-SNAPSHOT.jar
```

## CLI 使用示例

```text
TravelMind > 帮我规划去上海的三日旅游的行程

TravelMind > 第二天不要去博物馆，换成迪士尼

TravelMind > /export

TravelMind > /history

TravelMind > /help

TravelMind > /exit
```

### 支持的命令

| 命令 | 说明 |
| --- | --- |
| `/help` | 查看帮助 |
| `/new` | 开始新行程 |
| `/export` | 导出当前行程为 Markdown |
| `/history` | 查看历史行程 |
| `/exit` | 退出程序 |

## 项目结构

```text
travelmind
├── docs                                    # 文档目录
│   ├── travelmind-system-architecture.md   # 系统架构文档
│   └── travelmind-code-generation-guide.md # 代码生成指南
├── sql
│   └── schema.sql                          # 数据库初始化脚本
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── travelmind
│   │   │           ├── TravelMindApplication.java  # 应用入口
│   │   │           ├── amap                         # 高德地图模块
│   │   │           ├── cli                          # CLI 模块
│   │   │           ├── config                       # 配置模块
│   │   │           ├── conversation                 # 会话管理模块
│   │   │           ├── domain                       # 领域模型
│   │   │           ├── entity                       # 数据库实体
│   │   │           ├── export                       # 导出模块
│   │   │           ├── llm                          # 大模型模块
│   │   │           ├── planner                      # 行程规划模块
│   │   │           ├── repository                   # 数据访问层
│   │   │           └── support                      # 工具类
│   │   └── resources
│   │       ├── application.yml
│   │       └── mapper
│   └── test
│       └── java
│           └── com
│               └── travelmind
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

## 面试亮点

1. **大模型工程化**：不是简单调用大模型，而是将大模型放在可控的工程流程中
2. **清晰分层**：大模型负责理解和生成，Java 系统负责状态、工具、校验和持久化
3. **真实数据约束**：高德地图 API 提供真实地点约束，减少模型幻觉
4. **状态管理**：MySQL 保存多轮会话和历史行程，具备业务闭环
5. **可扩展设计**：通过 LlmClient 抽象支持后续切换模型供应商
6. **安全实践**：使用 `.env` 管理敏感信息，不提交到 Git
7. **可观测性**：使用 `llm_call_log` 记录模型调用，便于调试和问题定位
8. **合理取舍**：行程主体用 JSON 保存，降低数据库复杂度，适合 MVP 快速迭代
9. **交付物**：Markdown 导出让项目有明确可交付结果
