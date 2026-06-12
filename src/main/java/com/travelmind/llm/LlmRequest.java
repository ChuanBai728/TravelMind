package com.travelmind.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM 请求对象
 */
public class LlmRequest {

    /**
     * 调用类型，例如 INTENT_PARSE、ITINERARY_GENERATE、ITINERARY_MODIFY
     */
    private String callType;

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    public LlmRequest() {
        this.messages = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public LlmRequest(String callType) {
        this();
        this.callType = callType;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public void addMessage(String role, String content) {
        this.messages.add(new Message(role, content));
    }

    /**
     * 消息对象
     */
    public static class Message {
        private String role;
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
