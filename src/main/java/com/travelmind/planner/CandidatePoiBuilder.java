package com.travelmind.planner;

import com.travelmind.amap.AmapService;
import com.travelmind.domain.Poi;
import com.travelmind.domain.TripRequest;
import com.travelmind.entity.PoiCacheEntity;
import com.travelmind.repository.PoiCacheMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 候选 POI 构建器
 */
@Component
public class CandidatePoiBuilder {

    private static final Logger log = LoggerFactory.getLogger(CandidatePoiBuilder.class);

    /**
     * 默认搜索关键词
     */
    private static final List<String> DEFAULT_KEYWORDS = List.of("景点", "美食", "博物馆", "步行街", "夜景");

    /**
     * 每个关键词搜索数量限制
     */
    private static final int SEARCH_LIMIT = 10;

    private final AmapService amapService;
    private final PoiCacheMapper poiCacheMapper;
    private final ObjectMapper objectMapper;

    public CandidatePoiBuilder(AmapService amapService, PoiCacheMapper poiCacheMapper,
                                ObjectMapper objectMapper) {
        this.amapService = amapService;
        this.poiCacheMapper = poiCacheMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 构建候选 POI 列表
     *
     * @param tripRequest 旅行需求
     * @return 候选 POI 列表
     */
    public List<Poi> build(TripRequest tripRequest) {
        if (tripRequest == null || tripRequest.getDestination() == null) {
            return new ArrayList<>();
        }

        String city = tripRequest.getDestination();
        List<Poi> allPois = new ArrayList<>();

        // 1. 先从缓存查询
        List<Poi> cachedPois = searchFromCache(city);
        allPois.addAll(cachedPois);

        // 2. 根据偏好确定搜索关键词
        List<String> keywords = buildKeywords(tripRequest);

        // 3. 调用高德地图 API 搜索
        for (String keyword : keywords) {
            try {
                List<Poi> pois = amapService.searchPois(city, keyword, SEARCH_LIMIT);
                allPois.addAll(pois);

                // 保存到缓存
                saveToCache(pois);
            } catch (Exception e) {
                log.warn("Failed to search POIs for keyword: {}", keyword, e);
            }
        }

        // 4. 去重
        return deduplicate(allPois);
    }

    private List<String> buildKeywords(TripRequest tripRequest) {
        List<String> keywords = new ArrayList<>(DEFAULT_KEYWORDS);

        if (tripRequest.getPreferences() != null) {
            for (String pref : tripRequest.getPreferences()) {
                if (!keywords.contains(pref)) {
                    keywords.add(pref);
                }
            }
        }

        return keywords;
    }

    private List<Poi> searchFromCache(String city) {
        try {
            LambdaQueryWrapper<PoiCacheEntity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PoiCacheEntity::getCity, city);
            List<PoiCacheEntity> entities = poiCacheMapper.selectList(wrapper);

            return entities.stream()
                    .map(this::convertFromCache)
                    .filter(poi -> poi != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to search POI from cache", e);
            return new ArrayList<>();
        }
    }

    private Poi convertFromCache(PoiCacheEntity entity) {
        try {
            if (entity == null) {
                return null;
            }

            Poi poi = new Poi();
            poi.setSource(entity.getSource());
            poi.setSourceId(entity.getSourcePoiId());
            poi.setName(entity.getName());
            poi.setCity(entity.getCity());
            poi.setAddress(entity.getAddress());
            poi.setCategory(entity.getCategory());

            // 解析经纬度
            String location = entity.getLocation();
            if (location != null && location.contains(",")) {
                String[] parts = location.split(",");
                if (parts.length == 2) {
                    poi.setLongitude(new java.math.BigDecimal(parts[0]));
                    poi.setLatitude(new java.math.BigDecimal(parts[1]));
                }
            }

            return poi;
        } catch (Exception e) {
            log.warn("Failed to convert POI from cache", e);
            return null;
        }
    }

    private void saveToCache(List<Poi> pois) {
        for (Poi poi : pois) {
            try {
                // 检查是否已存在
                LambdaQueryWrapper<PoiCacheEntity> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(PoiCacheEntity::getSource, poi.getSource())
                        .eq(PoiCacheEntity::getSourcePoiId, poi.getSourceId());
                Long count = poiCacheMapper.selectCount(wrapper);

                if (count > 0) {
                    continue;
                }

                PoiCacheEntity entity = new PoiCacheEntity();
                entity.setSource(poi.getSource());
                entity.setSourcePoiId(poi.getSourceId());
                entity.setName(poi.getName());
                entity.setCity(poi.getCity());
                entity.setAddress(poi.getAddress());
                entity.setCategory(poi.getCategory());

                if (poi.getLongitude() != null && poi.getLatitude() != null) {
                    entity.setLocation(poi.getLongitude() + "," + poi.getLatitude());
                }

                entity.setRawJson(objectMapper.writeValueAsString(poi));
                poiCacheMapper.insert(entity);
            } catch (Exception e) {
                log.warn("Failed to save POI to cache: {}", poi.getName(), e);
            }
        }
    }

    private List<Poi> deduplicate(List<Poi> pois) {
        return pois.stream()
                .filter(poi -> poi.getSourceId() != null)
                .collect(Collectors.toMap(
                        Poi::getSourceId,
                        poi -> poi,
                        (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
    }
}
