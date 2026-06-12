# travelmind 代码生成实施说明

## 1. 文档目的

本文档是 `travelmind` 项目的代码生成实施说明，面向后续开发者或另一个 AI 编码助手。它的目标是把系统架构设计进一步落到可执行层面，明确项目结构、实现顺序、类与方法签名、数据库脚本、配置文件、Prompt 模板、外部 API 适配方式、测试要求和验收标准。

如果另一个 AI 需要根据项目设计生成代码，应优先遵守本文档，而不是自由发挥。

## 2. 项目最终范围

### 2.1 项目名称

```text
travelmind
```

### 2.2 项目类型

```text
Java CLI 智能行程规划项目
```

### 2.3 MVP 功能

必须实现：

- Spring Boot 3.x 项目骨架。
- JLine 交互式命令行聊天窗口。
- `.env` 读取敏感配置。
- MySQL 数据库连接。
- 5 张核心表初始化 SQL。
- 小米 MiMo 大模型适配器。
- 可配置 LLM Provider 抽象。
- 高德地图 API 适配器。
- 自然语言意图解析。
- 行程生成。
- 多轮行程修改。
- 当前会话状态管理。
- Markdown 导出。
- 基础规则校验。
- 核心单元测试。

暂不实现：

- Web 前端。
- 用户登录。
- 权限系统。
- PDF 导出。
- Word 导出。
- 酒店和机票实时比价。
- 支付订单。
- 复杂推荐模型。

## 3. 技术栈约束

必须使用：

```text
Java 17
Spring Boot 3.x
Maven
JLine
MyBatis Plus
MySQL 8.x
Jackson
OkHttp
dotenv-java
JUnit 5
Mockito
```

推荐 Maven 坐标：

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.0</spring-boot.version>
</properties>
```

依赖方向：

- `spring-boot-starter`
- `spring-boot-starter-validation`
- `mybatis-plus-boot-starter`
- `mysql-connector-j`
- `jline`
- `okhttp`
- `jackson-databind`
- `dotenv-java`
- `lombok`
- `spring-boot-starter-test`
- `mockito-core`

## 4. 代码生成总原则

生成代码时必须遵守：

- 不要把 API Key、数据库密码写死在代码中。
- 不要提交真实 `.env`。
- 只提交 `.env.example`。
- 所有外部调用都必须有接口抽象，便于 Mock 测试。
- 大模型调用失败不能导致程序直接崩溃。
- 高德地图调用失败时允许降级，但要明确提示用户。
- 行程主体可以用 JSON 存储，不要把 DayPlan、Activity 拆成过多数据库表。
- CLI 需要能持续循环读取输入，直到用户输入 `/exit`。
- `/export` 必须基于当前会话的最新行程导出 Markdown。
- 单元测试中不要真实调用 MiMo 或高德地图 API。

## 5. 推荐目录结构

生成代码时按以下结构创建：

```text
travelmind
├── docs
│   ├── travelmind-system-architecture.md
│   └── travelmind-code-generation-guide.md
├── sql
│   └── schema.sql
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── travelmind
│   │   │           ├── TravelMindApplication.java
│   │   │           ├── amap
│   │   │           │   ├── AmapClient.java
│   │   │           │   ├── AmapProperties.java
│   │   │           │   ├── AmapService.java
│   │   │           │   ├── dto
│   │   │           │   │   ├── AmapPoi.java
│   │   │           │   │   └── AmapRoute.java
│   │   │           │   └── impl
│   │   │           │       └── AmapServiceImpl.java
│   │   │           ├── cli
│   │   │           │   ├── CliRenderer.java
│   │   │           │   ├── CommandRouter.java
│   │   │           │   ├── ShellCommand.java
│   │   │           │   └── TravelMindShell.java
│   │   │           ├── config
│   │   │           │   ├── DotenvInitializer.java
│   │   │           │   ├── JacksonConfig.java
│   │   │           │   └── MybatisPlusConfig.java
│   │   │           ├── conversation
│   │   │           │   ├── ConversationContext.java
│   │   │           │   ├── ConversationIntent.java
│   │   │           │   ├── ConversationManager.java
│   │   │           │   └── UserMessageClassifier.java
│   │   │           ├── domain
│   │   │           │   ├── Activity.java
│   │   │           │   ├── DayPlan.java
│   │   │           │   ├── Itinerary.java
│   │   │           │   ├── Poi.java
│   │   │           │   ├── RouteInfo.java
│   │   │           │   └── TripRequest.java
│   │   │           ├── entity
│   │   │           │   ├── ItineraryEntity.java
│   │   │           │   ├── LlmCallLogEntity.java
│   │   │           │   ├── PoiCacheEntity.java
│   │   │           │   ├── TravelSessionEntity.java
│   │   │           │   └── TripRequestEntity.java
│   │   │           ├── export
│   │   │           │   ├── ExportProperties.java
│   │   │           │   └── MarkdownExporter.java
│   │   │           ├── llm
│   │   │           │   ├── LlmClient.java
│   │   │           │   ├── LlmClientFactory.java
│   │   │           │   ├── LlmProperties.java
│   │   │           │   ├── LlmRequest.java
│   │   │           │   ├── LlmResponse.java
│   │   │           │   ├── PromptTemplates.java
│   │   │           │   └── mimo
│   │   │           │       └── MimoLlmClient.java
│   │   │           ├── planner
│   │   │           │   ├── CandidatePoiBuilder.java
│   │   │           │   ├── IntentParser.java
│   │   │           │   ├── ItineraryGenerator.java
│   │   │           │   ├── RuleValidator.java
│   │   │           │   ├── TripContext.java
│   │   │           │   ├── TripContextBuilder.java
│   │   │           │   └── TripPlannerService.java
│   │   │           ├── repository
│   │   │           │   ├── ItineraryMapper.java
│   │   │           │   ├── LlmCallLogMapper.java
│   │   │           │   ├── PoiCacheMapper.java
│   │   │           │   ├── TravelSessionMapper.java
│   │   │           │   └── TripRequestMapper.java
│   │   │           └── support
│   │   │               ├── JsonUtils.java
│   │   │               └── TimeUtils.java
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

