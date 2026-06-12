package com.travelmind.conversation;

/**
 * 会话意图枚举
 */
public enum ConversationIntent {

    /**
     * 新建行程
     */
    NEW_PLAN,

    /**
     * 修改行程
     */
    MODIFY_PLAN,

    /**
     * 导出行程
     */
    EXPORT,

    /**
     * 系统命令
     */
    SYSTEM_COMMAND,

    /**
     * 未知
     */
    UNKNOWN
}
