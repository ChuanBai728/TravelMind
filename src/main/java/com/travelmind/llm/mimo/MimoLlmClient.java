package com.travelmind.llm.mimo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travelmind.llm.LlmClient;
import com.travelmind.llm.LlmProperties;
import com.travelmind.llm.LlmRequest;
import com.travelmind.llm.LlmResponse;
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
 * Anthropic Messages-compatible LLM client implementation.
 */
@Component
public class MimoLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MimoLlmClient.class);

    private static final int DEFAULT_MAX_TOKENS = 4096;

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public MimoLlmClient(LlmProperties llmProperties, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;

        LlmProperties.MimoProperties mimoConfig = llmProperties.getMimo();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(mimoConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(mimoConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
                .writeTimeout(mimoConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
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

            log.debug("Sending LLM request to: {}", url);

            try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : "";

                if (!httpResponse.isSuccessful()) {
                    throw new RuntimeException("LLM API call failed with status " + httpResponse.code() + ": " + responseBody);
                }

                JsonNode responseJson = objectMapper.readTree(responseBody);
                response.setRawResponse(responseBody);

                JsonNode content = responseJson.get("content");
                if (content != null && content.isArray() && content.size() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode block : content) {
                        if ("text".equals(block.path("type").asText())) {
                            sb.append(block.path("text").asText());
                        }
                    }
                    response.setContent(sb.toString());
                }

                JsonNode usage = responseJson.get("usage");
                if (usage != null) {
                    if (usage.has("input_tokens")) {
                        response.setPromptTokens(usage.get("input_tokens").asInt());
                    }
                    if (usage.has("output_tokens")) {
                        response.setCompletionTokens(usage.get("output_tokens").asInt());
                    }
                }
            }
        } catch (IOException e) {
            log.error("LLM API call failed", e);
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

            log.debug("Sending streaming LLM request to: {}", url);

            try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
                if (!httpResponse.isSuccessful()) {
                    String errorBody = httpResponse.body() != null ? httpResponse.body().string() : "";
                    throw new RuntimeException("LLM API call failed with status " + httpResponse.code() + ": " + errorBody);
                }

                BufferedSource source = httpResponse.body().source();
                StringBuilder fullContent = new StringBuilder();
                Integer inputTokens = null;
                Integer outputTokens = null;

                while (!source.exhausted()) {
                    String line = source.readUtf8Line();
                    if (line == null) break;

                    // Anthropic SSE: "event: xxx" followed by "data: {...}"
                    if (!line.startsWith("data: ")) continue;

                    String data = line.substring(6).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) continue;

                    try {
                        JsonNode event = objectMapper.readTree(data);
                        String type = event.path("type").asText();

                        switch (type) {
                            case "message_start":
                                JsonNode msgUsage = event.path("message").path("usage");
                                if (msgUsage.has("input_tokens")) {
                                    inputTokens = msgUsage.get("input_tokens").asInt();
                                }
                                break;

                            case "content_block_delta":
                                String text = event.path("delta").path("text").asText("");
                                if (!text.isEmpty()) {
                                    fullContent.append(text);
                                    if (callback != null) {
                                        callback.accept(text);
                                    }
                                }
                                break;

                            case "message_delta":
                                JsonNode deltaUsage = event.path("usage");
                                if (deltaUsage.has("output_tokens")) {
                                    outputTokens = deltaUsage.get("output_tokens").asInt();
                                }
                                break;

                            default:
                                break;
                        }
                    } catch (Exception e) {
                        log.debug("Failed to parse SSE data: {}", data, e);
                    }
                }

                response.setContent(fullContent.toString());
                response.setPromptTokens(inputTokens);
                response.setCompletionTokens(outputTokens);
            }
        } catch (IOException e) {
            log.error("Streaming LLM API call failed", e);
            throw new RuntimeException("LLM API call failed: " + e.getMessage(), e);
        } finally {
            response.setLatencyMs(System.currentTimeMillis() - startTime);
        }

        log.info("Streaming completed in {}ms, tokens: {}/{}",
                response.getLatencyMs(), response.getPromptTokens(), response.getCompletionTokens());

        return response;
    }

    private String buildUrl() {
        LlmProperties.MimoProperties mimoConfig = llmProperties.getMimo();
        return mimoConfig.getBaseUrl() + mimoConfig.getChatPath();
    }

    private String buildRequestBody(LlmRequest request, boolean stream) throws IOException {
        LlmProperties.MimoProperties mimoConfig = llmProperties.getMimo();

        ObjectNode requestBody = objectMapper.createObjectNode();
        String model = (request.getModel() != null && !request.getModel().isEmpty())
                ? request.getModel() : mimoConfig.getModel();
        requestBody.put("model", model);
        requestBody.put("max_tokens", DEFAULT_MAX_TOKENS);
        requestBody.put("temperature",
                request.getTemperature() != null ? request.getTemperature() : mimoConfig.getTemperature());

        if (stream) {
            requestBody.put("stream", true);
        }

        // Anthropic 格式：system 作为顶层字段
        String systemPrompt = extractSystemPrompt(request);
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            requestBody.put("system", systemPrompt);
        }

        // Anthropic 格式：messages 只包含 user/assistant 消息
        ArrayNode messagesArray = requestBody.putArray("messages");
        for (LlmRequest.Message message : request.getMessages()) {
            if ("system".equals(message.getRole())) {
                continue;
            }
            ObjectNode messageNode = messagesArray.addObject();
            messageNode.put("role", message.getRole());
            messageNode.put("content", message.getContent());
        }

        return objectMapper.writeValueAsString(requestBody);
    }

    private Request buildHttpRequest(String url, String requestJson) {
        LlmProperties.MimoProperties mimoConfig = llmProperties.getMimo();
        return new Request.Builder()
                .url(url)
                .addHeader("x-api-key", mimoConfig.getApiKey())
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                .build();
    }

    private String extractSystemPrompt(LlmRequest request) {
        for (LlmRequest.Message message : request.getMessages()) {
            if ("system".equals(message.getRole())) {
                return message.getContent();
            }
        }
        return null;
    }

    @Override
    public String getProviderName() {
        return "mimo";
    }

    @Override
    public String getModelName() {
        return llmProperties.getMimo().getModel();
    }
}
