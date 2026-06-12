package com.travelmind.llm;

/**
 * LLM 客户端接口
 */
public interface LlmClient {

    /**
     * 发送聊天请求
     *
     * @param request LLM 请求
     * @return LLM 响应
     */
    LlmResponse chat(LlmRequest request);

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