## 6. 配置文件生成要求

### 6.1 .env.example

必须生成 `.env.example`，内容如下：

```env
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=travelmind
MYSQL_USERNAME=root
MYSQL_PASSWORD=

LLM_PROVIDER=mimo
MIMO_BASE_URL=
MIMO_CHAT_PATH=/v1/chat/completions
MIMO_API_KEY=
MIMO_MODEL=
MIMO_TEMPERATURE=0.4
MIMO_TIMEOUT_SECONDS=60

AMAP_API_KEY=
AMAP_BASE_URL=https://restapi.amap.com
AMAP_TIMEOUT_SECONDS=20

EXPORT_DIR=exports
```

说明：

- `MIMO_BASE_URL` 由用户填写真实 MiMo 接入地址。
- `MIMO_CHAT_PATH` 默认按 OpenAI Chat Completions 兼容路径处理。
- 如果 MiMo 官方接口路径不同，只需要改 `.env`，不改业务代码。
- 不要生成真实 `.env`。

### 6.2 .gitignore

必须包含：

```gitignore
.env
target/
logs/
exports/
*.log
```

### 6.3 application.yml

必须生成：

```yaml
spring:
  application:
    name: travelmind
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:travelmind}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:}

mybatis-plus:
  mapper-locations: classpath*:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: auto

travelmind:
  llm:
    provider: ${LLM_PROVIDER:mimo}
    mimo:
      base-url: ${MIMO_BASE_URL:}
      chat-path: ${MIMO_CHAT_PATH:/v1/chat/completions}
      api-key: ${MIMO_API_KEY:}
      model: ${MIMO_MODEL:}
      temperature: ${MIMO_TEMPERATURE:0.4}
      timeout-seconds: ${MIMO_TIMEOUT_SECONDS:60}
  amap:
    base-url: ${AMAP_BASE_URL:https://restapi.amap.com}
    api-key: ${AMAP_API_KEY:}
    timeout-seconds: ${AMAP_TIMEOUT_SECONDS:20}
  export:
    dir: ${EXPORT_DIR:exports}
```

