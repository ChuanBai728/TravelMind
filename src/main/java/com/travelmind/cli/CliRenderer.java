package com.travelmind.cli;

import com.travelmind.domain.Itinerary;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * CLI 渲染器
 */
@Component
public class CliRenderer {

    /**
     * 渲染行程（转换为终端友好格式）
     *
     * @param itinerary 行程对象
     */
    public void renderItinerary(Itinerary itinerary) {
        if (itinerary == null || itinerary.getMarkdown() == null) {
            System.out.println("暂无行程内容");
            return;
        }

        System.out.println();
        System.out.println(convertMarkdownToTerminal(itinerary.getMarkdown()));
        System.out.println();

        renderReminders(itinerary);
    }

    /**
     * 渲染行程（流式模式），先输出已流式打印的内容，再显示提醒
     */
    public void renderItineraryAfterStream(Itinerary itinerary) {
        if (itinerary == null) return;
        System.out.println();
        renderReminders(itinerary);
    }

    /**
     * 创建流式输出回调，逐行将 Markdown 实时转换为终端格式输出
     *
     * @return 文本片段回调
     */
    public Consumer<String> createStreamingCallback() {
        return new StreamingRenderer();
    }

    /**
     * 流式渲染器：缓冲文本片段，输出完整的转换后行
     */
    public class StreamingRenderer implements Consumer<String> {
        private final StringBuilder lineBuffer = new StringBuilder();
        private boolean inList = false;

        @Override
        public void accept(String chunk) {
            lineBuffer.append(chunk);
            String buf = lineBuffer.toString();

            int newlineIdx;
            while ((newlineIdx = buf.indexOf("\n")) >= 0) {
                String completeLine = buf.substring(0, newlineIdx);
                buf = buf.substring(newlineIdx + 1);
                printLine(completeLine);
            }

            lineBuffer.setLength(0);
            lineBuffer.append(buf);
        }

        /**
         * 流结束后调用，处理缓冲区中剩余内容并刷新
         */
        public void flush() {
            if (lineBuffer.length() > 0) {
                printLine(lineBuffer.toString());
                lineBuffer.setLength(0);
            }
        }

        private void printLine(String rawLine) {
            // 清理装饰字符（LLM 可能在标题前输出 ? ? ? 等装饰符号）
            String cleaned = rawLine.replaceAll("^[\\s>*?❓❓▸►●◆★☆-]+", "").trim();
            String trimmed = rawLine.trim();

            if (cleaned.isEmpty() && trimmed.isEmpty()) {
                if (inList) inList = false;
                System.out.println();
                return;
            }

            // 分隔线
            if (trimmed.matches("^[-*_]{3,}$")) {
                System.out.println("  ------------------------------------------------------------");
                return;
            }

            // # 一级标题（兼容 #? 标题、# 标题）
            if (trimmed.matches("^#{1,3}[^#\\w]?.*")) {
                String content = trimmed.replaceFirst("^#{1,3}[^#\\w]?\\s*", "");
                int level = trimmed.indexOf(' ');
                if (level < 0) level = trimmed.length();
                level = Math.min(level, 3);
                String title = stripMd(content.trim());

                if (level == 1) {
                    System.out.println();
                    System.out.println("============================================================");
                    System.out.println("  " + title);
                    System.out.println("============================================================");
                } else if (level == 2) {
                    System.out.println();
                    System.out.println("------------------------------------------------------------");
                    System.out.println("  " + title);
                    System.out.println("------------------------------------------------------------");
                } else {
                    System.out.println();
                    System.out.println("  " + title);
                }
                inList = false;
                return;
            }

            // ? 装饰标题行（LLM 输出的 ? 当日行程安排 等）
            if (trimmed.matches("^[?❓❓]\\s*.*")) {
                String title = stripMd(trimmed.replaceFirst("^[?❓❓]\\s*", "").trim());
                if (!title.isEmpty()) {
                    System.out.println();
                    System.out.println("------------------------------------------------------------");
                    System.out.println("  " + title);
                    System.out.println("------------------------------------------------------------");
                }
                inList = false;
                return;
            }

            // > 引用
            if (trimmed.startsWith("> ")) {
                System.out.println("  " + stripMd(trimmed.substring(2).trim()));
                return;
            }
            if (trimmed.equals(">")) {
                System.out.println();
                return;
            }

            // 数字列表
            if (trimmed.matches("^\\d+\\.\\s+.*")) {
                String num = trimmed.replaceFirst("\\..*", "");
                String content = stripMd(trimmed.replaceFirst("^\\d+\\.\\s+", ""));
                System.out.println("    " + num + ". " + content);
                inList = true;
                return;
            }

            // 无序列表
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                System.out.println("    - " + stripMd(trimmed.substring(2).trim()));
                inList = true;
                return;
            }

            // 普通文本
            System.out.println("  " + stripMd(trimmed));
        }

