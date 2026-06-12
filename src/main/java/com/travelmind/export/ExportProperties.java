package com.travelmind.export;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 导出配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "travelmind.export")
public class ExportProperties {

    /**
     * 导出目录
     */
    private String dir = "exports";
}
