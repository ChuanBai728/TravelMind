package com.travelmind.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 高德地图路线数据
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapRoute {

    @JsonProperty("origin")
    private String origin;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("origin_id")
    private String originId;

    @JsonProperty("destination_id")
    private String destinationId;

    @JsonProperty("paths")
    private List<Path> paths;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        @JsonProperty("distance")
        private String distance;

        @JsonProperty("duration")
        private String duration;

        @JsonProperty("strategy")
        private String strategy;

        @JsonProperty("steps")
        private List<Step> steps;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Step {
        @JsonProperty("instruction")
        private String instruction;

        @JsonProperty("road")
        private String road;

        @JsonProperty("distance")
        private String distance;

        @JsonProperty("duration")
        private String duration;

        @JsonProperty("polyline")
        private String polyline;

        @JsonProperty("action")
        private String action;

        @JsonProperty("assistant_action")
        private String assistantAction;
    }
}