## 7. 数据库脚本

必须生成 `sql/schema.sql`。

```sql
CREATE DATABASE IF NOT EXISTS travelmind DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE travelmind;

CREATE TABLE IF NOT EXISTS travel_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    current_itinerary_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS trip_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    raw_input TEXT NOT NULL,
    destination VARCHAR(64) NOT NULL,
    duration_days INT NOT NULL,
    start_date DATE NULL,
    people_count INT NULL,
    budget_level VARCHAR(32) NULL,
    pace_level VARCHAR(32) NULL,
    transport_mode VARCHAR(32) NULL,
    preferences_json JSON NULL,
    hotel_area VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_trip_request_session_id (session_id),
    INDEX idx_trip_request_destination (destination)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS itinerary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 1,
    title VARCHAR(256) NOT NULL,
    itinerary_json JSON NOT NULL,
    markdown_content MEDIUMTEXT NOT NULL,
    validation_status VARCHAR(32) NOT NULL DEFAULT 'PASSED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_itinerary_session_id (session_id),
    INDEX idx_itinerary_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS poi_cache (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source VARCHAR(32) NOT NULL DEFAULT 'AMAP',
    source_poi_id VARCHAR(128) NOT NULL,
    name VARCHAR(256) NOT NULL,
    city VARCHAR(64) NOT NULL,
    address VARCHAR(512) NULL,
    location VARCHAR(64) NULL,
    category VARCHAR(128) NULL,
    raw_json JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_source_poi_id (source, source_poi_id),
    INDEX idx_poi_city_name (city, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS llm_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128) NOT NULL,
    call_type VARCHAR(64) NOT NULL,
    prompt_tokens INT NULL,
    completion_tokens INT NULL,
    latency_ms INT NULL,
    status VARCHAR(32) NOT NULL,
    error_message TEXT NULL,
    request_json JSON NULL,
    response_json JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_llm_call_log_session_id (session_id),
    INDEX idx_llm_call_log_call_type (call_type),
    INDEX idx_llm_call_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 8. 领域模型实现要求

### 8.1 TripRequest

```java
package com.travelmind.domain;

import java.time.LocalDate;
import java.util.List;

public class TripRequest {
    private String destination;
    private Integer durationDays;
    private LocalDate startDate;
    private Integer peopleCount;
    private String budgetLevel;
    private String paceLevel;
    private String transportMode;
    private List<String> preferences;
    private String hotelArea;
    private String rawInput;
}
```

默认值由 `TripContextBuilder` 补齐：

```text
peopleCount = 2
budgetLevel = "舒适型"
paceLevel = "适中"
transportMode = "公共交通+步行"
preferences = ["第一次到访经典路线"]
```

### 8.2 Poi

```java
package com.travelmind.domain;

import java.math.BigDecimal;
import java.util.List;

public class Poi {
    private String source;
    private String sourceId;
    private String name;
    private String city;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String category;
    private List<String> tags;
    private Integer recommendedStayMinutes;
}
```

### 8.3 RouteInfo

```java
package com.travelmind.domain;

public class RouteInfo {
    private String originName;
    private String destinationName;
    private Integer distanceMeters;
    private Integer durationMinutes;
    private String transportMode;
}
```

### 8.4 Activity

```java
package com.travelmind.domain;

import java.util.List;

public class Activity {
    private String timeSlot;
    private String title;
    private String locationName;
    private Integer stayMinutes;
    private String transportSuggestion;
    private String reason;
    private List<String> tips;
}
```

### 8.5 DayPlan

```java
package com.travelmind.domain;

import java.util.List;

public class DayPlan {
    private Integer dayIndex;
    private String theme;
    private List<Activity> activities;
    private Integer totalVisitMinutes;
    private Integer totalTransportMinutes;
}
```

### 8.6 Itinerary

```java
package com.travelmind.domain;

