package com.travelmind.amap;

import com.travelmind.domain.Poi;
import com.travelmind.domain.RouteInfo;

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

    /**
     * 估算路线
     *
     * @param origin        起点 POI
     * @param destination   终点 POI
     * @param transportMode 交通方式
     * @return 路线信息
     */
    RouteInfo estimateRoute(Poi origin, Poi destination, String transportMode);
}
