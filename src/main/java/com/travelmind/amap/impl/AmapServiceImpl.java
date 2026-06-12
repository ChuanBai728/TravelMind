package com.travelmind.amap.impl;

import com.travelmind.amap.AmapClient;
import com.travelmind.amap.AmapService;
import com.travelmind.amap.dto.AmapPoi;
import com.travelmind.amap.dto.AmapRoute;
import com.travelmind.domain.Poi;
import com.travelmind.domain.RouteInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 高德地图服务实现
 */
@Service
public class AmapServiceImpl implements AmapService {

    private static final Logger log = LoggerFactory.getLogger(AmapServiceImpl.class);

    private final AmapClient amapClient;

    public AmapServiceImpl(AmapClient amapClient) {
        this.amapClient = amapClient;
    }

    @Override
    public List<Poi> searchPois(String city, String keyword, int limit) {
        List<Poi> result = new ArrayList<>();
        List<AmapPoi> amapPois = amapClient.searchPois(city, keyword, limit, 1);

        for (AmapPoi amapPoi : amapPois) {
            Poi poi = convertToPoi(amapPoi);
            if (poi != null) {
                result.add(poi);
            }
        }

        return result;
    }

    @Override
    public Poi geocode(String address) {
        String location = amapClient.geocode(address, null);
        if (location == null) {
            return null;
        }

        Poi poi = new Poi();
        poi.setSource("AMAP");
        poi.setName(address);
        poi.setAddress(address);

        String[] parts = location.split(",");
        if (parts.length == 2) {
            try {
                poi.setLongitude(new BigDecimal(parts[0]));
                poi.setLatitude(new BigDecimal(parts[1]));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse location: {}", location);
            }
        }

        return poi;
    }

    @Override
    public RouteInfo estimateRoute(Poi origin, Poi destination, String transportMode) {
        if (origin.getLongitude() == null || origin.getLatitude() == null ||
                destination.getLongitude() == null || destination.getLatitude() == null) {
            log.warn("Cannot estimate route: missing coordinates");
            return null;
        }

        String originLocation = origin.getLongitude() + "," + origin.getLatitude();
        String destinationLocation = destination.getLongitude() + "," + destination.getLatitude();

        // 将交通方式映射到高德 API 的 mode
        String amapMode;
        switch (transportMode) {
            case "步行":
                amapMode = "walking";
                break;
            case "自驾":
                amapMode = "driving";
                break;
            case "公共交通":
            case "公共交通+步行":
            default:
                amapMode = "driving"; // MVP 阶段先用驾车估算
                break;
        }

        AmapRoute route = amapClient.getRoute(originLocation, destinationLocation, amapMode);
        if (route == null || route.getPaths() == null || route.getPaths().isEmpty()) {
            return null;
        }

        AmapRoute.Path path = route.getPaths().get(0);
        RouteInfo routeInfo = new RouteInfo();
        routeInfo.setOriginName(origin.getName());
        routeInfo.setDestinationName(destination.getName());
        routeInfo.setTransportMode(transportMode);

        try {
            routeInfo.setDistanceMeters(Integer.parseInt(path.getDistance()));
            // 高德返回的 duration 是秒，转换为分钟
            int durationSeconds = Integer.parseInt(path.getDuration());
            routeInfo.setDurationMinutes(durationSeconds / 60);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse route distance/duration");
        }

        return routeInfo;
    }

    private Poi convertToPoi(AmapPoi amapPoi) {
        if (amapPoi == null || amapPoi.getName() == null) {
            return null;
        }

        Poi poi = new Poi();
        poi.setSource("AMAP");
        poi.setSourceId(amapPoi.getId());
        poi.setName(amapPoi.getName());
        poi.setCity(amapPoi.getCityName());
        poi.setAddress(amapPoi.getAddress());
        poi.setCategory(amapPoi.getType());

        // 解析经纬度
        String location = amapPoi.getLocation();
        if (location != null && location.contains(",")) {
            String[] parts = location.split(",");
            if (parts.length == 2) {
                try {
                    poi.setLongitude(new BigDecimal(parts[0]));
                    poi.setLatitude(new BigDecimal(parts[1]));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse POI location: {}", location);
                }
            }
        }

        return poi;
    }
}
