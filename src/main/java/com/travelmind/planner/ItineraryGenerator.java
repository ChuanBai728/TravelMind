package com.travelmind.planner;

import com.travelmind.domain.Itinerary;
import com.travelmind.domain.TripRequest;
import com.travelmind.llm.*;
import com.travelmind.storage.LlmCallLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

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
     * 生成行程（同步）
     */
    public Itinerary generate(TripContext context) {
        LlmClient llmClient = llmClientFactory.getClient();
        LlmRequest request = buildGenerateRequest(context);

        long startTime = System.currentTimeMillis();
        LlmResponse response = null;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            response = llmClient.chat(request);

            Itinerary itinerary = buildItinerary(context, response.getContent());

            saveCallLog(request.getCallType(), response.getPromptTokens(), response.getCompletionTokens(),
                    status, null, System.currentTimeMillis() - startTime, context.getSessionId());

            return itinerary;
        } catch (Exception e) {
            log.error("Itinerary generation failed", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            saveCallLog(request.getCallType(), response != null ? response.getPromptTokens() : null,
                    response != null ? response.getCompletionTokens() : null,
                    status, errorMessage, System.currentTimeMillis() - startTime, context.getSessionId());

            throw e;
        }
    }

    /**
     * 生成行程（流式），文本片段通过 callback 实时回调
     */
    public Itinerary generateStream(TripContext context, Consumer<String> callback) {
        LlmClient llmClient = llmClientFactory.getClient();
        LlmRequest request = buildGenerateRequest(context);

        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        StreamResponse response = null;

        try {
            response = llmClient.chatStream(request, callback);

            Itinerary itinerary = buildItinerary(context, response.getContent());

            saveCallLog(request.getCallType(), response.getPromptTokens(), response.getCompletionTokens(),
                    status, null, System.currentTimeMillis() - startTime, context.getSessionId());

            return itinerary;
        } catch (Exception e) {
            log.error("Itinerary streaming generation failed", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            saveCallLog(request.getCallType(), response != null ? response.getPromptTokens() : null,
                    response != null ? response.getCompletionTokens() : null,
                    status, errorMessage, System.currentTimeMillis() - startTime, context.getSessionId());

            throw e;
        }
    }

    /**
     * 修改行程（同步）
     */
    public Itinerary modify(TripContext context, String currentMarkdown, String userInput) {
        LlmClient llmClient = llmClientFactory.getClient();
        LlmRequest request = buildModifyRequest(context, currentMarkdown, userInput);

        long startTime = System.currentTimeMillis();
        LlmResponse response = null;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            response = llmClient.chat(request);

            Itinerary itinerary = buildItinerary(context, response.getContent());

            saveCallLog(request.getCallType(), response.getPromptTokens(), response.getCompletionTokens(),
                    status, null, System.currentTimeMillis() - startTime, context.getSessionId());

            return itinerary;
        } catch (Exception e) {
            log.error("Itinerary modification failed", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            saveCallLog(request.getCallType(), response != null ? response.getPromptTokens() : null,
                    response != null ? response.getCompletionTokens() : null,
                    status, errorMessage, System.currentTimeMillis() - startTime, context.getSessionId());

            throw e;
        }
    }

    /**
     * 修改行程（流式），文本片段通过 callback 实时回调
     */
    public Itinerary modifyStream(TripContext context, String currentMarkdown,
                                  String userInput, Consumer<String> callback) {
        LlmClient llmClient = llmClientFactory.getClient();
        LlmRequest request = buildModifyRequest(context, currentMarkdown, userInput);

        long startTime = System.currentTimeMillis();
        String status = "SUCCESS";
        String errorMessage = null;
        StreamResponse response = null;

        try {
            response = llmClient.chatStream(request, callback);

            Itinerary itinerary = buildItinerary(context, response.getContent());

            saveCallLog(request.getCallType(), response.getPromptTokens(), response.getCompletionTokens(),
                    status, null, System.currentTimeMillis() - startTime, context.getSessionId());

            return itinerary;
        } catch (Exception e) {
            log.error("Itinerary streaming modification failed", e);
            status = "FAILED";
            errorMessage = e.getMessage();

            saveCallLog(request.getCallType(), response != null ? response.getPromptTokens() : null,
                    response != null ? response.getCompletionTokens() : null,
                    status, errorMessage, System.currentTimeMillis() - startTime, context.getSessionId());

            throw e;
        }
    }

    private LlmRequest buildGenerateRequest(TripContext context) {
        LlmRequest request = new LlmRequest("ITINERARY_GENERATE");
        try {
            TripRequest tripRequest = context.getTripRequest();
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to build generation request", e);
        }
        return request;
    }

    private LlmRequest buildModifyRequest(TripContext context, String currentMarkdown, String userInput) {
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
        } catch (Exception e) {
            throw new RuntimeException("Failed to build modification request", e);
        }
        return request;
    }

    private Itinerary buildItinerary(TripContext context, String content) {
        Itinerary itinerary = new Itinerary();
        itinerary.setSessionId(context.getSessionId());
        itinerary.setRequest(context.getTripRequest());
        itinerary.setMarkdown(content);
        return itinerary;
    }

    private void saveCallLog(String callType, Integer promptTokens, Integer completionTokens,
                             String status, String errorMessage, long latencyMs, Long sessionId) {
        try {
            LlmCallLogRepository.LlmCallLog logEntry = new LlmCallLogRepository.LlmCallLog();
            logEntry.setSessionId(sessionId);
            logEntry.setProvider("mimo");
            logEntry.setModel(llmClientFactory.getClient().getModelName());
            logEntry.setCallType(callType);
            logEntry.setLatencyMs((int) latencyMs);
            logEntry.setStatus(status);
            logEntry.setErrorMessage(errorMessage);
            logEntry.setPromptTokens(promptTokens);
            logEntry.setCompletionTokens(completionTokens);

            llmCallLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to save LLM call log", e);
        }
    }
}
