package com.travelmind.llm;

import com.travelmind.llm.mimo.MimoLlmClient;
import com.travelmind.llm.openai.OpenAiLlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LlmClientFactory {

    private final LlmProperties llmProperties;
    private final MimoLlmClient mimoLlmClient;
    private final OpenAiLlmClient openAiLlmClient;

    @Autowired
    public LlmClientFactory(LlmProperties llmProperties, MimoLlmClient mimoLlmClient,
                            OpenAiLlmClient openAiLlmClient) {
        this.llmProperties = llmProperties;
        this.mimoLlmClient = mimoLlmClient;
        this.openAiLlmClient = openAiLlmClient;
    }

    public LlmClientFactory(LlmProperties llmProperties, MimoLlmClient mimoLlmClient) {
        this(llmProperties, mimoLlmClient, null);
    }

    public LlmClient getClient() {
        if (llmProperties == null) {
            return mimoLlmClient;
        }

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

    public LlmClient getIntentClient() {
        if (llmProperties == null) {
            return getClient();
        }

        LlmProperties.OpenAiProperties openaiConfig = llmProperties.getOpenai();
        if (openaiConfig != null
                && openaiConfig.getBaseUrl() != null && !openaiConfig.getBaseUrl().isEmpty()
                && openaiConfig.getApiKey() != null && !openaiConfig.getApiKey().isEmpty()
                && openAiLlmClient != null) {
            return openAiLlmClient;
        }

        return getClient();
    }

    public String getIntentModel() {
        if (llmProperties == null) {
            return null;
        }

        LlmProperties.OpenAiProperties openaiConfig = llmProperties.getOpenai();
        if (openaiConfig != null && openaiConfig.getModel() != null && !openaiConfig.getModel().isEmpty()) {
            return openaiConfig.getModel();
        }

        return llmProperties.getMimo() == null ? null : llmProperties.getMimo().getIntentModel();
    }
}
