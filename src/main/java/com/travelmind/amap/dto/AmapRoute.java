package com.travelmind.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 高德地图路线数据
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapRoute {

    @JsonProperty("origin")
    private String origin;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("paths")
    private List<Path> paths;

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public List<Path> getPaths() { return paths; }
    public void setPaths(List<Path> paths) { this.paths = paths; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Path {
        @JsonProperty("distance")
        private String distance;

        @JsonProperty("duration")
        private String duration;

        @JsonProperty("steps")
        private List<Step> steps;

        public String getDistance() { return distance; }
        public void setDistance(String distance) { this.distance = distance; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
        public List<Step> getSteps() { return steps; }
        public void setSteps(List<Step> steps) { this.steps = steps; }
    }

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

        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
        public String getRoad() { return road; }
        public void setRoad(String road) { this.road = road; }
        public String getDistance() { return distance; }
        public void setDistance(String distance) { this.distance = distance; }
        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
    }
}
