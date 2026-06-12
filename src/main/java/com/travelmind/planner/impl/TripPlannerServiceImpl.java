package com.travelmind.planner.impl;

import com.travelmind.conversation.ConversationManager;
import com.travelmind.domain.Itinerary;
import com.travelmind.domain.Poi;
import com.travelmind.domain.RouteInfo;
import com.travelmind.domain.TripRequest;
import com.travelmind.entity.ItineraryEntity;
import com.travelmind.entity.TripRequestEntity;
import com.travelmind.entity.TravelSessionEntity;
import com.travelmind.planner.*;
import com.travelmind.repository.ItineraryMapper;
import com.travelmind.repository.TripRequestMapper;
import com.travelmind.repository.TravelSessionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 行程规划服务实现
 */
@Service
public class TripPlannerServiceImpl implements TripPlannerService {

    private static final Logger log = LoggerFactory.getLogger(TripPlannerServiceImpl.class);

    private final IntentParser intentParser;
    private final TripContextBuilder tripContextBuilder;
    private final CandidatePoiBuilder candidatePoiBuilder;
    private final ItineraryGenerator itineraryGenerator;
    private final RuleValidator ruleValidator;
    private final ConversationManager conversationManager;
    private final TravelSessionMapper travelSessionMapper;
    private final TripRequestMapper tripRequestMapper;
    private final ItineraryMapper itineraryMapper;
    private final ObjectMapper objectMapper;

