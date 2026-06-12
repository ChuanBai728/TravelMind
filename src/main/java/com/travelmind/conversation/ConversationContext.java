package com.travelmind.conversation;

import com.travelmind.domain.Itinerary;

/**
 * 会话上下文
 */
public class ConversationContext {

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 当前行程 ID
     */
    private Long currentItineraryId;

    /**
     * 目的地
     */
    private String destination;

    /**
     * 出行天数
     */
    private Integer durationDays;

    /**
     * 最后一条用户消息
     */
    private String lastUserMessage;

    /**
     * 当前行程
     */
    private Itinerary currentItinerary;

    public ConversationContext() {
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getCurrentItineraryId() {
        return currentItineraryId;
    }

    public void setCurrentItineraryId(Long currentItineraryId) {
        this.currentItineraryId = currentItineraryId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public String getLastUserMessage() {
        return lastUserMessage;
    }

    public void setLastUserMessage(String lastUserMessage) {
        this.lastUserMessage = lastUserMessage;
    }

    public Itinerary getCurrentItinerary() {
        return currentItinerary;
    }

    public void setCurrentItinerary(Itinerary currentItinerary) {
        this.currentItinerary = currentItinerary;
    }
}
