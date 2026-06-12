package com.travelmind.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户结构化后的行程需求
 */
public class TripRequest {

    /**
     * 目的地城市，例如上海
     */
    private String destination;

    /**
     * 出行天数
     */
    private Integer durationDays;

    /**
     * 出发日期，可为空
     */
    private LocalDate startDate;

    /**
     * 出行人数，可为空
     */
    private Integer peopleCount;

    /**
     * 预算等级，例如经济型、舒适型、高预算
     */
    private String budgetLevel;

    /**
     * 节奏，例如轻松、适中、紧凑
     */
    private String paceLevel;

    /**
     * 交通方式，例如公共交通、打车、自驾
     */
    private String transportMode;

    /**
     * 兴趣偏好，例如美食、亲子、历史文化
     */
    private List<String> preferences;

    /**
     * 住宿区域，可为空
     */
    private String hotelArea;

    /**
     * 用户原始输入
     */
    private String rawInput;

    public TripRequest() {
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getBudgetLevel() {
        return budgetLevel;
    }

    public void setBudgetLevel(String budgetLevel) {
        this.budgetLevel = budgetLevel;
    }

    public String getPaceLevel() {
        return paceLevel;
    }

    public void setPaceLevel(String paceLevel) {
        this.paceLevel = paceLevel;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public List<String> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<String> preferences) {
        this.preferences = preferences;
    }

    public String getHotelArea() {
        return hotelArea;
    }

    public void setHotelArea(String hotelArea) {
        this.hotelArea = hotelArea;
    }

    public String getRawInput() {
        return rawInput;
    }

    public void setRawInput(String rawInput) {
        this.rawInput = rawInput;
    }

    @Override
    public String toString() {
        return "TripRequest{" +
                "destination='" + destination + '\'' +
                ", durationDays=" + durationDays +
                ", startDate=" + startDate +
                ", peopleCount=" + peopleCount +
                ", budgetLevel='" + budgetLevel + '\'' +
                ", paceLevel='" + paceLevel + '\'' +
                ", transportMode='" + transportMode + '\'' +
                ", preferences=" + preferences +
                ", hotelArea='" + hotelArea + '\'' +
                '}';
    }
}
