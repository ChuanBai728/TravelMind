package com.travelmind.export;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 导出配置属性
 */
@Component
@ConfigurationProperties(prefix = "travelmind.export")
public class ExportProperties {

    private String dir = "exports";

    public String getDir() { return dir; }
    public void setDir(String dir) { this.dir = dir; }
}
