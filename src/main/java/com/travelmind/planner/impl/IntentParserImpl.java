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
import com.travelmind.storage.LlmCallLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 意图解析器实现
 */
@Component
public class IntentParserImpl implements IntentParser {

    private static final Logger log = LoggerFactory.getLogger(IntentParserImpl.class);

    /**
     * 快速路径正则：匹配 "帮我规划去南京的一日游" 等常见句式
     * 捕获组：1=目的地, 2=天数
     */
    private static final Pattern QUICK_NEW_PLAN = Pattern.compile(
            "(?:帮我)?(?:规划|制定|安排).*?去([^的,，。！!?？\\s]+?)的?(\\d+|[一二三四五六七八九十]+)[日天]",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 中文数字到阿拉伯数字映射
     */
    private static final String[] CN_NUMS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    private final LlmClientFactory llmClientFactory;
    private final ObjectMapper objectMapper;
    private final LlmCallLogRepository llmCallLogRepository;

    public IntentParserImpl(LlmClientFactory llmClientFactory, ObjectMapper objectMapper,
                            LlmCallLogRepository llmCallLogRepository) {
        this.llmClientFactory = llmClientFactory;
        this.objectMapper = objectMapper;
        this.llmCallLogRepository = llmCallLogRepository;
    }

    @Override
    public ParsedIntent parse(String userInput, TripContext context) {
        // 快速路径：正则匹配常见句式，跳过 LLM 调用
        ParsedIntent quickResult = tryQuickParse(userInput);
        if (quickResult != null) {
            log.info("Quick parse matched, skipping LLM intent parsing");
            return quickResult;
        }

        // 慢路径：LLM 意图解析
        return parseWithLlm(userInput, context);
    }

    /**
     * 尝试正则快速解析，匹配常见模式时直接返回，避免 LLM 调用
     */
    private ParsedIntent tryQuickParse(String input) {
        Matcher matcher = QUICK_NEW_PLAN.matcher(input);
        if (!matcher.find()) {
            return null;
        }

        String destination = matcher.group(1).trim();
        String daysStr = matcher.group(2).trim();

        if (destination.isEmpty() || daysStr.isEmpty()) {
            return null;
        }

        int days = parseDays(daysStr);
        if (days <= 0 || days > 30) {
            return null;
        }

        TripRequest tripRequest = new TripRequest();
        tripRequest.setRawInput(input);
        tripRequest.setDestination(destination);
        tripRequest.setDurationDays(days);

        // 尝试提取人数
        Pattern peoplePattern = Pattern.compile("(\\d+)\\s*人");
        Matcher peopleMatcher = peoplePattern.matcher(input);
        if (peopleMatcher.find()) {
            tripRequest.setPeopleCount(Integer.parseInt(peopleMatcher.group(1)));
        }

        // 尝试提取日期
        Pattern datePattern = Pattern.compile("(\\d{4})[年./-](\\d{1,2})[月./-](\\d{1,2})");
        Matcher dateMatcher = datePattern.matcher(input);
        if (dateMatcher.find()) {
            try {
                String dateStr = String.format("%s-%02d-%02d",
                        dateMatcher.group(1),
                        Integer.parseInt(dateMatcher.group(2)),
                        Integer.parseInt(dateMatcher.group(3)));
                tripRequest.setStartDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE));
            } catch (Exception ignored) {
            }
        }

        ParsedIntent intent = new ParsedIntent();
        intent.setIntent("NEW_PLAN");
        intent.setTripRequest(tripRequest);
        intent.setNeedClarification(false);
        return intent;
    }

    private int parseDays(String daysStr) {
        try {
            return Integer.parseInt(daysStr);
        } catch (NumberFormatException e) {
            // 中文数字
            for (int i = 0; i < CN_NUMS.length; i++) {
                if (CN_NUMS[i].equals(daysStr)) {
                    return i;
                }
            }
            // 十一、十二...
            if (daysStr.startsWith("十") && daysStr.length() == 2) {
                int unit = indexOfCN(daysStr.charAt(1));
                if (unit > 0) return 10 + unit;
            }
            return -1;
        }
    }

    private int indexOfCN(char c) {
        for (int i = 0; i < CN_NUMS.length; i++) {
            if (CN_NUMS[i].charAt(0) == c) return i;
        }
        return -1;
    }

    private ParsedIntent parseWithLlm(String userInput, TripContext context) {
        LlmClient llmClient = llmClientFactory.getClient();
        LlmRequest request = new LlmRequest("INTENT_PARSE");

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
            saveCallLog(request, response, status, errorMessage,
                    System.currentTimeMillis() - startTime, context.getSessionId());
        }
    }

    private ParsedIntent parseResponse(String content, String userInput) {
        try {
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

            intent.setIntent(root.path("intent").asText("UNKNOWN"));

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

            intent.setNeedClarification(root.path("needClarification").asBoolean(false));

            JsonNode questions = root.get("questions");
            if (questions != null && questions.isArray()) {
                List<String> questionList = new ArrayList<>();
                for (JsonNode q : questions) {
                    questionList.add(q.asText());
                }
                intent.setQuestions(questionList);
            }

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
