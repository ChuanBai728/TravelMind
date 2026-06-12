package com.travelmind.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 高德地图 POI 数据
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapPoi {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("typecode")
    private String typeCode;

    @JsonProperty("address")
    private String address;

    @JsonProperty("location")
    private String location;

    @JsonProperty("pname")
    private String provinceName;

    @JsonProperty("cityname")
    private String cityName;

    @JsonProperty("adname")
    private String districtName;

    @JsonProperty("importance")
    private String importance;

    @JsonProperty("shopid")
    private String shopId;

    @JsonProperty("shopinfo")
    private String shopInfo;

    @JsonProperty("poiweight")
    private String poiWeight;

    @JsonProperty("gridcode")
    private String gridCode;

    @JsonProperty("distance")
    private String distance;

    @JsonProperty("navi_poiid")
    private String naviPoiId;

    @JsonProperty("entr_location")
    private String entranceLocation;

    @JsonProperty("business_area")
    private String businessArea;

    @JsonProperty("indoor_map")
    private String indoorMap;

    @JsonProperty("alias")
    private String alias;

    @JsonProperty("tag")
    private String tag;

    @JsonProperty("photos")
    private Object photos;
}
