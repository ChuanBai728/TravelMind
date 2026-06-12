package com.travelmind.conversation;

import com.travelmind.domain.Itinerary;
import com.travelmind.domain.TripRequest;
import com.travelmind.storage.ItineraryRepository;
import com.travelmind.storage.TripRequestRepository;
import com.travelmind.storage.TravelSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会话管理器
 */
@Component
public class ConversationManager {

    private static final Logger log = LoggerFactory.getLogger(ConversationManager.class);

    private final TravelSessionRepository travelSessionRepository;
    private final ItineraryRepository itineraryRepository;
    private final TripRequestRepository tripRequestRepository;
    private final UserMessageClassifier userMessageClassifier;
    private final ObjectMapper objectMapper;

    public ConversationManager(TravelSessionRepository travelSessionRepository,
                               ItineraryRepository itineraryRepository,
                               TripRequestRepository tripRequestRepository,
                               UserMessageClassifier userMessageClassifier,
                               ObjectMapper objectMapper) {
        this.travelSessionRepository = travelSessionRepository;
        this.itineraryRepository = itineraryRepository;
        this.tripRequestRepository = tripRequestRepository;
        this.userMessageClassifier = userMessageClassifier;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建新会话
     *
     * @return 会话 ID
     */
    public Long createSession() {
        TravelSessionRepository.TravelSession session = new TravelSessionRepository.TravelSession();
        session.setSessionName("会话-" + System.currentTimeMillis());
        session.setStatus("ACTIVE");
        travelSessionRepository.save(session);

        log.info("Created new session: {}", session.getId());
        return session.getId();
    }

    /**
     * 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话
     */
    public TravelSessionRepository.TravelSession getSession(Long sessionId) {
        return travelSessionRepository.findById(sessionId);
    }

    /**
     * 获取会话上下文
     *
     * @param sessionId 会话 ID
     * @return 会话上下文
     */
    public ConversationContext getContext(Long sessionId) {
        ConversationContext context = new ConversationContext();
        context.setSessionId(sessionId);

        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session != null) {
            context.setCurrentItineraryId(session.getCurrentItineraryId());

            if (session.getCurrentItineraryId() != null) {
                ItineraryRepository.Itinerary itineraryEntity = itineraryRepository.findById(session.getCurrentItineraryId());
                if (itineraryEntity != null) {
                    Itinerary itinerary = new Itinerary();
                    itinerary.setId(itineraryEntity.getId());
                    itinerary.setSessionId(itineraryEntity.getSessionId());
                    itinerary.setMarkdown(itineraryEntity.getMarkdownContent());
                    itinerary.setRequest(loadTripRequest(itineraryEntity.getRequestId()));
                    context.setCurrentItinerary(itinerary);
                }
            }
        }

        return context;
    }

    /**
     * 更新会话上下文
     *
     * @param sessionId       会话 ID
     * @param itineraryId     行程 ID
     * @param lastUserMessage 最后一条用户消息
     */
    public void updateContext(Long sessionId, Long itineraryId, String lastUserMessage) {
        if (itineraryId == null) {
            log.debug("Skip context update for session {} because itineraryId is null", sessionId);
            return;
        }

        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session != null) {
            session.setCurrentItineraryId(itineraryId);
            travelSessionRepository.save(session);
        }
    }

    /**
     * 分类用户消息
     *
     * @param message 用户消息
     * @return 消息分类
     */
    public ConversationIntent classifyMessage(String message) {
        return userMessageClassifier.classify(message);
    }

    /**
     * 获取历史行程列表
     *
     * @param sessionId 会话 ID
     * @param limit     返回数量限制
     * @return 行程列表
     */
    public List<ItineraryRepository.Itinerary> getHistoryItineraries(Long sessionId, int limit) {
        return itineraryRepository.findBySessionId(sessionId).stream()
                .limit(limit)
                .toList();
    }

    /**
     * 清除会话上下文
     *
     * @param sessionId 会话 ID
     */
    public void clearContext(Long sessionId) {
        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session != null) {
            session.setCurrentItineraryId(null);
            travelSessionRepository.save(session);
        }
    }

    private TripRequest loadTripRequest(Long requestId) {
        if (requestId == null) {
            return null;
        }

        TripRequestRepository.TripRequest stored = tripRequestRepository.findById(requestId);
        if (stored == null) {
            return null;
        }

        TripRequest request = new TripRequest();
        request.setDestination(stored.getDestination());
        request.setDurationDays(stored.getDurationDays());
        request.setStartDate(stored.getStartDate());
        request.setPeopleCount(stored.getPeopleCount());
        request.setBudgetLevel(stored.getBudgetLevel());
        request.setPaceLevel(stored.getPaceLevel());
        request.setTransportMode(stored.getTransportMode());
        request.setHotelArea(stored.getHotelArea());
        request.setRawInput(stored.getRawInput());

        if (stored.getPreferencesJson() != null) {
            try {
                List<String> preferences = objectMapper.readValue(
                        stored.getPreferencesJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                request.setPreferences(preferences);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse preferences from stored trip request {}", requestId, e);
            }
        }

        return request;
    }
}
