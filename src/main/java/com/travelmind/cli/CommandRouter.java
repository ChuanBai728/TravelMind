package com.travelmind.cli;

import com.travelmind.conversation.ConversationManager;
import com.travelmind.domain.Itinerary;
import com.travelmind.domain.TripRequest;
import com.travelmind.storage.ItineraryRepository;
import com.travelmind.storage.TripRequestRepository;
import com.travelmind.export.MarkdownExporter;
import com.travelmind.planner.TripPlannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * 命令路由器
 */
@Component
public class CommandRouter {

    private static final Logger log = LoggerFactory.getLogger(CommandRouter.class);

    private final TripPlannerService tripPlannerService;
    private final ConversationManager conversationManager;
    private final MarkdownExporter markdownExporter;
    private final TripRequestRepository tripRequestRepository;
    private final CliRenderer cliRenderer;

    private Long currentSessionId;

    public CommandRouter(TripPlannerService tripPlannerService, ConversationManager conversationManager,
                         MarkdownExporter markdownExporter, TripRequestRepository tripRequestRepository,
                         CliRenderer cliRenderer) {
        this.tripPlannerService = tripPlannerService;
        this.conversationManager = conversationManager;
        this.markdownExporter = markdownExporter;
        this.tripRequestRepository = tripRequestRepository;
        this.cliRenderer = cliRenderer;
    }

    /**
     * 处理用户输入
     *
     * @param input 用户输入
     * @return 是否继续运行
     */
    public boolean handle(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }

        String trimmed = input.trim();

        // 解析命令
        ShellCommand command = parseCommand(trimmed);

        switch (command) {
            case HELP:
                showHelp();
                break;
            case NEW:
                handleNew();
                break;
            case EXPORT:
                handleExport();
                break;
            case HISTORY:
                handleHistory();
                break;
            case EXIT:
                return false;
            case INPUT:
                handleInput(trimmed);
                break;
        }

        return true;
    }

    private ShellCommand parseCommand(String input) {
        if (input.startsWith("/")) {
            String command = input.toLowerCase();
            switch (command) {
                case "/help":
                    return ShellCommand.HELP;
                case "/new":
                    return ShellCommand.NEW;
                case "/export":
                    return ShellCommand.EXPORT;
                case "/history":
                    return ShellCommand.HISTORY;
                case "/exit":
                    return ShellCommand.EXIT;
                default:
                    cliRenderer.renderError("未知命令: " + input + "。输入 /help 查看帮助。");
                    return ShellCommand.HELP;
            }
        }
        return ShellCommand.INPUT;
    }

    private void showHelp() {
        cliRenderer.renderHelp();
    }

    private void handleNew() {
        currentSessionId = conversationManager.createSession();
        cliRenderer.renderMessage("新会话已创建，会话 ID: " + currentSessionId);
        cliRenderer.renderMessage("请输入你的旅行需求，例如：帮我规划去上海的三日旅游的行程");
    }

    private void handleExport() {
        if (currentSessionId == null) {
            cliRenderer.renderError("请先创建一个新会话或输入旅行需求。");
            return;
        }

        try {
            // 获取当前行程
            List<ItineraryRepository.Itinerary> itineraries = conversationManager.getHistoryItineraries(currentSessionId, 1);
            if (itineraries.isEmpty()) {
                cliRenderer.renderError("当前会话没有行程，请先创建行程。");
                return;
            }

            ItineraryRepository.Itinerary latestItinerary = itineraries.get(0);
            Itinerary itinerary = new Itinerary();
            itinerary.setId(latestItinerary.getId());
            itinerary.setSessionId(latestItinerary.getSessionId());
            itinerary.setMarkdown(latestItinerary.getMarkdownContent());

            // 恢复 TripRequest 以获取目的地和天数信息
            if (latestItinerary.getRequestId() != null) {
                TripRequestRepository.TripRequest requestEntity = tripRequestRepository.findById(latestItinerary.getRequestId());
                if (requestEntity != null) {
                    TripRequest request = new TripRequest();
                    request.setDestination(requestEntity.getDestination());
                    request.setDurationDays(requestEntity.getDurationDays());
                    itinerary.setRequest(request);
                }
            }

            // 导出
            Path exportPath = markdownExporter.export(itinerary);
            cliRenderer.renderMessage("行程已导出到: " + exportPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Export failed", e);
            cliRenderer.renderError("导出失败: " + e.getMessage());
        }
    }

    private void handleHistory() {
        if (currentSessionId == null) {
            cliRenderer.renderError("请先创建一个新会话或输入旅行需求。");
            return;
        }

        try {
            List<ItineraryRepository.Itinerary> itineraries = conversationManager.getHistoryItineraries(currentSessionId, 10);
            if (itineraries.isEmpty()) {
                cliRenderer.renderMessage("当前会话没有历史行程。");
                return;
            }

            cliRenderer.renderMessage("=== 历史行程 ===");
            for (int i = 0; i < itineraries.size(); i++) {
                ItineraryRepository.Itinerary entity = itineraries.get(i);
                cliRenderer.renderMessage(String.format("%d. %s (版本: %d, 创建时间: %s)",
                        i + 1,
                        entity.getTitle(),
                        entity.getVersion(),
                        entity.getCreatedAt()));
            }
        } catch (Exception e) {
            log.error("Failed to get history", e);
            cliRenderer.renderError("获取历史行程失败: " + e.getMessage());
        }
    }

    private void handleInput(String input) {
        if (currentSessionId == null) {
            // 自动创建新会话
            currentSessionId = conversationManager.createSession();
            log.info("Auto-created new session: {}", currentSessionId);
        }

        try {
            // 处理用户消息
            Itinerary result = tripPlannerService.handleUserMessage(currentSessionId, input);

            // 渲染结果
            if (result != null && result.getMarkdown() != null) {
                cliRenderer.renderItinerary(result);

                // 只有真实保存过的行程才更新当前会话，避免追问/错误提示清空上下文。
                if (result.getId() != null) {
                    conversationManager.updateContext(currentSessionId, result.getId(), input);
                }
            } else {
                cliRenderer.renderMessage("抱歉，我无法处理你的请求。请尝试描述你的旅行需求。");
            }
        } catch (Exception e) {
            log.error("Failed to handle user input", e);
            cliRenderer.renderError("处理请求时出错: " + e.getMessage());
        }
    }
}
