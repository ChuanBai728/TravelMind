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
 * 小米 MiMo LLM 客户端实现
 */
@Component
public class MimoLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MimoLlmClient.class);

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

            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", mimoConfig.getModel());
            requestBody.put("temperature",
                    request.getTemperature() != null ? request.getTemperature() : mimoConfig.getTemperature());

            ArrayNode messagesArray = requestBody.putArray("messages");
            for (LlmRequest.Message message : request.getMessages()) {
                ObjectNode messageNode = messagesArray.addObject();
                messageNode.put("role", message.getRole());
                messageNode.put("content", message.getContent());
            }

            String requestJson = objectMapper.writeValueAsString(requestBody);

            // 发送请求
            Request httpRequest = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + mimoConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestJson, MediaType.parse("application/json")))
                    .build();

            log.debug("Sending LLM request to: {}", url);

            try (Response httpResponse = httpClient.newCall(httpRequest).execute()) {
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : "";

                if (!httpResponse.isSuccessful()) {
                    throw new RuntimeException("LLM API call failed with status " + httpResponse.code() + ": " + responseBody);
                }

                // 解析响应
                JsonNode responseJson = objectMapper.readTree(responseBody);
                response.setRawResponse(responseBody);

                // 提取内容
                JsonNode choices = responseJson.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null) {
                        response.setContent(message.get("content").asText());
                    }
                }

                // 提取 token 信息
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
            log.error("LLM API call failed", e);
            throw new RuntimeException("LLM API call failed: " + e.getMessage(), e);
        } finally {
            response.setLatencyMs(System.currentTimeMillis() - startTime);
        }

        return response;
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
