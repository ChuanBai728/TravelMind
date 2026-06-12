package com.travelmind.llm;

/**
 * LLM 响应对象
 */
public class LlmResponse {

    /**
     * 响应内容
     */
    private String content;

    /**
     * 提示词 token 数量
     */
    private Integer promptTokens;

    /**
     * 生成内容 token 数量
     */
    private Integer completionTokens;

    /**
     * 延迟时间（毫秒）
     */
    private Long latencyMs;

    /**
     * 原始响应 JSON
     */
    private String rawResponse;

    public LlmResponse() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
