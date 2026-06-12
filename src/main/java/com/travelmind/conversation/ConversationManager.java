package com.travelmind.conversation;

import com.travelmind.domain.Itinerary;
import com.travelmind.storage.ItineraryRepository;
import com.travelmind.storage.TravelSessionRepository;
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
    private final UserMessageClassifier userMessageClassifier;

    public ConversationManager(TravelSessionRepository travelSessionRepository,
                               ItineraryRepository itineraryRepository,
                               UserMessageClassifier userMessageClassifier) {
        this.travelSessionRepository = travelSessionRepository;
        this.itineraryRepository = itineraryRepository;
        this.userMessageClassifier = userMessageClassifier;
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
}
