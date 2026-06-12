package com.travelmind.planner.impl;

import com.travelmind.domain.Itinerary;
import com.travelmind.domain.Poi;
import com.travelmind.domain.RouteInfo;
import com.travelmind.domain.TripRequest;
import com.travelmind.planner.*;
import com.travelmind.storage.ItineraryRepository;
import com.travelmind.storage.TravelSessionRepository;
import com.travelmind.storage.TripRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
    private final TravelSessionRepository travelSessionRepository;
    private final TripRequestRepository tripRequestRepository;
    private final ItineraryRepository itineraryRepository;
    private final ObjectMapper objectMapper;

    public TripPlannerServiceImpl(IntentParser intentParser, TripContextBuilder tripContextBuilder,
                                   CandidatePoiBuilder candidatePoiBuilder, ItineraryGenerator itineraryGenerator,
                                   RuleValidator ruleValidator,
                                   TravelSessionRepository travelSessionRepository,
                                   TripRequestRepository tripRequestRepository,
                                   ItineraryRepository itineraryRepository, ObjectMapper objectMapper) {
        this.intentParser = intentParser;
        this.tripContextBuilder = tripContextBuilder;
        this.candidatePoiBuilder = candidatePoiBuilder;
        this.itineraryGenerator = itineraryGenerator;
        this.ruleValidator = ruleValidator;
        this.travelSessionRepository = travelSessionRepository;
        this.tripRequestRepository = tripRequestRepository;
        this.itineraryRepository = itineraryRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Itinerary handleUserMessage(Long sessionId, String userInput) {
        // 1. 获取或创建会话上下文
        TripContext context = tripContextBuilder.build(sessionId, userInput, null);

        // 获取当前行程
        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session != null && session.getCurrentItineraryId() != null) {
            ItineraryRepository.Itinerary itineraryEntity = itineraryRepository.findById(session.getCurrentItineraryId());
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
            unknown.setMarkdown("抱歉，我没有理解你的意思。请尝试描述你的旅行需求，例如：\"帮我规划去上海的三日旅游的行程\"");
            return unknown;
        }
    }

    @Override
    public Itinerary createPlan(Long sessionId, String userInput) {
        // 1. 解析意图
        TripContext tempContext = tripContextBuilder.build(sessionId, userInput, null);
        IntentParser.ParsedIntent parsedIntent = intentParser.parse(userInput, tempContext);

        if (parsedIntent.getTripRequest() == null ||
                parsedIntent.getTripRequest().getDestination() == null ||
                parsedIntent.getTripRequest().getDurationDays() == null) {
            Itinerary result = new Itinerary();
            result.setSessionId(sessionId);
            result.setMarkdown("请提供目的地和出行天数，例如：\"帮我规划去上海的三日旅游的行程\"");
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
        List<RouteInfo> routeInfos = estimateRoutes(candidatePois, tripRequest.getTransportMode());
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
    public Itinerary modifyPlan(Long sessionId, String userInput) {
        // 1. 获取当前行程
        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session == null || session.getCurrentItineraryId() == null) {
            Itinerary result = new Itinerary();
            result.setSessionId(sessionId);
            result.setMarkdown("当前没有行程，请先创建一个新行程。");
            return result;
        }

        ItineraryRepository.Itinerary currentItineraryEntity = itineraryRepository.findById(session.getCurrentItineraryId());
        if (currentItineraryEntity == null) {
            Itinerary result = new Itinerary();
            result.setSessionId(sessionId);
            result.setMarkdown("当前行程不存在，请先创建一个新行程。");
            return result;
        }

        // 2. 构建上下文
        TripRequest tripRequest = currentItineraryEntity.getRequestId() != null ?
                convertToTripRequest(tripRequestRepository.findById(currentItineraryEntity.getRequestId())) :
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

    private List<RouteInfo> estimateRoutes(List<Poi> pois, String transportMode) {
        List<RouteInfo> routeInfos = new ArrayList<>();

        // 直接使用本地估算（避免高德 QPS 超限）
        for (int i = 0; i < pois.size() - 1; i++) {
            Poi origin = pois.get(i);
            Poi destination = pois.get(i + 1);

            if (origin.getLongitude() == null || origin.getLatitude() == null ||
                    destination.getLongitude() == null || destination.getLatitude() == null) {
                continue;
            }

            RouteInfo routeInfo = estimateRouteLocally(origin, destination, transportMode);
            routeInfos.add(routeInfo);
        }

        return routeInfos;
    }

    private RouteInfo estimateRouteLocally(Poi origin, Poi destination, String transportMode) {
        RouteInfo routeInfo = new RouteInfo();
        routeInfo.setOriginName(origin.getName());
        routeInfo.setDestinationName(destination.getName());
        routeInfo.setTransportMode(transportMode);

        // Haversine 公式计算直线距离
        double distance = calculateDistance(
                origin.getLatitude().doubleValue(), origin.getLongitude().doubleValue(),
                destination.getLatitude().doubleValue(), destination.getLongitude().doubleValue());

        // 根据交通方式估算时间
        double avgSpeedKmh;
        switch (transportMode) {
            case "步行":
                avgSpeedKmh = 5;
                break;
            case "自驾":
                avgSpeedKmh = 40;
                break;
            case "公共交通":
            case "公共交通+步行":
            default:
                avgSpeedKmh = 25;
                break;
        }

        // 实际路线距离约为直线距离的 1.3 倍
        double actualDistance = distance * 1.3;
        routeInfo.setDistanceMeters((int) (actualDistance * 1000));
        routeInfo.setDurationMinutes((int) (actualDistance / avgSpeedKmh * 60));

        return routeInfo;
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
            TripRequestRepository.TripRequest requestEntity = new TripRequestRepository.TripRequest();
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
            tripRequestRepository.save(requestEntity);

            // 保存 Itinerary
            ItineraryRepository.Itinerary itineraryEntity = new ItineraryRepository.Itinerary();
            itineraryEntity.setSessionId(sessionId);
            itineraryEntity.setRequestId(requestEntity.getId());
            itineraryEntity.setVersion(1);
            itineraryEntity.setTitle(tripRequest.getDestination() + " " + tripRequest.getDurationDays() + "日游");
            itineraryEntity.setItineraryJson(objectMapper.writeValueAsString(itinerary));
            itineraryEntity.setMarkdownContent(itinerary.getMarkdown());
            itineraryEntity.setValidationStatus("PASSED");
            itineraryRepository.save(itineraryEntity);

            // 更新 TravelSession
            TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
            if (session == null) {
                session = new TravelSessionRepository.TravelSession();
                session.setId(sessionId);
                session.setSessionName("会话-" + sessionId);
                session.setStatus("ACTIVE");
            }
            session.setCurrentItineraryId(itineraryEntity.getId());
            travelSessionRepository.save(session);

            itinerary.setId(itineraryEntity.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trip data", e);
            throw new RuntimeException("保存行程失败", e);
        }
    }

    private void saveModifiedTripData(Long sessionId, TripRequest tripRequest, Itinerary itinerary, int version) {
        try {
            // 保存 TripRequest
            TripRequestRepository.TripRequest requestEntity = new TripRequestRepository.TripRequest();
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
            tripRequestRepository.save(requestEntity);

            // 保存 Itinerary
            ItineraryRepository.Itinerary itineraryEntity = new ItineraryRepository.Itinerary();
            itineraryEntity.setSessionId(sessionId);
            itineraryEntity.setRequestId(requestEntity.getId());
            itineraryEntity.setVersion(version);
            itineraryEntity.setTitle(tripRequest.getDestination() + " " + tripRequest.getDurationDays() + "日游 (v" + version + ")");
            itineraryEntity.setItineraryJson(objectMapper.writeValueAsString(itinerary));
            itineraryEntity.setMarkdownContent(itinerary.getMarkdown());
            itineraryEntity.setValidationStatus("PASSED");
            itineraryRepository.save(itineraryEntity);

            // 更新 TravelSession
            TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
            if (session != null) {
                session.setCurrentItineraryId(itineraryEntity.getId());
                travelSessionRepository.save(session);
            }

            itinerary.setId(itineraryEntity.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trip data", e);
            throw new RuntimeException("保存行程失败", e);
        }
    }

    private Itinerary convertToItinerary(ItineraryRepository.Itinerary entity) {
        if (entity == null) {
            return null;
        }

        Itinerary itinerary = new Itinerary();
        itinerary.setId(entity.getId());
        itinerary.setSessionId(entity.getSessionId());
        itinerary.setMarkdown(entity.getMarkdownContent());

        // 恢复 TripRequest
        if (entity.getRequestId() != null) {
            TripRequestRepository.TripRequest requestEntity = tripRequestRepository.findById(entity.getRequestId());
            if (requestEntity != null) {
                itinerary.setRequest(convertToTripRequest(requestEntity));
            }
        }

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

    private TripRequest convertToTripRequest(TripRequestRepository.TripRequest entity) {
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
