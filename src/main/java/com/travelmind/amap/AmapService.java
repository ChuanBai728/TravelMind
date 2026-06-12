package com.travelmind.amap;

import com.travelmind.domain.Poi;

import java.util.List;

/**
 * 高德地图服务接口
 */
public interface AmapService {

    /**
     * 搜索 POI
     *
     * @param city    城市名称
     * @param keyword 关键词
     * @param limit   返回数量限制
     * @return POI 列表
     */
    List<Poi> searchPois(String city, String keyword, int limit);

    /**
     * 地理编码
     *
     * @param address 地址
     * @return POI 信息（包含经纬度）
     */
    Poi geocode(String address);
}
