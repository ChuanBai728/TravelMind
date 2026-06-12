package com.travelmind.export;

import com.travelmind.domain.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Markdown 导出器
 */
@Component
public class MarkdownExporter {

    private static final Logger log = LoggerFactory.getLogger(MarkdownExporter.class);

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final ExportProperties exportProperties;

    public MarkdownExporter(ExportProperties exportProperties) {
        this.exportProperties = exportProperties;
    }

    /**
     * 导出行程为 Markdown 文件
     *
     * @param itinerary 行程对象
     * @return 导出文件路径
     */
    public Path export(Itinerary itinerary) {
        if (itinerary == null) {
            throw new IllegalArgumentException("行程对象不能为空");
        }

        String markdown = itinerary.getMarkdown();
        if (markdown == null || markdown.trim().isEmpty()) {
            throw new IllegalArgumentException("行程内容不能为空");
        }

        // 确保导出目录存在
        Path exportDir = Paths.get(exportProperties.getDir());
        try {
            Files.createDirectories(exportDir);
        } catch (IOException e) {
            log.error("Failed to create export directory: {}", exportDir, e);
            throw new RuntimeException("创建导出目录失败: " + e.getMessage(), e);
        }

        // 生成文件名
        String fileName = generateFileName(itinerary);
        Path filePath = exportDir.resolve(fileName);

        // 写入文件
        try {
            Files.writeString(filePath, markdown, StandardCharsets.UTF_8);
            log.info("Exported itinerary to: {}", filePath);
            return filePath;
        } catch (IOException e) {
            log.error("Failed to export itinerary", e);
            throw new RuntimeException("导出行程失败: " + e.getMessage(), e);
        }
    }

    private String generateFileName(Itinerary itinerary) {
        StringBuilder sb = new StringBuilder("travelmind-");

        if (itinerary.getRequest() != null && itinerary.getRequest().getDestination() != null) {
            sb.append(itinerary.getRequest().getDestination());
            sb.append("-");
        }

        if (itinerary.getRequest() != null && itinerary.getRequest().getDurationDays() != null) {
            sb.append(itinerary.getRequest().getDurationDays());
            sb.append("days-");
        }

        sb.append(LocalDateTime.now().format(FILE_DATE_FORMAT));
        sb.append(".md");

        return sb.toString();
    }
}
