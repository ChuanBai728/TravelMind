package com.travelmind.llm;

/**
 * Prompt 模板集中管理
 */
public final class PromptTemplates {

    private PromptTemplates() {
    }

    /**
     * 意图解析 System Prompt
     */
    public static final String INTENT_PARSE_SYSTEM = """
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
            """;

    /**
     * 意图解析 User Prompt 模板
     */
    public static final String INTENT_PARSE_USER = """
            当前会话摘要：
            %s

            用户输入：
            %s

            请输出严格 JSON。
            """;

    /**
     * 行程生成 System Prompt
     */
    public static final String ITINERARY_GENERATE_SYSTEM = """
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
            10. 如果开放时间、门票或预约信息不确定，写"出发前请再次确认"。
            11. 不要编造具体的餐厅、店铺、酒店名称。用餐建议只推荐区域和菜系（如"豫园商圈品尝本帮菜"），不要捏造店名。
            12. 只有候选 POI 中明确列出的商家才可以直接引用名称。
            """;

    /**
     * 行程生成 User Prompt 模板
     */
    public static final String ITINERARY_GENERATE_USER = """
            结构化旅行需求：
            %s

            默认规划假设：
            %s

            候选 POI：
            %s

            路线和距离信息：
            %s

            请生成 Markdown 行程。
            """;

    /**
     * 行程修改 System Prompt
     */
    public static final String ITINERARY_MODIFY_SYSTEM = """
            你是 travelmind 的行程修改助手。你需要根据用户修改要求，调整已有 Markdown 行程。

            要求：
            1. 只输出新的完整 Markdown 行程。
            2. 尽量只修改受影响的日期。
            3. 保留未受影响的日期安排。
            4. 修改后仍要包含规划假设、每日安排、交通建议、注意事项和备选方案。
            5. 不要编造具体门票价格和开放时间。
            6. 不要编造具体的餐厅、店铺、酒店名称。用餐建议只推荐区域和菜系，不要捏造店名。
            7. 只有候选 POI 中明确列出的商家才可以直接引用名称。
            """;

    /**
     * 行程修改 User Prompt 模板
     */
    public static final String ITINERARY_MODIFY_USER = """
            当前行程 Markdown：
            %s

            用户修改要求：
            %s

            新增或相关 POI：
            %s

            请输出修改后的完整 Markdown。
            """;

    /**
     * 默认规划假设 JSON
     */
    public static final String DEFAULT_ASSUMPTIONS = """
            {
              "peopleCount": "1-2人",
              "budgetLevel": "舒适型",
              "paceLevel": "适中",
              "transportMode": "公共交通+步行",
              "preferences": "第一次到访经典路线",
              "activitiesPerDay": "3-5个",
              "hoursPerDay": "8-10小时"
            }
            """;
}
