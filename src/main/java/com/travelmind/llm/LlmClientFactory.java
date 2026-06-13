package com.travelmind.llm;

import com.travelmind.llm.mimo.MimoLlmClient;
import com.travelmind.llm.openai.OpenAiLlmClient;
import org.springframework.stereotype.Component;

/**
 * LLM 客户端工厂
 */
@Component
public class LlmClientFactory {

    private final LlmProperties llmProperties;
    private final MimoLlmClient mimoLlmClient;
    private final OpenAiLlmClient openAiLlmClient;

    public LlmClientFactory(LlmProperties llmProperties, MimoLlmClient mimoLlmClient,
                             OpenAiLlmClient openAiLlmClient) {
        this.llmProperties = llmProperties;
        this.mimoLlmClient = mimoLlmClient;
        this.openAiLlmClient = openAiLlmClient;
    }

    /**
     * 获取 LLM 客户端
     */
    public LlmClient getClient() {
        String provider = llmProperties.getProvider() == null ? "mimo" : llmProperties.getProvider().toLowerCase();
        switch (provider) {
            case "mimo":
                return mimoLlmClient;
            case "openai":
                return openAiLlmClient;
            default:
                throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        }
    }

    /**
     * 获取意图解析专用客户端
     * 如果配置了 OPENAI_* 相关配置，使用 OpenAI 客户端（适用于硅基流动等平台的小模型）
     * 否则回退到主客户端
     */
    public LlmClient getIntentClient() {
        LlmProperties.OpenAiProperties openaiConfig = llmProperties.getOpenai();
        if (openaiConfig != null && openaiConfig.getBaseUrl() != null && !openaiConfig.getBaseUrl().isEmpty()
                && openaiConfig.getApiKey() != null && !openaiConfig.getApiKey().isEmpty()) {
            return openAiLlmClient;
        }
        return getClient();
    }

    /**
     * 获取意图解析专用模型名称
     */
    public String getIntentModel() {
        // 优先使用 OpenAI 配置的模型
        LlmProperties.OpenAiProperties openaiConfig = llmProperties.getOpenai();
        if (openaiConfig != null && openaiConfig.getModel() != null && !openaiConfig.getModel().isEmpty()) {
            return openaiConfig.getModel();
        }
        // 回退到 mimo 的 intentModel 配置
        return llmProperties.getMimo().getIntentModel();
    }
}
