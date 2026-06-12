package com.travelmind.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 完整行程
 */
public class Itinerary {

    /**
     * 行程 ID
     */
    private Long id;

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 原始需求
     */
    private TripRequest request;

    /**
     * 每日计划列表
     */
    private List<DayPlan> days;

    /**
     * 规划假设
     */
    private List<String> assumptions;

    /**
     * 提醒事项
     */
    private List<String> reminders;

    /**
     * 备选方案
     */
    private List<String> alternatives;

    /**
     * Markdown 内容
     */
    private String markdown;

    public Itinerary() {
        this.days = new ArrayList<>();
        this.assumptions = new ArrayList<>();
        this.reminders = new ArrayList<>();
        this.alternatives = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public TripRequest getRequest() {
        return request;
    }

    public void setRequest(TripRequest request) {
        this.request = request;
    }

    public List<DayPlan> getDays() {
        return days;
    }

    public void setDays(List<DayPlan> days) {
        this.days = days;
    }

    public List<String> getAssumptions() {
        return assumptions;
    }

    public void setAssumptions(List<String> assumptions) {
        this.assumptions = assumptions;
    }

    public List<String> getReminders() {
        return reminders;
    }

    public void setReminders(List<String> reminders) {
        this.reminders = reminders;
    }

    public List<String> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<String> alternatives) {
        this.alternatives = alternatives;
    }

    public String getMarkdown() {
        return markdown;
    }

    public void setMarkdown(String markdown) {
        this.markdown = markdown;
    }

    @Override
    public String toString() {
        return "Itinerary{" +
                "id=" + id +
                ", sessionId=" + sessionId +
                ", days=" + days +
                '}';
    }
}
