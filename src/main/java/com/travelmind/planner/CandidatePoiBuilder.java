package com.travelmind.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmind.amap.AmapService;
import com.travelmind.domain.Poi;
import com.travelmind.domain.TripRequest;
import com.travelmind.storage.PoiCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class CandidatePoiBuilder {

    private static final Logger log = LoggerFactory.getLogger(CandidatePoiBuilder.class);

    private static final List<String> DEFAULT_KEYWORDS = List.of("景点", "美食");
    private static final int SEARCH_LIMIT = 5;
    private static final int CACHE_HIT_MIN_SIZE = SEARCH_LIMIT;

    private final AmapService amapService;
    private final PoiCacheRepository poiCacheRepository;
    private final ObjectMapper objectMapper;

    public CandidatePoiBuilder(AmapService amapService, PoiCacheRepository poiCacheRepository,
                               ObjectMapper objectMapper) {
        this.amapService = amapService;
        this.poiCacheRepository = poiCacheRepository;
        this.objectMapper = objectMapper;
    }

    public List<Poi> build(TripRequest tripRequest) {
        if (tripRequest == null || tripRequest.getDestination() == null) {
            return new ArrayList<>();
        }

        String city = tripRequest.getDestination();
        List<Poi> cachedPois = searchFromCache(city);
        if (cachedPois.size() >= CACHE_HIT_MIN_SIZE) {
            log.info("Using cached POIs for city={}, count={}", city, cachedPois.size());
            return deduplicate(cachedPois);
        }

        List<Poi> allPois = new ArrayList<>(cachedPois);
        List<String> keywords = buildKeywords(tripRequest);
        List<CompletableFuture<List<Poi>>> futures = keywords.stream()
                .map(keyword -> CompletableFuture.supplyAsync(() -> searchKeyword(city, keyword)))
                .collect(Collectors.toList());

        for (CompletableFuture<List<Poi>> future : futures) {
            allPois.addAll(future.join());
        }

        return deduplicate(allPois);
    }

    private List<Poi> searchKeyword(String city, String keyword) {
        try {
            List<Poi> pois = amapService.searchPois(city, keyword, SEARCH_LIMIT);
            saveToCache(pois);
            return pois;
        } catch (Exception e) {
            log.warn("Failed to search POIs for keyword: {}", keyword, e);
            return new ArrayList<>();
        }
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

            String location = cache.getLocation();
            if (location != null && location.contains(",")) {
                String[] parts = location.split(",");
                if (parts.length == 2) {
                    poi.setLongitude(new BigDecimal(parts[0]));
                    poi.setLatitude(new BigDecimal(parts[1]));
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