import java.util.List;

public class Itinerary {
    private Long id;
    private Long sessionId;
    private TripRequest request;
    private List<DayPlan> days;
    private List<String> assumptions;
    private List<String> reminders;
    private List<String> alternatives;
    private String markdown;
}
```

## 9. 核心接口和方法签名

### 9.1 LlmClient

```java
package com.travelmind.llm;

public interface LlmClient {
    LlmResponse chat(LlmRequest request);
}
```

### 9.2 LlmRequest

```java
package com.travelmind.llm;

import java.util.List;
import java.util.Map;

public class LlmRequest {
    private String callType;
    private List<Message> messages;
    private Double temperature;
    private Map<String, Object> metadata;

    public static class Message {
        private String role;
        private String content;
    }
}
```

### 9.3 LlmResponse

```java
package com.travelmind.llm;

public class LlmResponse {
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Long latencyMs;
    private String rawResponse;
}
```

### 9.4 IntentParser

```java
package com.travelmind.planner;

import com.travelmind.domain.TripRequest;

public interface IntentParser {
    ParsedIntent parse(String userInput, TripContext context);

    class ParsedIntent {
        private String intent;
        private TripRequest tripRequest;
        private boolean needClarification;
        private java.util.List<String> questions;
        private String modificationInstruction;
    }
}
```

Intent 取值：

```text
NEW_PLAN
MODIFY_PLAN
UNKNOWN
```

### 9.5 AmapService

```java
package com.travelmind.amap;

import com.travelmind.domain.Poi;
import com.travelmind.domain.RouteInfo;
import java.util.List;

public interface AmapService {
    List<Poi> searchPois(String city, String keyword, int limit);

    Poi geocode(String address);

    RouteInfo estimateRoute(Poi origin, Poi destination, String transportMode);
}
```

### 9.6 TripPlannerService

```java
package com.travelmind.planner;

import com.travelmind.domain.Itinerary;

public interface TripPlannerService {
    Itinerary handleUserMessage(Long sessionId, String userInput);

    Itinerary createPlan(Long sessionId, String userInput);

    Itinerary modifyPlan(Long sessionId, String userInput);
}
```

### 9.7 MarkdownExporter

```java
package com.travelmind.export;

import com.travelmind.domain.Itinerary;
import java.nio.file.Path;

public interface MarkdownExporter {
    Path export(Itinerary itinerary);
}
```

## 10. CLI 实现要求

### 10.1 TravelMindShell

职责：

- 应用启动后进入循环。
- 使用 JLine 读取输入。
- 输入为空时继续等待。
- 输入 `/exit` 时退出。
- 输入 `/help` 时展示命令说明。
- 其他输入交给 `CommandRouter`。

伪代码：

```java
while (running) {
    String line = lineReader.readLine("TravelMind > ");
    if (line == null || line.isBlank()) {
        continue;
    }
    commandRouter.handle(line.trim());
}
```

### 10.2 CommandRouter

处理规则：

| 输入 | 行为 |
| --- | --- |
| `/help` | 输出帮助 |
| `/new` | 清空当前上下文，创建新会话 |
| `/history` | 查询历史行程 |
| `/export` | 导出当前行程 |
| `/exit` | 退出 |
| 其他文本 | 调用 `TripPlannerService.handleUserMessage` |

## 11. 大模型实现要求

### 11.1 MiMo 接入策略

由于不同平台暴露 MiMo 的 API 形式可能不同，代码必须做成可配置：

```text
MIMO_BASE_URL
MIMO_CHAT_PATH
MIMO_API_KEY
MIMO_MODEL
```

默认请求路径：

```text
{MIMO_BASE_URL}{MIMO_CHAT_PATH}
```

默认请求格式按 OpenAI Chat Completions 兼容格式实现：

```json
{
  "model": "your_mimo_model",
  "messages": [
    {
      "role": "system",
      "content": "..."
    },
    {
      "role": "user",
      "content": "..."
    }
  ],
  "temperature": 0.4
}
```

默认响应解析：

```text
choices[0].message.content
```

如果实际 MiMo 接口不兼容该格式，只修改 `MimoLlmClient` 的请求和响应映射，不改上层业务。

### 11.2 请求头

默认请求头：

```text
Authorization: Bearer ${MIMO_API_KEY}
Content-Type: application/json
```

不要在日志中打印完整 Authorization。

### 11.3 LLM 调用日志

每次调用后写入 `llm_call_log`：

```text
provider = mimo
model = MIMO_MODEL
call_type = INTENT_PARSE / ITINERARY_GENERATE / ITINERARY_MODIFY
latency_ms
status = SUCCESS / FAILED
error_message
request_json
response_json
```

## 12. Prompt 模板

所有 Prompt 建议集中放在：

```text
com.travelmind.llm.PromptTemplates
```

### 12.1 意图解析 Prompt

System Prompt：

```text
你是 travelmind 的旅行需求解析器。你的任务是把用户输入解析为严格 JSON。

