package com.travelmind.llm.mimo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travelmind.llm.LlmClient;
import com.travelmind.llm.LlmProperties;
import com.travelmind.llm.LlmRequest;
import com.travelmind.llm.LlmResponse;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 小米 MiMo LLM 客户端实现（Anthropic API 兼容格式）
 */
@Component
public class MimoLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MimoLlmClient.class);

    private static final int DEFAULT_MAX_TOKENS = 2048;

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
            LlmProperties.MimoProperties mimoConfig = llmProperties.getMimo();
            String url = mimoConfig.getBaseUrl() + mimoConfig.getChatPath();

            // 构建 Anthropic 格式请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", mimoConfig.getModel());
            requestBody.put("max_tokens", DEFAULT_MAX_TOKENS);
            requestBody.put("temperature",
                    request.getTemperature() != null ? request.getTemperature() : mimoConfig.getTemperature());

            // Anthropic 格式：system 作为顶层字段
            String systemPrompt = extractSystemPrompt(request);
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                requestBody.put("system", systemPrompt);
            }

            // Anthropic 格式：messages 只包含 user/assistant 消息
            ArrayNode messagesArray = requestBody.putArray("messages");
            for (LlmRequest.Message message : request.getMessages()) {
                if ("system".equals(message.getRole())) {
                    continue; // system 已经在顶层处理
                }
                ObjectNode messageNode = messagesArray.addObject();
                messageNode.put("role", message.getRole());
                messageNode.put("content", message.getContent());
            }

            String requestJson = objectMapper.writeValueAsString(requestBody);

            // 发送请求（Anthropic 使用 x-api-key header）
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("x-api-key", mimoConfig.getApiKey())
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .build();

            log.debug("Sending LLM request to: {}", url);

            try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : "";

                if (!httpResponse.isSuccessful()) {
                    throw new RuntimeException("LLM API call failed with status " + httpResponse.code() + ": " + responseBody);
                }

                // 解析 Anthropic 格式响应
                JsonNode responseJson = objectMapper.readTree(responseBody);
                response.setRawResponse(responseBody);

                // Anthropic 响应格式：content 数组
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

                // 提取 token 信息（Anthropic 使用 input_tokens/output_tokens）
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
