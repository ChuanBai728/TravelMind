package com.travelmind.export;

import com.travelmind.domain.Itinerary;
import com.travelmind.domain.TripRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MarkdownExporter 单元测试
 */
class MarkdownExporterTest {

    private MarkdownExporter markdownExporter;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ExportProperties properties = new ExportProperties();
        properties.setDir(tempDir.toString());
        markdownExporter = new MarkdownExporter(properties);
    }

    @Test
    void export_ValidItinerary_ShouldCreateFile() {
        Itinerary itinerary = createTestItinerary();

        Path result = markdownExporter.export(itinerary);

        assertNotNull(result);
        assertTrue(Files.exists(result));
    }

    @Test
    void export_ValidItinerary_ShouldContainMarkdown() throws IOException {
        Itinerary itinerary = createTestItinerary();

        Path result = markdownExporter.export(itinerary);
        String content = Files.readString(result);

        assertEquals(itinerary.getMarkdown(), content);
    }

    @Test
    void export_ValidItinerary_FileNameShouldContainDestination() {
        Itinerary itinerary = createTestItinerary();

        Path result = markdownExporter.export(itinerary);

        assertTrue(result.getFileName().toString().contains("上海"));
    }

    @Test
    void export_ValidItinerary_FileNameShouldContainDays() {
        Itinerary itinerary = createTestItinerary();

        Path result = markdownExporter.export(itinerary);

        assertTrue(result.getFileName().toString().contains("3days"));
    }

    @Test
    void export_ValidItinerary_FileNameShouldEndWithMd() {
        Itinerary itinerary = createTestItinerary();

        Path result = markdownExporter.export(itinerary);

        assertTrue(result.getFileName().toString().endsWith(".md"));
    }

    @Test
    void export_NullItinerary_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> {
            markdownExporter.export(null);
        });
    }

    @Test
    void export_NullMarkdown_ShouldThrowException() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown(null);

        assertThrows(IllegalArgumentException.class, () -> {
            markdownExporter.export(itinerary);
        });
    }

    @Test
    void export_EmptyMarkdown_ShouldThrowException() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown("");

        assertThrows(IllegalArgumentException.class, () -> {
            markdownExporter.export(itinerary);
        });
    }

    @Test
    void export_ExportDirNotExist_ShouldCreateDir() {
        ExportProperties properties = new ExportProperties();
        properties.setDir(tempDir.resolve("new-dir").toString());
        MarkdownExporter exporter = new MarkdownExporter(properties);

        Itinerary itinerary = createTestItinerary();
        Path result = exporter.export(itinerary);

        assertNotNull(result);
        assertTrue(Files.exists(result));
    }

    @Test
    void export_Utf8Content_ShouldPreserveEncoding() throws IOException {
        Itinerary itinerary = createTestItinerary();
        itinerary.setMarkdown("# 上海三日游\n\n## 第 1 天：经典上海\n\n- 上午：人民广场、上海博物馆");

        Path result = markdownExporter.export(itinerary);
        String content = Files.readString(result);

        assertTrue(content.contains("上海三日游"));
        assertTrue(content.contains("人民广场"));
    }

    private Itinerary createTestItinerary() {
        Itinerary itinerary = new Itinerary();
        itinerary.setSessionId(1L);

        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);
        itinerary.setRequest(request);

        itinerary.setMarkdown("# 上海三日游\n\n## 第 1 天\n- 上午：测试");

        return itinerary;
    }
}
