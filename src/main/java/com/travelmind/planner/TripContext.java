package com.travelmind.planner;

import com.travelmind.domain.Itinerary;
import com.travelmind.domain.Poi;
import com.travelmind.domain.RouteInfo;
import com.travelmind.domain.TripRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 行程规划上下文
 */
public class TripContext {

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 结构化旅行需求
     */
    private TripRequest tripRequest;

    /**
     * 候选 POI 列表
     */
    private List<Poi> candidatePois;

    /**
     * 路线信息列表
     */
    private List<RouteInfo> routeInfos;

    /**
     * 当前行程（用于修改场景）
     */
    private Itinerary currentItinerary;

    /**
     * 用户原始输入
     */
    private String userInput;

    /**
     * 会话摘要
     */
    private String contextSummary;

    public TripContext() {
        this.candidatePois = new ArrayList<>();
        this.routeInfos = new ArrayList<>();
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public TripRequest getTripRequest() {
        return tripRequest;
    }

    public void setTripRequest(TripRequest tripRequest) {
        this.tripRequest = tripRequest;
    }

    public List<Poi> getCandidatePois() {
        return candidatePois;
    }

    public void setCandidatePois(List<Poi> candidatePois) {
        this.candidatePois = candidatePois;
    }

    public List<RouteInfo> getRouteInfos() {
        return routeInfos;
    }

    public void setRouteInfos(List<RouteInfo> routeInfos) {
        this.routeInfos = routeInfos;
    }

    public Itinerary getCurrentItinerary() {
        return currentItinerary;
    }

    public void setCurrentItinerary(Itinerary currentItinerary) {
        this.currentItinerary = currentItinerary;
    }

    public String getUserInput() {
        return userInput;
    }

    public void setUserInput(String userInput) {
        this.userInput = userInput;
    }

    public String getContextSummary() {
        return contextSummary;
    }

    public void setContextSummary(String contextSummary) {
        this.contextSummary = contextSummary;
    }
}
