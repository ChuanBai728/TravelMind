package com.travelmind.planner;

import com.travelmind.domain.Itinerary;
import com.travelmind.domain.TripRequest;
import com.travelmind.llm.LlmClient;
import com.travelmind.llm.LlmClientFactory;
import com.travelmind.llm.LlmRequest;
import com.travelmind.llm.LlmResponse;
import com.travelmind.llm.PromptTemplates;
import com.travelmind.storage.LlmCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 行程生成器
 */
@Component
public class ItineraryGenerator {

    private static final Logger log = LoggerFactory.getLogger(ItineraryGenerator.class);

    private final LlmClientFactory llmClientFactory;
    private final ObjectMapper objectMapper;
    private final LlmCallLogRepository llmCallLogRepository;

    public ItineraryGenerator(LlmClientFactory llmClientFactory, ObjectMapper objectMapper,
                              LlmCallLogRepository llmCallLogRepository) {
        this.llmClientFactory = llmClientFactory;
        this.objectMapper = objectMapper;
        this.llmCallLogRepository = llmCallLogRepository;
    }

    /**
     * 生成行程
     *
     * @param context 规划上下文
     * @return 生成的行程
     */
    public Itinerary generate(TripContext context) {
        LlmClient llmClient = llmClientFactory.getClient();
        LlmRequest request = new LlmRequest("ITINERARY_GENERATE");

        try {
            TripRequest tripRequest = context.getTripRequest();

            // 构建请求内容
            String tripRequestJson = objectMapper.writeValueAsString(tripRequest);
            String candidatePoisJson = objectMapper.writeValueAsString(context.getCandidatePois());
            String routeInfosJson = objectMapper.writeValueAsString(context.getRouteInfos());

            request.addMessage("system", PromptTemplates.ITINERARY_GENERATE_SYSTEM);
            request.addMessage("user", String.format(
                    PromptTemplates.ITINERARY_GENERATE_USER,
                    tripRequestJson,
                    PromptTemplates.DEFAULT_ASSUMPTIONS,
                    candidatePoisJson,
                    routeInfosJson
            ));

            long startTime = System.currentTimeMillis();
            LlmResponse response = null;
            String status = "SUCCESS";
            String errorMessage = null;

            try {
                response = llmClient.chat(request);

                // 构建行程对象
                Itinerary itinerary = new Itinerary();
                itinerary.setSessionId(context.getSessionId());
                itinerary.setRequest(tripRequest);
                itinerary.setMarkdown(response.getContent());

                // 记录调用日志
                saveCallLog(request, response, status, null,
                        System.currentTimeMillis() - startTime, context.getSessionId());

                return itinerary;
            } catch (Exception e) {
                log.error("Itinerary generation failed", e);
                status = "FAILED";
                errorMessage = e.getMessage();

                // 记录调用日志
                saveCallLog(request, response, status, errorMessage,
                        System.currentTimeMillis() - startTime, context.getSessionId());

                throw e;
            }
        } catch (Exception e) {
            log.error("Failed to prepare itinerary generation request", e);
            throw new RuntimeException("行程生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 修改行程
     *
     * @param context         规划上下文
     * @param currentMarkdown 当前行程 Markdown
     * @param userInput       用户修改要求
     * @return 修改后的行程
     */
    public Itinerary modify(TripContext context, String currentMarkdown, String userInput) {
        LlmClient llmClient = llmClientFactory.getClient();
        LlmRequest request = new LlmRequest("ITINERARY_MODIFY");

        try {
            String relatedPoisJson = objectMapper.writeValueAsString(context.getCandidatePois());

            request.addMessage("system", PromptTemplates.ITINERARY_MODIFY_SYSTEM);
            request.addMessage("user", String.format(
                    PromptTemplates.ITINERARY_MODIFY_USER,
                    currentMarkdown,
                    userInput,
                    relatedPoisJson
            ));

            long startTime = System.currentTimeMillis();
            LlmResponse response = null;
            String status = "SUCCESS";
            String errorMessage = null;

            try {
                response = llmClient.chat(request);

                // 构建行程对象
                Itinerary itinerary = new Itinerary();
                itinerary.setSessionId(context.getSessionId());
                itinerary.setRequest(context.getTripRequest());
                itinerary.setMarkdown(response.getContent());

                // 记录调用日志
                saveCallLog(request, response, status, null,
                        System.currentTimeMillis() - startTime, context.getSessionId());

                return itinerary;
            } catch (Exception e) {
                log.error("Itinerary modification failed", e);
                status = "FAILED";
                errorMessage = e.getMessage();

                // 记录调用日志
                saveCallLog(request, response, status, errorMessage,
                        System.currentTimeMillis() - startTime, context.getSessionId());

                throw e;
            }
        } catch (Exception e) {
            log.error("Failed to prepare itinerary modification request", e);
            throw new RuntimeException("行程修改失败: " + e.getMessage(), e);
        }
    }

    private void saveCallLog(LlmRequest request, LlmResponse response, String status,
                             String errorMessage, long latencyMs, Long sessionId) {
        try {
            LlmCallLogRepository.LlmCallLog logEntry = new LlmCallLogRepository.LlmCallLog();
            logEntry.setSessionId(sessionId);
            logEntry.setProvider("mimo");
            logEntry.setModel(llmClientFactory.getClient().getModelName());
            logEntry.setCallType(request.getCallType());
            logEntry.setLatencyMs((int) latencyMs);
            logEntry.setStatus(status);
            logEntry.setErrorMessage(errorMessage);

            if (response != null) {
                logEntry.setPromptTokens(response.getPromptTokens());
                logEntry.setCompletionTokens(response.getCompletionTokens());
                logEntry.setResponseJson(response.getRawResponse());
            }

            llmCallLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to save LLM call log", e);
        }
    }
}
