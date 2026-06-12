package com.travelmind.planner;

import com.travelmind.domain.Itinerary;

import java.util.function.Consumer;

/**
 * 行程规划服务接口
 */
public interface TripPlannerService {

    /**
     * 处理用户消息
     *
     * @param sessionId 会话 ID
     * @param userInput 用户输入
     * @return 处理结果（行程或追问）
     */
    Itinerary handleUserMessage(Long sessionId, String userInput);

    /**
     * 处理用户消息（流式），行程生成阶段实时回调文本片段
     *
     * @param sessionId 会话 ID
     * @param userInput 用户输入
     * @param callback  流式文本回调
     * @return 处理结果（行程或追问）
     */
    Itinerary handleUserMessageStream(Long sessionId, String userInput, Consumer<String> callback);

    /**
     * 创建新行程
     *
     * @param sessionId 会话 ID
     * @param userInput 用户输入
     * @return 生成的行程
     */
    Itinerary createPlan(Long sessionId, String userInput);

    /**
     * 创建新行程（流式）
     */
    Itinerary createPlanStream(Long sessionId, String userInput, Consumer<String> callback);

    /**
     * 修改行程
     *
     * @param sessionId 会话 ID
     * @param userInput 用户输入
     * @return 修改后的行程
     */
    Itinerary modifyPlan(Long sessionId, String userInput);

    /**
     * 修改行程（流式）
     */
    Itinerary modifyPlanStream(Long sessionId, String userInput, Consumer<String> callback);
}