你只能输出 JSON，不要输出解释文字，不要使用 Markdown。

字段要求：
- intent: NEW_PLAN、MODIFY_PLAN 或 UNKNOWN
- destination: 目的地城市，无法识别时为 null
- durationDays: 出行天数，无法识别时为 null
- startDate: yyyy-MM-dd，无法识别时为 null
- peopleCount: 人数，无法识别时为 null
- budgetLevel: 经济型、舒适型、高预算，无法识别时为 null
- paceLevel: 轻松、适中、紧凑，无法识别时为 null
- transportMode: 公共交通、打车、自驾、公共交通+步行，无法识别时为 null
- preferences: 字符串数组
- needClarification: 是否需要追问
- questions: 追问问题数组，最多 2 个
- modificationInstruction: 如果是修改行程，写出修改要求，否则为 null

判断规则：
- 如果用户要规划新的旅行，intent 为 NEW_PLAN。
- 如果用户是在调整当前行程，intent 为 MODIFY_PLAN。
- 如果缺少目的地或天数，needClarification 为 true。
- 如果只缺少预算、人数、偏好、交通方式，不需要追问，可以使用默认值。
```

User Prompt 模板：

```text
当前会话摘要：
{contextSummary}

用户输入：
{userInput}

请输出严格 JSON。
```

示例输出：

```json
{
  "intent": "NEW_PLAN",
  "destination": "上海",
  "durationDays": 3,
  "startDate": null,
  "peopleCount": null,
  "budgetLevel": null,
  "paceLevel": null,
  "transportMode": null,
  "preferences": [],
  "needClarification": false,
  "questions": [],
  "modificationInstruction": null
}
```

### 12.2 行程生成 Prompt

System Prompt：

```text
你是 travelmind 的专业行程规划助手。你需要根据结构化旅行需求和候选 POI 生成可执行的 Markdown 行程。

要求：
1. 只输出 Markdown。
2. 每天必须包含上午、中午、下午、晚上。
3. 每天必须有主题。
4. 每天必须有用餐区域建议。
5. 必须给出交通建议和注意事项。
6. 不要把一天安排得过满。
7. 优先使用候选 POI 中的地点。
8. 如果提到候选 POI 外的地点，必须标记为可选建议。
9. 不要编造具体门票价格和开放时间。
10. 如果开放时间、门票或预约信息不确定，写“出发前请再次确认”。
```

User Prompt 模板：

```text
结构化旅行需求：
{tripRequestJson}

默认规划假设：
{defaultAssumptionsJson}

候选 POI：
{candidatePoisJson}

路线和距离信息：
{routeInfosJson}

请生成 Markdown 行程。
```

### 12.3 行程修改 Prompt

System Prompt：

```text
你是 travelmind 的行程修改助手。你需要根据用户修改要求，调整已有 Markdown 行程。