        private String stripMd(String text) {
            if (text == null) return "";
            text = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
            text = text.replaceAll("\\*(.*?)\\*", "$1");
            text = text.replaceAll("`([^`]*)`", "$1");
            text = text.replaceAll("\\[([^\\]]*)\\]\\([^\\)]*\\)", "$1");
            return text;
        }
    }

    private void renderReminders(Itinerary itinerary) {
        if (itinerary.getReminders() != null && !itinerary.getReminders().isEmpty()) {
            System.out.println("--------- 温馨提示 ---------");
            for (String reminder : itinerary.getReminders()) {
                System.out.println("  [!] " + reminder);
            }
            System.out.println();
        }
    }

    /**
     * 将 Markdown 转换为终端友好格式
     */
    private String convertMarkdownToTerminal(String markdown) {
        if (markdown == null) return "";

        // 清理特殊字符（BOM、零宽字符等）
        markdown = markdown.replace("﻿", "");   // BOM
        markdown = markdown.replace("​", "");   // 零宽空格
        markdown = markdown.replace("‌", "");   // 零宽非连接符
        markdown = markdown.replace("‍", "");   // 零宽连接符
        markdown = markdown.replace(" ", " ");  // 不换行空格

        StringBuilder sb = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // 跳过空行
            if (trimmed.isEmpty()) {
                if (inList) {
                    inList = false;
                }
                sb.append("\n");
                continue;
            }

            // 分隔线 --- 或 *** 或 ___
            if (trimmed.matches("^[-*_]{3,}$")) {
                sb.append("  ").append("------------------------------------------------------------").append("\n");
                continue;
            }

            // 标题（兼容 # 标题、#? 标题、## 标题、### 标题）
            if (trimmed.matches("^#{1,3}[^#\\w]?.*")) {
                String content = trimmed.replaceFirst("^#{1,3}[^#\\w]?\\s*", "");
                int level = trimmed.indexOf(' ');
                if (level < 0) level = trimmed.length();
                level = Math.min(level, 3);
                String title = removeMarkdownFormatting(content.trim());

                if (level == 1) {
                    sb.append("\n============================================================\n");
                    sb.append("  ").append(title).append("\n");
                    sb.append("============================================================\n");
                } else if (level == 2) {
                    sb.append("\n------------------------------------------------------------\n");
                    sb.append("  ").append(title).append("\n");
                    sb.append("------------------------------------------------------------\n");
                } else {
                    sb.append("\n  ").append(title).append("\n");
                }
                inList = false;
                continue;
            }

            // ? 装饰标题行
            if (trimmed.matches("^[?❓❓]\\s*.*")) {
                String title = removeMarkdownFormatting(trimmed.replaceFirst("^[?❓❓]\\s*", "").trim());
                if (!title.isEmpty()) {
                    sb.append("\n------------------------------------------------------------\n");
                    sb.append("  ").append(title).append("\n");
                    sb.append("------------------------------------------------------------\n");
                }
                inList = false;
                continue;
            }

            // > 引用行
            if (trimmed.startsWith("> ")) {
                String content = removeMarkdownFormatting(trimmed.substring(2).trim());
                sb.append("  ").append(content).append("\n");
                continue;
            }
            if (trimmed.equals(">")) {
                sb.append("\n");
                continue;
            }

            // 数字列表 1. xxx 2. xxx
            if (trimmed.matches("^\\d+\\.\\s+.*")) {
                String content = removeMarkdownFormatting(trimmed.replaceFirst("^\\d+\\.\\s+", ""));
                String num = trimmed.replaceFirst("\\..*", "");
                sb.append("    ").append(num).append(". ").append(content).append("\n");
                inList = true;
                continue;
            }

            // 列表项 - xxx 或 * xxx
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                String content = removeMarkdownFormatting(trimmed.substring(2).trim());
                sb.append("    - ").append(content).append("\n");
                inList = true;
                continue;
            }

            // 普通文本
            sb.append("  ").append(removeMarkdownFormatting(trimmed)).append("\n");
        }

        return sb.toString();
    }

    /**
     * 去除 Markdown 格式符号
     */
    private String removeMarkdownFormatting(String text) {
        if (text == null) return "";

        // 去掉 **粗体**
        text = text.replaceAll("\\*\\*(.*?)\\*\\*", "$1");

        // 去掉 *斜体*
        text = text.replaceAll("\\*(.*?)\\*", "$1");

        // 去掉 `代码`
        text = text.replaceAll("`([^`]*)`", "$1");

        // 去掉 [链接](url) 只保留文字
        text = text.replaceAll("\\[([^\\]]*)\\]\\([^\\)]*\\)", "$1");

        return text;
    }

    /**
     * 渲染消息
     */
    public void renderMessage(String message) {
        System.out.println(message);
    }

    /**
     * 渲染错误信息
     */
    public void renderError(String error) {
        System.err.println("[错误] " + error);
    }

    /**
     * 渲染帮助信息
     */
    public void renderHelp() {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("                    TravelMind 命令帮助");
        System.out.println("============================================================");
        System.out.println();
        System.out.println("  系统命令:");
        System.out.println("    /help     -  显示此帮助信息");
        System.out.println("    /new      -  开始新会话");
        System.out.println("    /export   -  导出当前行程为 Markdown 文件");
        System.out.println("    /history  -  查看历史行程");
        System.out.println("    /exit     -  退出程序");
        System.out.println();
        System.out.println("  使用示例:");
        System.out.println("    帮我规划去上海的三日旅游的行程");
        System.out.println("    第二天不要去博物馆，换成迪士尼");
        System.out.println();
    }

    /**
     * 渲染欢迎信息
     */
    public void renderWelcome() {
        System.out.println();
        System.out.println("============================================================");
        System.out.println("            TravelMind 智能行程规划助手");
        System.out.println("============================================================");
        System.out.println();
        System.out.println("  欢迎使用 TravelMind！我可以帮你规划旅行行程。");
        System.out.println();
        System.out.println("  试试输入：帮我规划去上海的三日旅游的行程");
        System.out.println();
        System.out.println("  输入 /help 查看所有可用命令。");
        System.out.println();
    }

    /**
     * 渲染退出信息
     */
    public void renderGoodbye() {
        System.out.println();
        System.out.println("  感谢使用 TravelMind，祝你旅途愉快！");
        System.out.println();
    }
}
