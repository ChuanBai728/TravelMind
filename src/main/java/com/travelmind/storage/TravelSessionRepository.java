package com.travelmind.storage;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 旅行会话内存存储
 */
@Repository
public class TravelSessionRepository {

    private final Map<Long, TravelSession> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public TravelSession save(TravelSession session) {
        if (session.getId() == null) {
            session.setId(idGenerator.getAndIncrement());
            session.setCreatedAt(LocalDateTime.now());
        }
        session.setUpdatedAt(LocalDateTime.now());
        store.put(session.getId(), session);
        return session;
    }

    public TravelSession findById(Long id) {
        return store.get(id);
    }

    /**
     * 旅行会话数据对象
     */
    public static class TravelSession {
        private Long id;
        private String sessionName;
        private String status;
        private Long currentItineraryId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSessionName() { return sessionName; }
        public void setSessionName(String sessionName) { this.sessionName = sessionName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getCurrentItineraryId() { return currentItineraryId; }
        public void setCurrentItineraryId(Long currentItineraryId) { this.currentItineraryId = currentItineraryId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}