要求：
1. 只输出新的完整 Markdown 行程。
2. 尽量只修改受影响的日期。
3. 保留未受影响的日期安排。
4. 修改后仍要包含规划假设、每日安排、交通建议、注意事项和备选方案。
5. 不要编造具体门票价格和开放时间。
```

User Prompt 模板：

```text
当前行程 Markdown：
{currentMarkdown}

用户修改要求：
{userInput}

新增或相关 POI：
{relatedPoisJson}

请输出修改后的完整 Markdown。
```

## 13. 高德地图 API 实现要求

### 13.1 基础配置

```text
AMAP_BASE_URL=https://restapi.amap.com
AMAP_API_KEY=your_amap_api_key
```

### 13.2 POI 关键字搜索

接口：

```text
GET /v3/place/text
```

完整 URL：

```text
https://restapi.amap.com/v3/place/text
```

参数：

| 参数 | 说明 |
| --- | --- |
| key | 高德 API Key |
| keywords | 搜索关键词 |
| city | 城市 |
| offset | 每页数量 |
| page | 页码 |
| extensions | all |

示例：

```text
GET https://restapi.amap.com/v3/place/text?key=xxx&keywords=景点&city=上海&offset=20&page=1&extensions=all
```

返回字段映射：

| 高德字段 | Poi 字段 |
| --- | --- |
| id | sourceId |
| name | name |
| cityname | city |
| address | address |
| location | longitude, latitude |
| type | category |

注意：

高德 `location` 格式通常是：

```text
经度,纬度
```

### 13.3 地理编码

接口：

```text
GET /v3/geocode/geo
```

参数：

| 参数 | 说明 |
| --- | --- |
| key | 高德 API Key |
| address | 地址 |
| city | 城市，可选 |

### 13.4 路径规划

MVP 可以优先实现驾车路径或步行路径估算。

驾车路径：

```text
GET /v3/direction/driving
```

步行路径：

```text
GET /v3/direction/walking
```

参数：

| 参数 | 说明 |
| --- | --- |
| key | 高德 API Key |
| origin | 起点经纬度，格式 lng,lat |
| destination | 终点经纬度，格式 lng,lat |

返回字段：

| 高德字段 | RouteInfo 字段 |
| --- | --- |
| route.paths[0].distance | distanceMeters |
| route.paths[0].duration | durationMinutes，秒转分钟 |

### 13.5 API 失败降级

如果高德 API 返回失败：

- 先查 `poi_cache`。
- 缓存没有数据时返回空列表。
- CLI 提示：地图服务暂不可用，本次行程将使用通用规划，建议出发前确认路线。

## 14. 行程规划流程实现

### 14.1 handleUserMessage

流程：

```text
1. 获取或创建 TravelSession。
2. 调用 IntentParser.parse。
3. 如果 needClarification = true，返回追问结果，不生成行程。
4. 如果 intent = NEW_PLAN，调用 createPlan。
5. 如果 intent = MODIFY_PLAN，调用 modifyPlan。
6. 如果无法识别，提示用户重新描述。
```

### 14.2 createPlan

流程：

```text
1. 解析 TripRequest。
2. TripContextBuilder 补齐默认值。
3. CandidatePoiBuilder 查询候选 POI。
4. 根据 POI 粗略估算路线信息。
5. ItineraryGenerator 调用大模型生成 Markdown。
6. RuleValidator 校验 Markdown 和结构化信息。
7. 保存 trip_request。
8. 保存 itinerary。
9. 更新 travel_session.current_itinerary_id。
10. 返回 Itinerary。
```

### 14.3 modifyPlan

流程：

```text
1. 从 travel_session 获取 current_itinerary_id。
2. 如果没有当前行程，提示用户先创建行程。
3. 调用 IntentParser 识别修改要求。
4. 根据修改文本查询相关 POI。
5. 调用 ItineraryGenerator 修改行程。
6. version = 上一个版本 + 1。
7. 保存新 itinerary。
8. 更新 travel_session.current_itinerary_id。
9. 返回新 Itinerary。
```

## 15. RuleValidator 实现要求

### 15.1 MVP 校验项

先实现简单规则，不做复杂 NLP：

```text
1. Markdown 不能为空。
2. Markdown 必须包含“第 1 天”。
3. 如果 durationDays = 3，则应包含“第 1 天”“第 2 天”“第 3 天”。
4. Markdown 应包含“上午”“中午”“下午”“晚上”。
5. Markdown 应包含“注意事项”或“提醒”。
6. 如果某一天出现超过 6 个列表项，添加“当天安排可能偏满”的提醒。
```

### 15.2 返回对象

```java
public class ValidationResult {
    private boolean passed;
    private List<String> warnings;
}
```

轻微问题不阻止保存，只把 warnings 加入 `Itinerary.reminders`。

## 16. Markdown 导出实现

### 16.1 导出规则

`MarkdownExporter.export(Itinerary itinerary)` 必须：

```text
1. 检查 itinerary 不为空。
2. 检查 markdown 不为空。
3. 如果导出目录不存在，自动创建。
4. 根据目的地和天数生成文件名。
5. 写入 UTF-8 Markdown 文件。
6. 返回 Path。
```

### 16.2 文件名规则

```text
travelmind-{destination}-{days}days-{yyyyMMddHHmm}.md
```

示例：

```text
travelmind-shanghai-3days-202606121530.md
```

如果目的地是中文，可以保留中文：

```text
travelmind-上海-3days-202606121530.md
```

## 17. Entity 和 Mapper 要求

### 17.1 Entity

每张表对应一个 Entity：

```text
TravelSessionEntity
TripRequestEntity
ItineraryEntity
PoiCacheEntity
LlmCallLogEntity
```

要求：

- 使用 MyBatis Plus 注解。
- 字段使用 camelCase。
- 表名使用 `@TableName`。
- 主键使用 `@TableId(type = IdType.AUTO)`。

示例：

```java
@TableName("travel_session")
public class TravelSessionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionName;
    private String status;
    private Long currentItineraryId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 17.2 Mapper

