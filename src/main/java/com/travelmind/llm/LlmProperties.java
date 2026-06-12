package com.travelmind.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "travelmind.llm")
public class LlmProperties {

    /**
     * 提供者名称
     */
    private String provider = "mimo";

    /**
     * MiMo 配置
     */
    private MimoProperties mimo = new MimoProperties();

    @Data
    public static class MimoProperties {
        /**
         * 基础 URL
         */
        private String baseUrl;

        /**
         * 聊天接口路径
         */
        private String chatPath = "/v1/chat/completions";

        /**
         * API Key
         */
        private String apiKey;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 温度参数
         */
        private Double temperature = 0.4;

        /**
         * 超时时间（秒）
         */
        private Integer timeoutSeconds = 60;
    }
}
