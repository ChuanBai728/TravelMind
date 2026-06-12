package com.travelmind.domain;

/**
 * 两个地点之间的路线信息
 */
public class RouteInfo {

    /**
     * 起点名称
     */
    private String originName;

    /**
     * 终点名称
     */
    private String destinationName;

    /**
     * 距离（米）
     */
    private Integer distanceMeters;

    /**
     * 耗时（分钟）
     */
    private Integer durationMinutes;

    /**
     * 交通方式
     */
    private String transportMode;

    public RouteInfo() {
    }

    public String getOriginName() {
        return originName;
    }

    public void setOriginName(String originName) {
        this.originName = originName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public Integer getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(Integer distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    @Override
    public String toString() {
        return "RouteInfo{" +
                "originName='" + originName + '\'' +
                ", destinationName='" + destinationName + '\'' +
                ", distanceMeters=" + distanceMeters +
                ", durationMinutes=" + durationMinutes +
                ", transportMode='" + transportMode + '\'' +
                '}';
    }
}
