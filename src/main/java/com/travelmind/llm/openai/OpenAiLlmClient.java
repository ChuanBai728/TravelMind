package com.travelmind.llm.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travelmind.llm.LlmClient;
import com.travelmind.llm.LlmRequest;
import com.travelmind.llm.LlmResponse;
import com.travelmind.llm.LlmProperties;
import com.travelmind.llm.StreamResponse;
import okhttp3.*;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * OpenAI 兼容格式 LLM 客户端（适用于硅基流动等平台）
 */
@Component
public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private static final int DEFAULT_MAX_TOKENS = 4096;

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public OpenAiLlmClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;

        LlmProperties.OpenAiProperties config = llmProperties.getOpenai();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
                .build();
    }

    @Override
    public LlmResponse chat(LlmRequest request) {
        long startTime = System.currentTimeMillis();
        LlmResponse response = new LlmResponse();

        try {
            String url = buildUrl();
            String requestJson = buildRequestBody(request, false);

            Request httpRequest = buildHttpRequest(url, requestJson);

            log.debug("Sending OpenAI LLM request to: {}", url);

            try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : "";

                if (!httpResponse.isSuccessful()) {
                    throw new RuntimeException("LLM API call failed with status " + httpResponse.code() + ": " + responseBody);
                }

                JsonNode responseJson = objectMapper.readTree(responseBody);
                response.setRawResponse(responseBody);

                JsonNode choices = responseJson.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).path("message").path("content").asText("");
                    response.setContent(content);
                }

                JsonNode usage = responseJson.get("usage");
                if (usage != null) {
                    if (usage.has("prompt_tokens")) {
                        response.setPromptTokens(usage.get("prompt_tokens").asInt());
                    }
                    if (usage.has("completion_tokens")) {
                        response.setCompletionTokens(usage.get("completion_tokens").asInt());
                    }
                }
            }
        } catch (IOException e) {
            log.error("OpenAI LLM API call failed", e);
            throw new RuntimeException("LLM API call failed: " + e.getMessage(), e);
        } finally {
            response.setLatencyMs(System.currentTimeMillis() - startTime);
        }

        return response;
    }

    @Override
    public StreamResponse chatStream(LlmRequest request, Consumer<String> callback) {
        long startTime = System.currentTimeMillis();
        StreamResponse response = new StreamResponse();

        try {
            String url = buildUrl();
            String requestJson = buildRequestBody(request, true);

            Request httpRequest = buildHttpRequest(url, requestJson);

            log.debug("Sending streaming OpenAI LLM request to: {}", url);

            try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
                if (!httpResponse.isSuccessful()) {
                    String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "";
                    throw new RuntimeException("LLM API call failed with status " + httpResponse.code() + ": " + errorBody);
                }

                BufferedSource source = httpResponse.body().source();
                StringBuilder fullContent = new StringBuilder();
                Integer promptTokens = null;
                Integer completionTokens = null;

                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) break;

                    if (!line.startsWith("data: ")) continue;

                    String data = line.substring(6).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;

                    try {
                        JsonNode event = objectMapper.readTree(data);

                        JsonNode choices = event.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).path("delta");
                            String text = delta.path("content").asText("");
                            if (!text.isEmpty()) {
                                fullContent.append(text);
                                if (callback != null) {
                                    callback.accept(text);
                                }
                            }
                        }

                        JsonNode usage = event.get("usage");
                        if (usage != null) {
                            if (usage.has("prompt_tokens")) {
                                promptTokens = usage.get("prompt_tokens").asInt();
                            }
                            if (usage.has("completion_tokens")) {
                                completionTokens = usage.get("completion_tokens").asInt();
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Failed to parse SSE data: {}", data, e);
                    }
                }

                response.setContent(fullContent.toString());
                response.setPromptTokens(promptTokens);
                response.setCompletionTokens(completionTokens);
            }
        } catch (IOException e) {
            log.error("Streaming OpenAI LLM API call failed", e);
            throw new RuntimeException("LLM API call failed: " + e.getMessage(), e);
        } finally {
            response.setLatencyMs(System.currentTimeMillis() - startTime);
        }

        return response;
    }

    private String buildUrl() {
        LlmProperties.OpenAiProperties config = llmProperties.getOpenai();
        return config.getBaseUrl() + config.getChatPath();
    }

    private String buildRequestBody(LlmRequest request, boolean stream) throws IOException {
        LlmProperties.OpenAiProperties config = llmProperties.getOpenai();

        ObjectNode requestBody = objectMapper.createObjectNode();
        String model = (request.getModel() != null && !request.getModel().isEmpty())
                ? request.getModel() : config.getModel();
        requestBody.put("model", model);
        requestBody.put("max_tokens", DEFAULT_MAX_TOKENS);
        requestBody.put("temperature",
                request.getTemperature() != null ? request.getTemperature() : config.getTemperature());

        if (stream) {
            requestBody.put("stream", true);
        }

        ArrayNode messagesArray = requestBody.putArray("messages");
        for (LlmRequest.Message message : request.getMessages()) {
            ObjectNode messageNode = messagesArray.addObject();
            messageNode.put("role", message.getRole());
            messageNode.put("content", message.getContent());
        }

        return objectMapper.writeValueAsString(requestBody);
    }

    private Request buildHttpRequest(String url, String requestJson) {
        LlmProperties.OpenAiProperties config = llmProperties.getOpenai();
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                .build();
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public String getModelName() {
        return llmProperties.getOpenai().getModel();
    }
}
