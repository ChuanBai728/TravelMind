package com.travelmind.planner.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmind.domain.TripRequest;
import com.travelmind.llm.LlmClient;
import com.travelmind.llm.LlmClientFactory;
import com.travelmind.llm.LlmRequest;
import com.travelmind.llm.LlmResponse;
import com.travelmind.llm.PromptTemplates;
import com.travelmind.planner.IntentParser;
import com.travelmind.planner.TripContext;
import com.travelmind.repository.LlmCallLogMapper;
import com.travelmind.entity.LlmCallLogEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 意图解析器实现
 */
@Component
public class IntentParserImpl implements IntentParser {

    private static final Logger log = LoggerFactory.getLogger(IntentParserImpl.class);

    private final LlmClientFactory llmClientFactory;
    private final ObjectMapper objectMapper;
    private final LlmCallLogMapper llmCallLogMapper;

    public IntentParserImpl(LlmClientFactory llmClientFactory, ObjectMapper objectMapper,
                            LlmCallLogMapper llmCallLogMapper) {
        this.llmClientFactory = llmClientFactory;
        this.objectMapper = objectMapper;
        this.llmCallLogMapper = llmCallLogMapper;
    }

    @Override
    public ParsedIntent parse(String userInput, TripContext context) {
        LlmClient llmClient = llmClientFactory.getClient();
        LlmRequest request = new LlmRequest("INTENT_PARSE");

        // 构建上下文摘要
        String contextSummary = buildContextSummary(context);

        request.addMessage("system", PromptTemplates.INTENT_PARSE_SYSTEM);
        request.addMessage("user", String.format(PromptTemplates.INTENT_PARSE_USER, contextSummary, userInput));

        long startTime = System.currentTimeMillis();
        LlmResponse response = null;
        String status = "SUCCESS";
        String errorMessage = null;

        try {
            response = llmClient.chat(request);
            return parseResponse(response.getContent(), userInput);
        } catch (Exception e) {
            log.error("Intent parsing failed", e);
            status = "FAILED";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            // 记录调用日志
            saveCallLog(request, response, status, errorMessage,
                    System.currentTimeMillis() - startTime, context.getSessionId());
        }
    }

    private ParsedIntent parseResponse(String content, String userInput) {
        try {
            // 清理响应内容，移除可能的 Markdown 标记
            String jsonContent = content.trim();
            if (jsonContent.startsWith("```json")) {
                jsonContent = jsonContent.substring(7);
            }
            if (jsonContent.startsWith("```")) {
                jsonContent = jsonContent.substring(3);
            }
            if (jsonContent.endsWith("```")) {
                jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
            }
            jsonContent = jsonContent.trim();

            JsonNode root = objectMapper.readTree(jsonContent);
            ParsedIntent intent = new ParsedIntent();

            // 解析 intent
            intent.setIntent(root.path("intent").asText("UNKNOWN"));

            // 解析 tripRequest
            TripRequest tripRequest = new TripRequest();
            tripRequest.setRawInput(userInput);

            JsonNode destination = root.get("destination");
            if (destination != null && !destination.isNull()) {
                tripRequest.setDestination(destination.asText());
            }

            JsonNode durationDays = root.get("durationDays");
            if (durationDays != null && !durationDays.isNull()) {
                tripRequest.setDurationDays(durationDays.asInt());
            }

            JsonNode startDate = root.get("startDate");
            if (startDate != null && !startDate.isNull() && !startDate.asText().equals("null")) {
                try {
                    tripRequest.setStartDate(LocalDate.parse(startDate.asText(), DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (Exception e) {
                    log.warn("Failed to parse startDate: {}", startDate.asText());
                }
            }

            JsonNode peopleCount = root.get("peopleCount");
            if (peopleCount != null && !peopleCount.isNull()) {
                tripRequest.setPeopleCount(peopleCount.asInt());
            }

            JsonNode budgetLevel = root.get("budgetLevel");
            if (budgetLevel != null && !budgetLevel.isNull()) {
                tripRequest.setBudgetLevel(budgetLevel.asText());
            }

            JsonNode paceLevel = root.get("paceLevel");
            if (paceLevel != null && !paceLevel.isNull()) {
                tripRequest.setPaceLevel(paceLevel.asText());
            }

            JsonNode transportMode = root.get("transportMode");
            if (transportMode != null && !transportMode.isNull()) {
                tripRequest.setTransportMode(transportMode.asText());
            }

            JsonNode preferences = root.get("preferences");
            if (preferences != null && preferences.isArray()) {
                List<String> prefList = new ArrayList<>();
                for (JsonNode pref : preferences) {
                    prefList.add(pref.asText());
                }
                tripRequest.setPreferences(prefList);
            }

            intent.setTripRequest(tripRequest);

            // 解析 needClarification
            intent.setNeedClarification(root.path("needClarification").asBoolean(false));

            // 解析 questions
            JsonNode questions = root.get("questions");
            if (questions != null && questions.isArray()) {
                List<String> questionList = new ArrayList<>();
                for (JsonNode q : questions) {
                    questionList.add(q.asText());
                }
                intent.setQuestions(questionList);
            }

            // 解析 modificationInstruction
            JsonNode modInstruction = root.get("modificationInstruction");
            if (modInstruction != null && !modInstruction.isNull()) {
                intent.setModificationInstruction(modInstruction.asText());
            }

            return intent;
        } catch (Exception e) {
            log.error("Failed to parse intent response: {}", content, e);
            ParsedIntent fallback = new ParsedIntent();
            fallback.setIntent("UNKNOWN");
            fallback.setNeedClarification(true);
            fallback.setQuestions(List.of("抱歉，我没有理解你的意思，请重新描述一下你的旅行需求。"));
            return fallback;
        }
    }

    private String buildContextSummary(TripContext context) {
        if (context == null) {
            return "无";
        }

        StringBuilder sb = new StringBuilder();
        if (context.getCurrentItinerary() != null) {
            sb.append("当前行程：");
            sb.append(context.getCurrentItinerary().getRequest().getDestination());
            sb.append(" ");
            sb.append(context.getCurrentItinerary().getRequest().getDurationDays());
            sb.append("日游\n");
        } else {
            sb.append("当前无行程\n");
        }

        if (context.getUserInput() != null) {
            sb.append("上一条用户输入：").append(context.getUserInput());
        }

        return sb.toString();
    }

    private void saveCallLog(LlmRequest request, LlmResponse response, String status,
                             String errorMessage, long latencyMs, Long sessionId) {
        try {
            LlmCallLogEntity logEntity = new LlmCallLogEntity();
            logEntity.setSessionId(sessionId);
            logEntity.setProvider("mimo");
            logEntity.setModel(llmClientFactory.getClient().getModelName());
            logEntity.setCallType(request.getCallType());
            logEntity.setLatencyMs((int) latencyMs);
            logEntity.setStatus(status);
            logEntity.setErrorMessage(errorMessage);

            if (response != null) {
                logEntity.setPromptTokens(response.getPromptTokens());
                logEntity.setCompletionTokens(response.getCompletionTokens());
                logEntity.setResponseJson(response.getRawResponse());
            }

            llmCallLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("Failed to save LLM call log", e);
        }
    }
}
