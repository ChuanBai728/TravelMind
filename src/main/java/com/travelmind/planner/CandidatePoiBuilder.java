package com.travelmind.planner;

import com.travelmind.amap.AmapService;
import com.travelmind.domain.Poi;
import com.travelmind.domain.TripRequest;
import com.travelmind.storage.PoiCacheRepository;
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
     * 默认搜索关键词（精简）
     */
    private static final List<String> DEFAULT_KEYWORDS = List.of("景点", "美食");

    /**
     * 每个关键词搜索数量限制
     */
    private static final int SEARCH_LIMIT = 5;

    private final AmapService amapService;
    private final PoiCacheRepository poiCacheRepository;
    private final ObjectMapper objectMapper;

    public CandidatePoiBuilder(AmapService amapService, PoiCacheRepository poiCacheRepository,
                                ObjectMapper objectMapper) {
        this.amapService = amapService;
        this.poiCacheRepository = poiCacheRepository;
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

        // 3. 调用高德地图 API 搜索（加延时避免 QPS 超限）
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            try {
                List<Poi> pois = amapService.searchPois(city, keywords.get(i), SEARCH_LIMIT);
                allPois.addAll(pois);

                // 保存到缓存
                saveToCache(pois);
            } catch (Exception e) {
                log.warn("Failed to search POIs for keyword: {}", keywords.get(i), e);
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
            List<PoiCacheRepository.PoiCache> cachedPois = poiCacheRepository.findByCity(city);
            return cachedPois.stream()
                    .map(this::convertFromCache)
                    .filter(poi -> poi != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to search POI from cache", e);
            return new ArrayList<>();
        }
    }

    private Poi convertFromCache(PoiCacheRepository.PoiCache cache) {
        try {
            if (cache == null) {
                return null;
            }

            Poi poi = new Poi();
            poi.setSource(cache.getSource());
            poi.setSourceId(cache.getSourcePoiId());
            poi.setName(cache.getName());
            poi.setCity(cache.getCity());
            poi.setAddress(cache.getAddress());
            poi.setCategory(cache.getCategory());

            // 解析经纬度
            String location = cache.getLocation();
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
                PoiCacheRepository.PoiCache existing = poiCacheRepository.findBySourceAndPoiId(
                        poi.getSource(), poi.getSourceId());
                if (existing != null) {
                    continue;
                }

                PoiCacheRepository.PoiCache cache = new PoiCacheRepository.PoiCache();
                cache.setSource(poi.getSource());
                cache.setSourcePoiId(poi.getSourceId());
                cache.setName(poi.getName());
                cache.setCity(poi.getCity());
                cache.setAddress(poi.getAddress());
                cache.setCategory(poi.getCategory());

                if (poi.getLongitude() != null && poi.getLatitude() != null) {
                    cache.setLocation(poi.getLongitude() + "," + poi.getLatitude());
                }

                cache.setRawJson(objectMapper.writeValueAsString(poi));
                poiCacheRepository.save(cache);
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
