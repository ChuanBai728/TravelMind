package com.travelmind.storage;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLM 调用日志内存存储
 */
@Repository
public class LlmCallLogRepository {

    private final Map<Long, LlmCallLog> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public LlmCallLog save(LlmCallLog log) {
        if (log.getId() == null) {
            log.setId(idGenerator.getAndIncrement());
            log.setCreatedAt(LocalDateTime.now());
        }
        store.put(log.getId(), log);
        return log;
    }

    public List<LlmCallLog> findBySessionId(Long sessionId) {
        List<LlmCallLog> result = new ArrayList<>();
        for (LlmCallLog log : store.values()) {
            if (sessionId.equals(log.getSessionId())) {
                result.add(log);
            }
        }
        return result;
    }

    /**
     * LLM 调用日志数据对象
     */
    public static class LlmCallLog {
        private Long id;
        private Long sessionId;
        private String provider;
        private String model;
        private String callType;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer latencyMs;
        private String status;
        private String errorMessage;
        private String requestJson;
        private String responseJson;
        private LocalDateTime createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getCallType() { return callType; }
        public void setCallType(String callType) { this.callType = callType; }
        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        public Integer getLatencyMs() { return latencyMs; }
        public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getRequestJson() { return requestJson; }
        public void setRequestJson(String requestJson) { this.requestJson = requestJson; }
        public String getResponseJson() { return responseJson; }
        public void setResponseJson(String responseJson) { this.responseJson = responseJson; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
