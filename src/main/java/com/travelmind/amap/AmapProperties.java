package com.travelmind.amap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 高德地图配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "travelmind.amap")
public class AmapProperties {

    /**
     * 基础 URL
     */
    private String baseUrl = "https://restapi.amap.com";

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 超时时间（秒）
     */
    private Integer timeoutSeconds = 20;
}
