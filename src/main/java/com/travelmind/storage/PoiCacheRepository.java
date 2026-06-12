package com.travelmind.storage;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * POI 缓存内存存储
 */
@Repository
public class PoiCacheRepository {

    private final Map<String, PoiCache> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public PoiCache save(PoiCache poi) {
        if (poi.getId() == null) {
            poi.setId(idGenerator.getAndIncrement());
            poi.setCreatedAt(LocalDateTime.now());
        }
        poi.setUpdatedAt(LocalDateTime.now());
        String key = poi.getSource() + ":" + poi.getSourcePoiId();
        store.put(key, poi);
        return poi;
    }

    public PoiCache findBySourceAndPoiId(String source, String sourcePoiId) {
        String key = source + ":" + sourcePoiId;
        return store.get(key);
    }

    public List<PoiCache> findByCity(String city) {
        return store.values().stream()
                .filter(p -> city.equals(p.getCity()))
                .collect(Collectors.toList());
    }

    /**
     * POI 缓存数据对象
     */
    public static class PoiCache {
        private Long id;
        private String source;
        private String sourcePoiId;
        private String name;
        private String city;
        private String address;
        private String location;
        private String category;
        private String rawJson;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getSourcePoiId() { return sourcePoiId; }
        public void setSourcePoiId(String sourcePoiId) { this.sourcePoiId = sourcePoiId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getRawJson() { return rawJson; }
        public void setRawJson(String rawJson) { this.rawJson = rawJson; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
