package com.travelmind.planner;

import com.travelmind.domain.TripRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 行程上下文构建器，负责补全默认值
 */
@Component
public class TripContextBuilder {

    /**
     * 默认人数
     */
    private static final Integer DEFAULT_PEOPLE_COUNT = 2;

    /**
     * 默认预算等级
     */
    private static final String DEFAULT_BUDGET_LEVEL = "舒适型";

    /**
     * 默认节奏
     */
    private static final String DEFAULT_PACE_LEVEL = "适中";

    /**
     * 默认交通方式
     */
    private static final String DEFAULT_TRANSPORT_MODE = "公共交通+步行";

    /**
     * 默认偏好
     */
    private static final List<String> DEFAULT_PREFERENCES = List.of("第一次到访经典路线");

    /**
     * 补全默认值
     *
     * @param tripRequest 原始需求
     * @return 补全后的需求
     */
    public TripRequest fillDefaults(TripRequest tripRequest) {
        if (tripRequest == null) {
            return null;
        }

        if (tripRequest.getPeopleCount() == null) {
            tripRequest.setPeopleCount(DEFAULT_PEOPLE_COUNT);
        }

        if (tripRequest.getBudgetLevel() == null) {
            tripRequest.setBudgetLevel(DEFAULT_BUDGET_LEVEL);
        }

        if (tripRequest.getPaceLevel() == null) {
            tripRequest.setPaceLevel(DEFAULT_PACE_LEVEL);
        }

        if (tripRequest.getTransportMode() == null) {
            tripRequest.setTransportMode(DEFAULT_TRANSPORT_MODE);
        }

        if (tripRequest.getPreferences() == null || tripRequest.getPreferences().isEmpty()) {
            tripRequest.setPreferences(DEFAULT_PREFERENCES);
        }

        return tripRequest;
    }

    /**
     * 构建规划上下文
     *
     * @param sessionId  会话 ID
     * @param userInput  用户输入
     * @param tripRequest 旅行需求
     * @return 规划上下文
     */
    public TripContext build(Long sessionId, String userInput, TripRequest tripRequest) {
        TripContext context = new TripContext();
        context.setSessionId(sessionId);
        context.setUserInput(userInput);
        context.setTripRequest(fillDefaults(tripRequest));

        // 构建上下文摘要
        context.setContextSummary(buildContextSummary(tripRequest));

        return context;
    }

    private String buildContextSummary(TripRequest tripRequest) {
        if (tripRequest == null) {
            return "无";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("目的地：").append(tripRequest.getDestination() != null ? tripRequest.getDestination() : "未指定");
        sb.append("，天数：").append(tripRequest.getDurationDays() != null ? tripRequest.getDurationDays() : "未指定");

        if (tripRequest.getBudgetLevel() != null) {
            sb.append("，预算：").append(tripRequest.getBudgetLevel());
        }

        if (tripRequest.getPaceLevel() != null) {
            sb.append("，节奏：").append(tripRequest.getPaceLevel());
        }

        if (tripRequest.getTransportMode() != null) {
            sb.append("，交通：").append(tripRequest.getTransportMode());
        }

        return sb.toString();
    }
}
