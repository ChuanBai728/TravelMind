package com.travelmind.conversation;

import org.springframework.stereotype.Component;

/**
 * 用户消息分类器
 */
@Component
public class UserMessageClassifier {

    /**
     * 分类用户消息
     *
     * @param message 用户消息
     * @return 消息分类
     */
    public ConversationIntent classify(String message) {
        if (message == null || message.trim().isEmpty()) {
            return ConversationIntent.UNKNOWN;
        }

        String trimmed = message.trim();

        // 检查是否是系统命令
        if (trimmed.startsWith("/")) {
            return classifyCommand(trimmed);
        }

        // 检查是否是修改行程的意图
        if (isModificationIntent(trimmed)) {
            return ConversationIntent.MODIFY_PLAN;
        }

        // 检查是否是新建行程的意图
        if (isNewPlanIntent(trimmed)) {
            return ConversationIntent.NEW_PLAN;
        }

        // 默认返回未知
        return ConversationIntent.UNKNOWN;
    }

    private ConversationIntent classifyCommand(String command) {
        switch (command.toLowerCase()) {
            case "/help":
            case "/new":
            case "/history":
            case "/exit":
                return ConversationIntent.SYSTEM_COMMAND;
            case "/export":
                return ConversationIntent.EXPORT;
            default:
                return ConversationIntent.SYSTEM_COMMAND;
        }
    }

    private boolean isModificationIntent(String message) {
        // 常见的修改关键词
        String[] modificationKeywords = {
                "修改", "调整", "改变", "换", "去掉", "删除", "不要",
                "第二天", "第三天", "第四天", "第五天",
                "第1天", "第2天", "第3天", "第4天", "第5天",
                "改成", "换成", "换成", "替换成"
        };

        for (String keyword : modificationKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private boolean isNewPlanIntent(String message) {
        // 常见的新建行程关键词
        String[] newPlanKeywords = {
                "规划", "安排", "计划", "行程", "旅游", "旅行",
                "帮我", "想要", "打算", "准备"
        };

        for (String keyword : newPlanKeywords) {
            if (message.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
