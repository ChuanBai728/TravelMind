package com.travelmind.storage;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 行程内存存储
 */
@Repository
public class ItineraryRepository {

    private final Map<Long, Itinerary> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Itinerary save(Itinerary itinerary) {
        if (itinerary.getId() == null) {
            itinerary.setId(idGenerator.getAndIncrement());
            itinerary.setCreatedAt(LocalDateTime.now());
        }
        itinerary.setUpdatedAt(LocalDateTime.now());
        store.put(itinerary.getId(), itinerary);
        return itinerary;
    }

    public Itinerary findById(Long id) {
        return store.get(id);
    }

    public List<Itinerary> findBySessionId(Long sessionId) {
        return store.values().stream()
                .filter(i -> sessionId.equals(i.getSessionId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    /**
     * 行程数据对象
     */
    public static class Itinerary {
        private Long id;
        private Long sessionId;
        private Long requestId;
        private Integer version;
        private String title;
        private String itineraryJson;
        private String markdownContent;
        private String validationStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public Long getRequestId() { return requestId; }
        public void setRequestId(Long requestId) { this.requestId = requestId; }
        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getItineraryJson() { return itineraryJson; }
        public void setItineraryJson(String itineraryJson) { this.itineraryJson = itineraryJson; }
        public String getMarkdownContent() { return markdownContent; }
        public void setMarkdownContent(String markdownContent) { this.markdownContent = markdownContent; }
        public String getValidationStatus() { return validationStatus; }
        public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
