package com.travelmind.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 一天的安排
 */
public class DayPlan {

    /**
     * 天数索引，从 1 开始
     */
    private Integer dayIndex;

    /**
     * 当天主题
     */
    private String theme;

    /**
     * 活动列表
     */
    private List<Activity> activities;

    /**
     * 总参观时间（分钟）
     */
    private Integer totalVisitMinutes;

    /**
     * 总交通时间（分钟）
     */
    private Integer totalTransportMinutes;

    public DayPlan() {
        this.activities = new ArrayList<>();
    }

    public Integer getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(Integer dayIndex) {
        this.dayIndex = dayIndex;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }

    public Integer getTotalVisitMinutes() {
        return totalVisitMinutes;
    }

    public void setTotalVisitMinutes(Integer totalVisitMinutes) {
        this.totalVisitMinutes = totalVisitMinutes;
    }

    public Integer getTotalTransportMinutes() {
        return totalTransportMinutes;
    }

    public void setTotalTransportMinutes(Integer totalTransportMinutes) {
        this.totalTransportMinutes = totalTransportMinutes;
    }

    @Override
    public String toString() {
        return "DayPlan{" +
                "dayIndex=" + dayIndex +
                ", theme='" + theme + '\'' +
                ", activities=" + activities +
                '}';
    }
}
