package com.travelmind.conversation;

import com.travelmind.domain.Itinerary;
import com.travelmind.entity.ItineraryEntity;
import com.travelmind.entity.TravelSessionEntity;
import com.travelmind.repository.ItineraryMapper;
import com.travelmind.repository.TravelSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话管理器
 */
@Component
public class ConversationManager {

    private static final Logger log = LoggerFactory.getLogger(ConversationManager.class);

    private final TravelSessionMapper travelSessionMapper;
    private final ItineraryMapper itineraryMapper;
    private final UserMessageClassifier userMessageClassifier;

    public ConversationManager(TravelSessionMapper travelSessionMapper, ItineraryMapper itineraryMapper,
                               UserMessageClassifier userMessageClassifier) {
        this.travelSessionMapper = travelSessionMapper;
        this.itineraryMapper = itineraryMapper;
        this.userMessageClassifier = userMessageClassifier;
    }

    /**
     * 创建新会话
     *
     * @return 会话 ID
     */
    public Long createSession() {
        TravelSessionEntity session = new TravelSessionEntity();
        session.setSessionName("会话-" + System.currentTimeMillis());
        session.setStatus("ACTIVE");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        travelSessionMapper.insert(session);

        log.info("Created new session: {}", session.getId());
        return session.getId();
    }

    /**
     * 获取会话
     *
     * @param sessionId 会话 ID
     * @return 会话实体
     */
    public TravelSessionEntity getSession(Long sessionId) {
        return travelSessionMapper.selectById(sessionId);
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

        TravelSessionEntity session = travelSessionMapper.selectById(sessionId);
        if (session != null) {
            context.setCurrentItineraryId(session.getCurrentItineraryId());

            if (session.getCurrentItineraryId() != null) {
                ItineraryEntity itineraryEntity = itineraryMapper.selectById(session.getCurrentItineraryId());
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
     * @param sessionId     会话 ID
     * @param itineraryId   行程 ID
     * @param lastUserMessage 最后一条用户消息
     */
    public void updateContext(Long sessionId, Long itineraryId, String lastUserMessage) {
        TravelSessionEntity session = travelSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setCurrentItineraryId(itineraryId);
            session.setUpdatedAt(LocalDateTime.now());
            travelSessionMapper.updateById(session);
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
    public List<ItineraryEntity> getHistoryItineraries(Long sessionId, int limit) {
        return itineraryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ItineraryEntity>()
                        .eq(ItineraryEntity::getSessionId, sessionId)
                        .orderByDesc(ItineraryEntity::getCreatedAt)
                        .last("LIMIT " + limit)
        );
    }

    /**
     * 清除会话上下文
     *
     * @param sessionId 会话 ID
     */
    public void clearContext(Long sessionId) {
        TravelSessionEntity session = travelSessionMapper.selectById(sessionId);
        if (session != null) {
            session.setCurrentItineraryId(null);
            session.setUpdatedAt(LocalDateTime.now());
            travelSessionMapper.updateById(session);
        }
    }
}