    public TripPlannerServiceImpl(IntentParser intentParser, TripContextBuilder tripContextBuilder,
                                   CandidatePoiBuilder candidatePoiBuilder, ItineraryGenerator itineraryGenerator,
                                   RuleValidator ruleValidator, ConversationManager conversationManager,
                                   TravelSessionMapper travelSessionMapper, TripRequestMapper tripRequestMapper,
                                   ItineraryMapper itineraryMapper, ObjectMapper objectMapper) {
        this.intentParser = intentParser;
        this.tripContextBuilder = tripContextBuilder;
        this.candidatePoiBuilder = candidatePoiBuilder;
        this.itineraryGenerator = itineraryGenerator;
        this.ruleValidator = ruleValidator;
        this.conversationManager = conversationManager;
        this.travelSessionMapper = travelSessionMapper;
        this.tripRequestMapper = tripRequestMapper;
        this.itineraryMapper = itineraryMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Itinerary handleUserMessage(Long sessionId, String userInput) {
        // 1. 获取或创建会话上下文
        TripContext context = tripContextBuilder.build(sessionId, userInput, null);

        // 获取当前行程
        TravelSessionEntity session = travelSessionMapper.selectById(sessionId);
        if (session != null && session.getCurrentItineraryId() != null) {
            ItineraryEntity itineraryEntity = itineraryMapper.selectById(session.getCurrentItineraryId());
            if (itineraryEntity != null) {
                Itinerary currentItinerary = convertToItinerary(itineraryEntity);
                context.setCurrentItinerary(currentItinerary);
            }
        }

        // 2. 解析意图
        IntentParser.ParsedIntent parsedIntent = intentParser.parse(userInput, context);

        // 3. 如果需要追问，返回追问结果
        if (parsedIntent.isNeedClarification()) {
            Itinerary clarification = new Itinerary();
            clarification.setSessionId(sessionId);

            StringBuilder sb = new StringBuilder();
            if (parsedIntent.getQuestions() != null) {
                for (String question : parsedIntent.getQuestions()) {
                    sb.append(question).append("\n");
                }
            }
            clarification.setMarkdown(sb.toString());
            return clarification;
        }

        // 4. 根据意图执行相应操作
        String intent = parsedIntent.getIntent();
        if ("NEW_PLAN".equals(intent)) {
            return createPlan(sessionId, userInput);
        } else if ("MODIFY_PLAN".equals(intent)) {
            return modifyPlan(sessionId, userInput);
        } else {
            Itinerary unknown = new Itinerary();
            unknown.setSessionId(sessionId);
            unknown.setMarkdown("抱歉，我没有理解你的意思。请尝试描述你的旅行需求，例如："帮我规划去上海的三日旅游的行程"");
            return unknown;
        }
    }

    @Override
    @Transactional
    public Itinerary createPlan(Long sessionId, String userInput) {
        // 1. 解析意图
        TripContext tempContext = tripContextBuilder.build(sessionId, userInput, null);
        IntentParser.ParsedIntent parsedIntent = intentParser.parse(userInput, tempContext);

        if (parsedIntent.getTripRequest() == null ||
                parsedIntent.getTripRequest().getDestination() == null ||
                parsedIntent.getTripRequest().getDurationDays() == null) {
            Itinerary result = new Itinerary();
            result.setSessionId(sessionId);
            result.setMarkdown("请提供目的地和出行天数，例如："帮我规划去上海的三日旅游的行程"");
            return result;
        }

        // 2. 补全默认值
        TripRequest tripRequest = tripContextBuilder.fillDefaults(parsedIntent.getTripRequest());
        tripRequest.setRawInput(userInput);

        // 3. 构建上下文
        TripContext context = tripContextBuilder.build(sessionId, userInput, tripRequest);

        // 4. 查询候选 POI
        List<Poi> candidatePois = candidatePoiBuilder.build(tripRequest);
        context.setCandidatePois(candidatePois);

        // 5. 估算路线信息
        List<RouteInfo> routeInfos = estimateRoutes(candidatePois);
        context.setRouteInfos(routeInfos);

        // 6. 生成行程
        Itinerary itinerary = itineraryGenerator.generate(context);

        // 7. 规则校验
        RuleValidator.ValidationResult validationResult = ruleValidator.validate(itinerary);
        if (!validationResult.getWarnings().isEmpty()) {
            itinerary.setReminders(validationResult.getWarnings());
        }

        // 8. 保存数据
        saveTripData(sessionId, tripRequest, itinerary);

        return itinerary;
    }

    @Override
    @Transactional
    public Itinerary modifyPlan(Long sessionId, String userInput) {
        // 1. 获取当前行程
        TravelSessionEntity session = travelSessionMapper.selectById(sessionId);
        if (session == null || session.getCurrentItineraryId() == null) {
            Itinerary result = new Itinerary();
            result.setSessionId(sessionId);
            result.setMarkdown("当前没有行程，请先创建一个新行程。");
            return result;
        }

        ItineraryEntity currentItineraryEntity = itineraryMapper.selectById(session.getCurrentItineraryId());
        if (currentItineraryEntity == null) {
            Itinerary result = new Itinerary();
            result.setSessionId(sessionId);
            result.setMarkdown("当前行程不存在，请先创建一个新行程。");
            return result;
        }

        // 2. 解析意图
        TripContext tempContext = tripContextBuilder.build(sessionId, userInput, null);
        IntentParser.ParsedIntent parsedIntent = intentParser.parse(userInput, tempContext);

        // 3. 构建上下文
        TripRequest tripRequest = currentItineraryEntity.getRequestId() != null ?
                convertToTripRequest(tripRequestMapper.selectById(currentItineraryEntity.getRequestId())) :
                new TripRequest();

        TripContext context = tripContextBuilder.build(sessionId, userInput, tripRequest);

        // 4. 查询相关 POI
        List<Poi> candidatePois = candidatePoiBuilder.build(tripRequest);
        context.setCandidatePois(candidatePois);

        // 5. 修改行程
        Itinerary itinerary = itineraryGenerator.modify(context, currentItineraryEntity.getMarkdownContent(), userInput);

        // 6. 规则校验
        RuleValidator.ValidationResult validationResult = ruleValidator.validate(itinerary);
        if (!validationResult.getWarnings().isEmpty()) {
            itinerary.setReminders(validationResult.getWarnings());
        }

        // 7. 保存新版本
        int newVersion = currentItineraryEntity.getVersion() + 1;
        saveModifiedTripData(sessionId, tripRequest, itinerary, newVersion);

        return itinerary;
    }

    private List<RouteInfo> estimateRoutes(List<Poi> pois) {
        List<RouteInfo> routeInfos = new ArrayList<>();

        // 简单实现：只估算相邻 POI 之间的路线
        for (int i = 0; i < pois.size() - 1; i++) {
            Poi origin = pois.get(i);
            Poi destination = pois.get(i + 1);

            if (origin.getLongitude() != null && destination.getLongitude() != null) {
                try {
                    RouteInfo routeInfo = new RouteInfo();
                    routeInfo.setOriginName(origin.getName());
                    routeInfo.setDestinationName(destination.getName());
                    routeInfo.setTransportMode("公共交通+步行");

                    // 简单估算：假设平均速度 30km/h
                    double distance = calculateDistance(
                            origin.getLatitude().doubleValue(), origin.getLongitude().doubleValue(),
                            destination.getLatitude().doubleValue(), destination.getLongitude().doubleValue());

                    routeInfo.setDistanceMeters((int) (distance * 1000));
                    routeInfo.setDurationMinutes((int) (distance / 30 * 60)); // 假设 30km/h

                    routeInfos.add(routeInfo);
                } catch (Exception e) {
                    log.warn("Failed to estimate route between {} and {}", origin.getName(), destination.getName());
                }
            }
        }

        return routeInfos;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine 公式计算两点间距离（单位：公里）
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void saveTripData(Long sessionId, TripRequest tripRequest, Itinerary itinerary) {
        try {
            // 保存 TripRequest
            TripRequestEntity requestEntity = new TripRequestEntity();
            requestEntity.setSessionId(sessionId);
            requestEntity.setRawInput(tripRequest.getRawInput());
            requestEntity.setDestination(tripRequest.getDestination());
            requestEntity.setDurationDays(tripRequest.getDurationDays());
            requestEntity.setStartDate(tripRequest.getStartDate());
            requestEntity.setPeopleCount(tripRequest.getPeopleCount());
            requestEntity.setBudgetLevel(tripRequest.getBudgetLevel());
            requestEntity.setPaceLevel(tripRequest.getPaceLevel());
            requestEntity.setTransportMode(tripRequest.getTransportMode());
            requestEntity.setPreferencesJson(objectMapper.writeValueAsString(tripRequest.getPreferences()));
            requestEntity.setHotelArea(tripRequest.getHotelArea());
            tripRequestMapper.insert(requestEntity);

            // 保存 Itinerary
            ItineraryEntity itineraryEntity = new ItineraryEntity();
            itineraryEntity.setSessionId(sessionId);
            itineraryEntity.setRequestId(requestEntity.getId());
            itineraryEntity.setVersion(1);
            itineraryEntity.setTitle(tripRequest.getDestination() + " " + tripRequest.getDurationDays() + "日游");
            itineraryEntity.setItineraryJson(objectMapper.writeValueAsString(itinerary));
            itineraryEntity.setMarkdownContent(itinerary.getMarkdown());
            itineraryEntity.setValidationStatus("PASSED");
            itineraryMapper.insert(itineraryEntity);

            // 更新 TravelSession
            TravelSessionEntity session = travelSessionMapper.selectById(sessionId);
            if (session == null) {
                session = new TravelSessionEntity();
                session.setId(sessionId);
                session.setSessionName("会话-" + sessionId);
                session.setStatus("ACTIVE");
                session.setCurrentItineraryId(itineraryEntity.getId());
                travelSessionMapper.insert(session);
            } else {
                session.setCurrentItineraryId(itineraryEntity.getId());
                travelSessionMapper.updateById(session);
            }

            itinerary.setId(itineraryEntity.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trip data", e);
            throw new RuntimeException("保存行程失败", e);
        }
    }

    private void saveModifiedTripData(Long sessionId, TripRequest tripRequest, Itinerary itinerary, int version) {
        try {
            // 保存 TripRequest
            TripRequestEntity requestEntity = new TripRequestEntity();
            requestEntity.setSessionId(sessionId);
            requestEntity.setRawInput(tripRequest.getRawInput());
            requestEntity.setDestination(tripRequest.getDestination());
            requestEntity.setDurationDays(tripRequest.getDurationDays());
            requestEntity.setStartDate(tripRequest.getStartDate());
            requestEntity.setPeopleCount(tripRequest.getPeopleCount());
            requestEntity.setBudgetLevel(tripRequest.getBudgetLevel());
            requestEntity.setPaceLevel(tripRequest.getPaceLevel());
            requestEntity.setTransportMode(tripRequest.getTransportMode());
            requestEntity.setPreferencesJson(objectMapper.writeValueAsString(tripRequest.getPreferences()));
            requestEntity.setHotelArea(tripRequest.getHotelArea());
            tripRequestMapper.insert(requestEntity);

            // 保存 Itinerary
            ItineraryEntity itineraryEntity = new ItineraryEntity();
            itineraryEntity.setSessionId(sessionId);
            itineraryEntity.setRequestId(requestEntity.getId());
            itineraryEntity.setVersion(version);
            itineraryEntity.setTitle(tripRequest.getDestination() + " " + tripRequest.getDurationDays() + "日游 (v" + version + ")");
            itineraryEntity.setItineraryJson(objectMapper.writeValueAsString(itinerary));
            itineraryEntity.setMarkdownContent(itinerary.getMarkdown());
            itineraryEntity.setValidationStatus("PASSED");
            itineraryMapper.insert(itineraryEntity);

            // 更新 TravelSession
            TravelSessionEntity session = travelSessionMapper.selectById(sessionId);
            if (session != null) {
                session.setCurrentItineraryId(itineraryEntity.getId());
                travelSessionMapper.updateById(session);
            }

            itinerary.setId(itineraryEntity.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trip data", e);
            throw new RuntimeException("保存行程失败", e);
        }
    }

    private Itinerary convertToItinerary(ItineraryEntity entity) {
        if (entity == null) {
            return null;
        }

        Itinerary itinerary = new Itinerary();
        itinerary.setId(entity.getId());
        itinerary.setSessionId(entity.getSessionId());
        itinerary.setMarkdown(entity.getMarkdownContent());

        try {
            if (entity.getItineraryJson() != null) {
                Itinerary parsed = objectMapper.readValue(entity.getItineraryJson(), Itinerary.class);
                itinerary.setDays(parsed.getDays());
                itinerary.setAssumptions(parsed.getAssumptions());
                itinerary.setReminders(parsed.getReminders());
                itinerary.setAlternatives(parsed.getAlternatives());
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse itinerary JSON", e);
        }

        return itinerary;
    }

    private TripRequest convertToTripRequest(TripRequestEntity entity) {
        if (entity == null) {
            return null;
        }

        TripRequest request = new TripRequest();
        request.setDestination(entity.getDestination());
        request.setDurationDays(entity.getDurationDays());
        request.setStartDate(entity.getStartDate());
        request.setPeopleCount(entity.getPeopleCount());
        request.setBudgetLevel(entity.getBudgetLevel());
        request.setPaceLevel(entity.getPaceLevel());
        request.setTransportMode(entity.getTransportMode());
        request.setHotelArea(entity.getHotelArea());
        request.setRawInput(entity.getRawInput());

        try {
            if (entity.getPreferencesJson() != null) {
                List<String> preferences = objectMapper.readValue(entity.getPreferencesJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                request.setPreferences(preferences);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse preferences JSON", e);
        }

        return request;
    }
}