每张表一个 Mapper：

```java
public interface TravelSessionMapper extends BaseMapper<TravelSessionEntity> {
}
```

## 18. README 生成要求

必须生成 `README.md`，包含：

- 项目简介。
- 技术栈。
- 功能列表。
- 环境准备。
- MySQL 初始化。
- `.env` 配置。
- 启动方式。
- CLI 使用示例。
- 项目结构。
- 面试亮点。

启动命令示例：

```bash
mvn spring-boot:run
```

打包命令示例：

```bash
mvn clean package
java -jar target/travelmind-0.0.1-SNAPSHOT.jar
```

## 19. 单元测试要求

### 19.1 必须测试的类

```text
RuleValidatorTest
MarkdownExporterTest
TripContextBuilderTest
UserMessageClassifierTest
IntentParserTest
```

### 19.2 测试原则

- 不真实调用 MiMo。
- 不真实调用高德地图。
- 不依赖真实 MySQL。
- 使用 Mock 或内存对象。

### 19.3 样例测试点

`RuleValidatorTest`：

- Markdown 为空时校验失败。
- 三日游缺少第 3 天时给出 warning。
- 包含上午、中午、下午、晚上时通过。

`MarkdownExporterTest`：

- 能创建导出目录。
- 能写入 UTF-8 文件。
- 文件名包含目的地和天数。

`TripContextBuilderTest`：

- 缺少预算时默认舒适型。
- 缺少交通方式时默认公共交通+步行。

## 20. 验收标准

代码生成完成后，至少满足以下验收标准。

### 20.1 启动验收

执行：

```bash
mvn spring-boot:run
```

期望：

```text
程序启动成功
出现 TravelMind > 提示符
```

### 20.2 帮助命令

输入：

```text
/help
```

期望：

```text
显示 /new /history /export /exit 等命令说明
```

### 20.3 新建行程

输入：

```text
帮我规划去上海的三日旅游的行程
```

期望：

```text
系统调用意图解析
识别 destination = 上海
识别 durationDays = 3
生成三日 Markdown 行程
保存到 itinerary 表
CLI 输出行程内容
```

