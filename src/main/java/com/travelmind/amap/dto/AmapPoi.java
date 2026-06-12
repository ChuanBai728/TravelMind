package com.travelmind.amap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 高德地图 POI 数据
 */
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

    @JsonProperty("business_area")
    private String businessArea;

    // tag 可能是字符串或数组，用 Object 接收
    @JsonProperty("tag")
    private Object tag;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getProvinceName() { return provinceName; }
    public void setProvinceName(String provinceName) { this.provinceName = provinceName; }
    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }
    public String getDistrictName() { return districtName; }
    public void setDistrictName(String districtName) { this.districtName = districtName; }
    public String getBusinessArea() { return businessArea; }
    public void setBusinessArea(String businessArea) { this.businessArea = businessArea; }
    public Object getTag() { return tag; }
    public void setTag(Object tag) { this.tag = tag; }
}
