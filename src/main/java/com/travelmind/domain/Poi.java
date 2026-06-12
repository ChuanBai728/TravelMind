package com.travelmind.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 景点、餐厅、商圈、车站等地点信息
 */
public class Poi {

    /**
     * 数据来源，例如 AMAP
     */
    private String source;

    /**
     * 来源系统中的 ID
     */
    private String sourceId;

    /**
     * 地点名称
     */
    private String name;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 地址
     */
    private String address;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 分类，例如景点、餐厅
     */
    private String category;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 建议停留时间（分钟）
     */
    private Integer recommendedStayMinutes;

    public Poi() {
        this.tags = new ArrayList<>();
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getRecommendedStayMinutes() {
        return recommendedStayMinutes;
    }

    public void setRecommendedStayMinutes(Integer recommendedStayMinutes) {
        this.recommendedStayMinutes = recommendedStayMinutes;
    }

    @Override
    public String toString() {
        return "Poi{" +
                "name='" + name + '\'' +
                ", city='" + city + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