### 20.4 多轮修改

输入：

```text
第二天不要去博物馆，换成迪士尼
```

期望：

```text
系统识别为 MODIFY_PLAN
生成新版本 itinerary
当前会话指向新 itinerary
CLI 输出修改后的行程
```

### 20.5 导出

输入：

```text
/export
```

期望：

```text
在 exports 目录生成 Markdown 文件
CLI 输出导出路径
```

### 20.6 测试

执行：

```bash
mvn test
```

期望：

```text
所有单元测试通过
```

## 21. AI 编码助手执行顺序

如果由 AI 生成代码，请严格按以下顺序：

1. 创建 Maven Spring Boot 项目骨架。
2. 创建 `.gitignore`、`.env.example`、`README.md`。
3. 创建 `sql/schema.sql`。
4. 创建 domain 模型。
5. 创建 entity 和 mapper。
6. 创建配置类。
7. 创建 LLM 抽象和 `MimoLlmClient`。
8. 创建高德地图 `AmapService`。
9. 创建 Prompt 模板。
10. 创建 `IntentParser`。
11. 创建 `TripContextBuilder`。
12. 创建 `CandidatePoiBuilder`。
13. 创建 `ItineraryGenerator`。
14. 创建 `RuleValidator`。
15. 创建 `TripPlannerService`。
16. 创建 `ConversationManager`。
17. 创建 CLI Shell 和命令路由。
18. 创建 Markdown 导出。
19. 创建单元测试。
20. 运行 `mvn test`。
21. 修复编译和测试问题。

不要一开始就实现复杂优化算法。先保证 MVP 可运行。

## 22. 允许的简化

为了控制面试项目复杂度，允许以下简化：

- 行程主体用 Markdown 和 JSON 保存，不拆分到 day_plan、activity 表。
- 高德路线估算失败时，可以只使用 POI 列表生成行程。
- 第一个版本可以只实现公共交通文字建议，实际路线用驾车或步行接口估算。
- `/history` 可以只展示最近 10 条 itinerary 标题和创建时间。
- 多轮修改可以先整篇 Markdown 重写，不必实现复杂局部 AST 修改。

## 23. 禁止的实现方式

不要这样做：

- 不要把所有业务都写在一个 Main 类里。
- 不要在代码中写真实 API Key。
- 不要让大模型直接决定数据库写入。
- 不要让 CLI 直接调用 Mapper。
- 不要在单元测试里真实访问外部 API。
- 不要实现前端页面。
- 不要扩展 PDF、Word 导出。
- 不要在 MVP 中引入向量数据库。

## 24. 面试讲解时的代码亮点

实现时请保留以下可讲点：

- `LlmClient` 抽象：模型供应商可替换。
- `AmapService` 抽象：地图服务集中封装。
- `ConversationManager`：CLI 也有多轮状态管理。
- `RuleValidator`：用规则兜底大模型不稳定。
- `llm_call_log`：可观测性和问题排查。
- `.env`：敏感配置不入库。
- Markdown 导出：项目有实际交付物。
- JSON 存储行程：MVP 阶段控制表结构复杂度。

## 25. 最终交付物清单

代码生成完成后，项目至少应包含：

```text
pom.xml
.gitignore
.env.example
README.md
sql/schema.sql
docs/travelmind-system-architecture.md
docs/travelmind-code-generation-guide.md
src/main/java/com/travelmind/**
src/main/resources/application.yml
src/test/java/com/travelmind/**
```

并且满足：

```text
mvn test 可执行
mvn spring-boot:run 可进入 TravelMind > 交互界面
/help 可用
/export 可用
```

## 26. 备注

本文档的目标是让另一个 AI 或开发者减少猜测空间，按照明确的工程边界生成一个可运行、可演示、可面试讲解的 Java CLI 项目。

实现时如果遇到 MiMo 实际 API 与 OpenAI Chat Completions 格式不兼容，应只调整 `MimoLlmClient` 内部适配逻辑，不改变 `LlmClient` 接口和上层业务流程。
