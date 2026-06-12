package com.travelmind.llm;

import java.util.function.Consumer;

/**
 * LLM 客户端接口
 */
public interface LlmClient {

    /**
     * 发送聊天请求（同步）
     *
     * @param request LLM 请求
     * @return LLM 响应
     */
    LlmResponse chat(LlmRequest request);

    /**
     * 发送聊天请求（流式），每个文本片段通过 callback 实时回调
     *
     * @param request  LLM 请求
     * @param callback 文本片段回调
     * @return 流式响应（包含完整内容和 token 信息）
     */
    StreamResponse chatStream(LlmRequest request, Consumer<String> callback);

    /**
     * 获取提供者名称
     *
     * @return 提供者名称
     */
    String getProviderName();

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    String getModelName();
}
