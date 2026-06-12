package com.travelmind.planner;

import com.travelmind.amap.AmapService;
import com.travelmind.domain.Itinerary;
import com.travelmind.domain.Poi;
import com.travelmind.llm.*;
import com.travelmind.planner.impl.IntentParserImpl;
import com.travelmind.planner.impl.TripPlannerServiceImpl;
import com.travelmind.storage.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TripPlannerService 服务级测试
 */
class TripPlannerServiceTest {

    private TripPlannerService tripPlannerService;
    private TravelSessionRepository travelSessionRepository;
    private ItineraryRepository itineraryRepository;
    private LlmClient mockLlmClient;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // 创建真实的 Repository
        travelSessionRepository = new TravelSessionRepository();
        TripRequestRepository tripRequestRepository = new TripRequestRepository();
        itineraryRepository = new ItineraryRepository();
        PoiCacheRepository poiCacheRepository = new PoiCacheRepository();
        LlmCallLogRepository llmCallLogRepository = new LlmCallLogRepository();

        // 创建 Mock LLM 客户端
        mockLlmClient = createMockLlmClient();
        LlmClientFactory llmClientFactory = new LlmClientFactory(null, null) {
            @Override
            public LlmClient getClient() {
                return mockLlmClient;
            }
        };

        // 创建 Mock AmapService
        AmapService mockAmapService = createMockAmapService();

        // 创建依赖
        IntentParser intentParser = new IntentParserImpl(llmClientFactory, objectMapper, llmCallLogRepository);
        TripContextBuilder tripContextBuilder = new TripContextBuilder();
        CandidatePoiBuilder candidatePoiBuilder = new CandidatePoiBuilder(mockAmapService, poiCacheRepository, objectMapper);
        ItineraryGenerator itineraryGenerator = new ItineraryGenerator(llmClientFactory, objectMapper, llmCallLogRepository);
        RuleValidator ruleValidator = new RuleValidator();

        tripPlannerService = new TripPlannerServiceImpl(
                intentParser, tripContextBuilder, candidatePoiBuilder,
                itineraryGenerator, ruleValidator,
                travelSessionRepository, tripRequestRepository,
                itineraryRepository, objectMapper
        );
    }

    @Test
    void createPlan_ShouldGenerateItinerary() {
        Itinerary result = tripPlannerService.createPlan(1L, "帮我规划去上海的两日游");

        assertNotNull(result);
        assertNotNull(result.getMarkdown());
        assertTrue(result.getMarkdown().contains("上海"));
    }

    @Test
    void handleUserMessage_ShouldReturnItinerary() {
        Itinerary result = tripPlannerService.handleUserMessage(1L, "帮我规划去上海的两日游");

        assertNotNull(result);
        assertNotNull(result.getMarkdown());
    }

    @Test
    void handleUserMessage_ClarificationShouldNotClearCurrentItinerary() {
        Itinerary first = tripPlannerService.handleUserMessage(1L, "帮我规划去上海的两日游");
        assertNotNull(first.getId());

        Long currentBefore = travelSessionRepository.findById(1L).getCurrentItineraryId();
        Itinerary clarification = tripPlannerService.handleUserMessage(1L, "帮我规划一个旅游");

        assertNotNull(clarification.getMarkdown());
        assertNull(clarification.getId());
        assertEquals(currentBefore, travelSessionRepository.findById(1L).getCurrentItineraryId());
    }

    @Test
    void handleUserMessage_ModifyPlanShouldCreateNewVersion() {
        Itinerary first = tripPlannerService.handleUserMessage(1L, "帮我规划去上海的两日游");
        Itinerary modified = tripPlannerService.handleUserMessage(1L, "第二天不要去博物馆，换成迪士尼");

        assertNotNull(first.getId());
        assertNotNull(modified.getId());
        assertNotEquals(first.getId(), modified.getId());
        assertTrue(modified.getMarkdown().contains("修改说明"));
        assertEquals(2, itineraryRepository.findBySessionId(1L).size());
    }

    private LlmClient createMockLlmClient() {
        return new LlmClient() {
            @Override
            public LlmResponse chat(LlmRequest request) {
                LlmResponse response = new LlmResponse();
                response.setLatencyMs(100L);

                if ("INTENT_PARSE".equals(request.getCallType())) {
                    String prompt = request.getMessages().stream()
                            .map(LlmRequest.Message::getContent)
                            .reduce("", (left, right) -> left + "\n" + right);
                    if (prompt.contains("帮我规划一个旅游")) {
                        response.setContent("""
                                {
                                  "intent": "NEW_PLAN",
                                  "destination": null,
                                  "durationDays": null,
                                  "needClarification": true,
                                  "questions": ["你想去哪个城市？计划玩几天？"],
                                  "modificationInstruction": null
                                }
                                """);
                    } else if (prompt.contains("第二天") || prompt.contains("换成")) {
                        response.setContent("""
                                {
                                  "intent": "MODIFY_PLAN",
                                  "destination": null,
                                  "durationDays": null,
                                  "needClarification": false,
                                  "questions": [],
                                  "modificationInstruction": "第二天不要去博物馆，换成迪士尼"
                                }
                                """);
                    } else {
                        response.setContent("""
                            {
                              "intent": "NEW_PLAN",
                              "destination": "上海",
                              "durationDays": 2,
                              "needClarification": false,
                              "questions": [],
                              "modificationInstruction": null
                            }
                            """);
                    }
                } else if ("ITINERARY_MODIFY".equals(request.getCallType())) {
                    response.setContent("""
                            # 上海两日游

                            ## 第 1 天：经典上海

                            - 上午：人民广场、上海博物馆
                            - 中午：南京东路用餐
                            - 下午：外滩
                            - 晚上：陆家嘴夜景

                            ## 第 2 天：迪士尼主题日

                            - 上午：上海迪士尼乐园
                            - 中午：园区用餐
                            - 下午：继续游玩
                            - 晚上：烟花或返程

                            ## 注意事项
                            - 请提前确认门票和开放时间

                            ## 修改说明
                            - 已将第二天调整为迪士尼
                            """);
                } else {
                    response.setContent("""
                            # 上海两日游

                            ## 第 1 天：经典上海

                            - 上午：人民广场、上海博物馆
                            - 中午：南京东路用餐
                            - 下午：外滩
                            - 晚上：陆家嘴夜景

                            ## 第 2 天：现代上海

                            - 上午：田子坊
                            - 下午：上海科技馆

                            ## 注意事项
                            - 请提前预约博物馆
                            """);
                }
                return response;
            }

            @Override
            public String getProviderName() { return "mock"; }

            @Override
            public String getModelName() { return "mock-model"; }
        };
    }

    private AmapService createMockAmapService() {
        return new AmapService() {
            @Override
            public List<Poi> searchPois(String city, String keyword, int limit) {
                Poi poi = new Poi();
                poi.setSource("AMAP");
                poi.setSourceId("B001");
                poi.setName("上海博物馆");
                poi.setCity(city);
                poi.setLongitude(new BigDecimal("121.47"));
                poi.setLatitude(new BigDecimal("31.23"));
                return List.of(poi);
            }

            @Override
            public Poi geocode(String address) {
                Poi poi = new Poi();
                poi.setSource("AMAP");
                poi.setName(address);
                poi.setLongitude(new BigDecimal("121.47"));
                poi.setLatitude(new BigDecimal("31.23"));
                return poi;
            }

        };
    }
}
