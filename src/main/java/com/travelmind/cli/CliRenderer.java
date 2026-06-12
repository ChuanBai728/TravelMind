package com.travelmind.cli;

import com.travelmind.domain.Itinerary;
import org.springframework.stereotype.Component;

/**
 * CLI 渲染器
 */
@Component
public class CliRenderer {

    /**
     * 渲染行程
     *
     * @param itinerary 行程对象
     */
    public void renderItinerary(Itinerary itinerary) {
        if (itinerary == null || itinerary.getMarkdown() == null) {
            System.out.println("暂无行程内容");
            return;
        }

        System.out.println();
        System.out.println(itinerary.getMarkdown());
        System.out.println();

        // 显示提醒信息
        if (itinerary.getReminders() != null && !itinerary.getReminders().isEmpty()) {
            System.out.println("--- 温馨提示 ---");
            for (String reminder : itinerary.getReminders()) {
                System.out.println("• " + reminder);
            }
            System.out.println();
        }
    }

    /**
     * 渲染消息
     *
     * @param message 消息内容
     */
    public void renderMessage(String message) {
        System.out.println(message);
    }

    /**
     * 渲染错误信息
     *
     * @param error 错误信息
     */
    public void renderError(String error) {
        System.err.println("错误: " + error);
    }

    /**
     * 渲染帮助信息
     */
    public void renderHelp() {
        System.out.println();
        System.out.println("=== TravelMind 命令帮助 ===");
        System.out.println();
        System.out.println("系统命令:");
        System.out.println("  /help     - 显示此帮助信息");
        System.out.println("  /new      - 开始新会话");
        System.out.println("  /export   - 导出当前行程为 Markdown 文件");
        System.out.println("  /history  - 查看历史行程");
        System.out.println("  /exit     - 退出程序");
        System.out.println();
        System.out.println("使用示例:");
        System.out.println("  帮我规划去上海的三日旅游的行程");
        System.out.println("  第二天不要去博物馆，换成迪士尼");
        System.out.println();
    }

    /**
     * 渲染欢迎信息
     */
    public void renderWelcome() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                 TravelMind 智能行程规划助手                 ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("欢迎使用 TravelMind！我可以帮你规划旅行行程。");
        System.out.println();
        System.out.println("试试输入：帮我规划去上海的三日旅游的行程");
        System.out.println();
        System.out.println("输入 /help 查看所有可用命令。");
        System.out.println();
    }

    /**
     * 渲染退出信息
     */
    public void renderGoodbye() {
        System.out.println();
        System.out.println("感谢使用 TravelMind，祝你旅途愉快！");
        System.out.println();
    }
}
