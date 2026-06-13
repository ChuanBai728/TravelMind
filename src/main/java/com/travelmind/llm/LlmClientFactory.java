package com.travelmind.llm;

import com.travelmind.llm.mimo.MimoLlmClient;
import org.springframework.stereotype.Component;

/**
 * LLM 客户端工厂
 */
@Component
public class LlmClientFactory {

    private final LlmProperties llmProperties;
    private final MimoLlmClient mimoLlmClient;

    public LlmClientFactory(LlmProperties llmProperties, MimoLlmClient mimoLlmClient) {
        this.llmProperties = llmProperties;
        this.mimoLlmClient = mimoLlmClient;
    }

    /**
     * 获取 LLM 客户端
     *
     * @return LLM 客户端实例
     */
    public LlmClient getClient() {
        String provider = llmProperties.getProvider() == null ? "mimo" : llmProperties.getProvider().toLowerCase();
        switch (provider) {
            case "mimo":
                return mimoLlmClient;
            default:
                throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        }
    }

    /**
     * 获取意图解析专用模型名称（未配置时返回 null，使用默认模型）
     */
    public String getIntentModel() {
        return llmProperties.getMimo().getIntentModel();
    }
}
