package com.travelmind.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 配置属性
 */
@Component
@ConfigurationProperties(prefix = "travelmind.llm")
public class LlmProperties {

    private String provider = "mimo";
    private MimoProperties mimo = new MimoProperties();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public MimoProperties getMimo() { return mimo; }
    public void setMimo(MimoProperties mimo) { this.mimo = mimo; }

    public static class MimoProperties {
        private String baseUrl;
        private String chatPath = "/v1/chat/completions";
        private String apiKey;
        private String model;
        private Double temperature = 0.4;
        private Integer timeoutSeconds = 60;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getChatPath() { return chatPath; }
        public void setChatPath(String chatPath) { this.chatPath = chatPath; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}
