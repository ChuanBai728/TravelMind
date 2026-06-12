package com.travelmind.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 具体活动
 */
public class Activity {

    /**
     * 时间段，例如上午、中午、下午、晚上
     */
    private String timeSlot;

    /**
     * 活动标题
     */
    private String title;

    /**
     * 地点名称
     */
    private String locationName;

    /**
     * 建议停留时间（分钟）
     */
    private Integer stayMinutes;

    /**
     * 交通建议
     */
    private String transportSuggestion;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 小贴士列表
     */
    private List<String> tips;

    public Activity() {
        this.tips = new ArrayList<>();
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Integer getStayMinutes() {
        return stayMinutes;
    }

    public void setStayMinutes(Integer stayMinutes) {
        this.stayMinutes = stayMinutes;
    }

    public String getTransportSuggestion() {
        return transportSuggestion;
    }

    public void setTransportSuggestion(String transportSuggestion) {
        this.transportSuggestion = transportSuggestion;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getTips() {
        return tips;
    }

    public void setTips(List<String> tips) {
        this.tips = tips;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "timeSlot='" + timeSlot + '\'' +
                ", title='" + title + '\'' +
                ", locationName='" + locationName + '\'' +
                '}';
    }
}
