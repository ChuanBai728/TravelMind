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
import java.util.function.Consumer;

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
        TripContext context = tripContextBuilder.build(sessionId, userInput, null);

        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session != null && session.getCurrentItineraryId() != null) {
            ItineraryRepository.Itinerary itineraryEntity = itineraryRepository.findById(session.getCurrentItineraryId());
            if (itineraryEntity != null) {
                Itinerary currentItinerary = convertToItinerary(itineraryEntity);
                context.setCurrentItinerary(currentItinerary);
            }
        }

        IntentParser.ParsedIntent parsedIntent = intentParser.parse(userInput, context);

        if (parsedIntent.isNeedClarification()) {
            return buildClarification(sessionId, parsedIntent);
        }

        String intent = parsedIntent.getIntent();
        if ("NEW_PLAN".equals(intent)) {
            return createPlan(sessionId, userInput);
        } else if ("MODIFY_PLAN".equals(intent)) {
            return modifyPlan(sessionId, userInput);
        } else {
            return buildUnknownResponse(sessionId);
        }
    }

    @Override
    public Itinerary handleUserMessageStream(Long sessionId, String userInput, Consumer<String> callback) {
        TripContext context = tripContextBuilder.build(sessionId, userInput, null);

        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session != null && session.getCurrentItineraryId() != null) {
            ItineraryRepository.Itinerary itineraryEntity = itineraryRepository.findById(session.getCurrentItineraryId());
            if (itineraryEntity != null) {
                Itinerary currentItinerary = convertToItinerary(itineraryEntity);
                context.setCurrentItinerary(currentItinerary);
            }
        }

        // 意图解析（同步，不需要流式）
        System.out.println("  正在分析你的需求...");
        IntentParser.ParsedIntent parsedIntent;
        try {
            parsedIntent = intentParser.parse(userInput, context);
        } catch (Exception e) {
            log.error("Intent parsing failed", e);
            return buildErrorResponse(sessionId, "意图解析失败: " + e.getMessage());
        }

        if (parsedIntent.isNeedClarification()) {
            return buildClarification(sessionId, parsedIntent);
        }

        String intent = parsedIntent.getIntent();
        log.info("Parsed intent: {}", intent);

        try {
            if ("NEW_PLAN".equals(intent)) {
                return createPlanStream(sessionId, userInput, callback);
            } else if ("MODIFY_PLAN".equals(intent)) {
                return modifyPlanStream(sessionId, userInput, callback);
            } else {
                return buildUnknownResponse(sessionId);
            }
        } catch (Exception e) {
            log.error("Plan execution failed for intent: {}", intent, e);
            return buildErrorResponse(sessionId, "行程处理失败: " + e.getMessage());
        }
    }

    @Override
    public Itinerary createPlan(Long sessionId, String userInput) {
        TripRequest tripRequest = parseAndValidateTripRequest(sessionId, userInput);
        if (tripRequest == null) {
            return buildErrorResponse(sessionId, "请提供目的地和出行天数，例如：\"帮我规划去上海的三日旅游的行程\"");
        }

        TripContext context = buildTripContext(sessionId, userInput, tripRequest);

        Itinerary itinerary = itineraryGenerator.generate(context);

        validateAndAttachWarnings(itinerary);
        saveTripData(sessionId, tripRequest, itinerary);

        return itinerary;
    }

    @Override
    public Itinerary createPlanStream(Long sessionId, String userInput, Consumer<String> callback) {
        TripRequest tripRequest = parseAndValidateTripRequest(sessionId, userInput);
        if (tripRequest == null) {
            return buildErrorResponse(sessionId, "请提供目的地和出行天数，例如：\"帮我规划去上海的三日旅游的行程\"");
        }

        TripContext context = buildTripContext(sessionId, userInput, tripRequest);

        Itinerary itinerary = itineraryGenerator.generateStream(context, callback);

        validateAndAttachWarnings(itinerary);
        saveTripData(sessionId, tripRequest, itinerary);

        return itinerary;
    }

    @Override
    public Itinerary modifyPlan(Long sessionId, String userInput) {
        Itinerary currentItinerary = getCurrentItinerary(sessionId);
        if (currentItinerary == null) {
            return buildErrorResponse(sessionId, "当前没有行程，请先创建一个新行程。");
        }

        TripContext context = buildModifyContext(sessionId, userInput, currentItinerary);

        Itinerary itinerary = itineraryGenerator.modify(context, currentItinerary.getMarkdown(), userInput);

        validateAndAttachWarnings(itinerary);

        int newVersion = getNextVersion(sessionId);
        saveModifiedTripData(sessionId, context.getTripRequest(), itinerary, newVersion);

        return itinerary;
    }

    @Override
    public Itinerary modifyPlanStream(Long sessionId, String userInput, Consumer<String> callback) {
        Itinerary currentItinerary = getCurrentItinerary(sessionId);
        if (currentItinerary == null) {
            return buildErrorResponse(sessionId, "当前没有行程，请先创建一个新行程。");
        }

        TripContext context = buildModifyContext(sessionId, userInput, currentItinerary);

        Itinerary itinerary = itineraryGenerator.modifyStream(context, currentItinerary.getMarkdown(), userInput, callback);

        validateAndAttachWarnings(itinerary);

        int newVersion = getNextVersion(sessionId);
        saveModifiedTripData(sessionId, context.getTripRequest(), itinerary, newVersion);

        return itinerary;
    }

    // ========== 辅助方法 ==========

    private TripRequest parseAndValidateTripRequest(Long sessionId, String userInput) {
        TripContext tempContext = tripContextBuilder.build(sessionId, userInput, null);
        IntentParser.ParsedIntent parsedIntent = intentParser.parse(userInput, tempContext);

        if (parsedIntent.getTripRequest() == null ||
                parsedIntent.getTripRequest().getDestination() == null ||
                parsedIntent.getTripRequest().getDurationDays() == null) {
            return null;
        }

        TripRequest tripRequest = tripContextBuilder.fillDefaults(parsedIntent.getTripRequest());
        tripRequest.setRawInput(userInput);
        return tripRequest;
    }

    private TripContext buildTripContext(Long sessionId, String userInput, TripRequest tripRequest) {
        TripContext context = tripContextBuilder.build(sessionId, userInput, tripRequest);
        List<Poi> candidatePois = candidatePoiBuilder.build(tripRequest);
        context.setCandidatePois(candidatePois);
        List<RouteInfo> routeInfos = estimateRoutes(candidatePois, tripRequest.getTransportMode());
        context.setRouteInfos(routeInfos);
        return context;
    }

    private TripContext buildModifyContext(Long sessionId, String userInput, Itinerary currentItinerary) {
        TripRequest tripRequest = currentItinerary.getRequest() != null ? currentItinerary.getRequest() : new TripRequest();
        TripContext context = tripContextBuilder.build(sessionId, userInput, tripRequest);
        List<Poi> candidatePois = candidatePoiBuilder.build(tripRequest);
        context.setCandidatePois(candidatePois);
        return context;
    }

    private Itinerary getCurrentItinerary(Long sessionId) {
        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session == null || session.getCurrentItineraryId() == null) {
            return null;
        }
        ItineraryRepository.Itinerary entity = itineraryRepository.findById(session.getCurrentItineraryId());
        if (entity == null) {
            return null;
        }
        return convertToItinerary(entity);
    }

    private int getNextVersion(Long sessionId) {
        TravelSessionRepository.TravelSession session = travelSessionRepository.findById(sessionId);
        if (session != null && session.getCurrentItineraryId() != null) {
            ItineraryRepository.Itinerary entity = itineraryRepository.findById(session.getCurrentItineraryId());
            if (entity != null) {
                return entity.getVersion() + 1;
            }
        }
        return 1;
    }

    private void validateAndAttachWarnings(Itinerary itinerary) {
        RuleValidator.ValidationResult validationResult = ruleValidator.validate(itinerary);
        if (!validationResult.getWarnings().isEmpty()) {
            itinerary.setReminders(validationResult.getWarnings());
        }
    }

    private Itinerary buildClarification(Long sessionId, IntentParser.ParsedIntent parsedIntent) {
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

    private Itinerary buildUnknownResponse(Long sessionId) {
        Itinerary unknown = new Itinerary();
        unknown.setSessionId(sessionId);
        unknown.setMarkdown("抱歉，我没有理解你的意思。请尝试描述你的旅行需求，例如：\"帮我规划去上海的三日旅游的行程\"");
        return unknown;
    }

    private Itinerary buildErrorResponse(Long sessionId, String message) {
        Itinerary result = new Itinerary();
        result.setSessionId(sessionId);
        result.setMarkdown(message);
        return result;
    }

    // ========== 路线估算 ==========

    private List<RouteInfo> estimateRoutes(List<Poi> pois, String transportMode) {
        List<RouteInfo> routeInfos = new ArrayList<>();
        for (int i = 0; i < pois.size() - 1; i++) {
            Poi origin = pois.get(i);
            Poi destination = pois.get(i + 1);
            if (origin.getLongitude() == null || origin.getLatitude() == null ||
                    destination.getLongitude() == null || destination.getLatitude() == null) {
                continue;
            }
            routeInfos.add(estimateRouteLocally(origin, destination, transportMode));
        }
        return routeInfos;
    }

    private RouteInfo estimateRouteLocally(Poi origin, Poi destination, String transportMode) {
        RouteInfo routeInfo = new RouteInfo();
        routeInfo.setOriginName(origin.getName());
        routeInfo.setDestinationName(destination.getName());
        routeInfo.setTransportMode(transportMode);

        double distance = calculateDistance(
                origin.getLatitude().doubleValue(), origin.getLongitude().doubleValue(),
                destination.getLatitude().doubleValue(), destination.getLongitude().doubleValue());

        double avgSpeedKmh;
        switch (transportMode) {
            case "步行": avgSpeedKmh = 5; break;
            case "自驾": avgSpeedKmh = 40; break;
            default: avgSpeedKmh = 25; break;
        }

        double actualDistance = distance * 1.3;
        routeInfo.setDistanceMeters((int) (actualDistance * 1000));
        routeInfo.setDurationMinutes((int) (actualDistance / avgSpeedKmh * 60));

        return routeInfo;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ========== 数据持久化 ==========

    private void saveTripData(Long sessionId, TripRequest tripRequest, Itinerary itinerary) {
        try {
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

            ItineraryRepository.Itinerary itineraryEntity = new ItineraryRepository.Itinerary();
            itineraryEntity.setSessionId(sessionId);
            itineraryEntity.setRequestId(requestEntity.getId());
            itineraryEntity.setVersion(1);
            itineraryEntity.setTitle(tripRequest.getDestination() + " " + tripRequest.getDurationDays() + "日游");
            itineraryEntity.setItineraryJson(objectMapper.writeValueAsString(itinerary));
            itineraryEntity.setMarkdownContent(itinerary.getMarkdown());
            itineraryEntity.setValidationStatus("PASSED");
            itineraryRepository.save(itineraryEntity);

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

            ItineraryRepository.Itinerary itineraryEntity = new ItineraryRepository.Itinerary();
            itineraryEntity.setSessionId(sessionId);
            itineraryEntity.setRequestId(requestEntity.getId());
            itineraryEntity.setVersion(version);
            itineraryEntity.setTitle(tripRequest.getDestination() + " " + tripRequest.getDurationDays() + "日游 (v" + version + ")");
            itineraryEntity.setItineraryJson(objectMapper.writeValueAsString(itinerary));
            itineraryEntity.setMarkdownContent(itinerary.getMarkdown());
            itineraryEntity.setValidationStatus("PASSED");
            itineraryRepository.save(itineraryEntity);

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
        if (entity == null) return null;

        Itinerary itinerary = new Itinerary();
        itinerary.setId(entity.getId());
        itinerary.setSessionId(entity.getSessionId());
        itinerary.setMarkdown(entity.getMarkdownContent());

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
        if (entity == null) return null;

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
