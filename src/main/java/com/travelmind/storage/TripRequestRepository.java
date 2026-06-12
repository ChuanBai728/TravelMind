package com.travelmind.storage;

import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 旅行需求内存存储
 */
@Repository
public class TripRequestRepository {

    private final Map<Long, TripRequest> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public TripRequest save(TripRequest request) {
        if (request.getId() == null) {
            request.setId(idGenerator.getAndIncrement());
            request.setCreatedAt(LocalDateTime.now());
        }
        request.setUpdatedAt(LocalDateTime.now());
        store.put(request.getId(), request);
        return request;
    }

    public TripRequest findById(Long id) {
        return store.get(id);
    }

    public List<TripRequest> findBySessionId(Long sessionId) {
        return store.values().stream()
                .filter(r -> sessionId.equals(r.getSessionId()))
                .collect(Collectors.toList());
    }

    /**
     * 旅行需求数据对象
     */
    public static class TripRequest {
        private Long id;
        private Long sessionId;
        private String rawInput;
        private String destination;
        private Integer durationDays;
        private LocalDate startDate;
        private Integer peopleCount;
        private String budgetLevel;
        private String paceLevel;
        private String transportMode;
        private String preferencesJson;
        private String hotelArea;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String getRawInput() { return rawInput; }
        public void setRawInput(String rawInput) { this.rawInput = rawInput; }
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        public Integer getDurationDays() { return durationDays; }
        public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public Integer getPeopleCount() { return peopleCount; }
        public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
        public String getBudgetLevel() { return budgetLevel; }
        public void setBudgetLevel(String budgetLevel) { this.budgetLevel = budgetLevel; }
        public String getPaceLevel() { return paceLevel; }
        public void setPaceLevel(String paceLevel) { this.paceLevel = paceLevel; }
        public String getTransportMode() { return transportMode; }
        public void setTransportMode(String transportMode) { this.transportMode = transportMode; }
        public String getPreferencesJson() { return preferencesJson; }
        public void setPreferencesJson(String preferencesJson) { this.preferencesJson = preferencesJson; }
        public String getHotelArea() { return hotelArea; }
        public void setHotelArea(String hotelArea) { this.hotelArea = hotelArea; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
