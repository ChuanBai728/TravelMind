package com.travelmind.planner;

import com.travelmind.domain.TripRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TripContextBuilder 单元测试
 */
class TripContextBuilderTest {

    private TripContextBuilder tripContextBuilder;

    @BeforeEach
    void setUp() {
        tripContextBuilder = new TripContextBuilder();
    }

    @Test
    void fillDefaults_MissingBudget_ShouldDefaultToComfortable() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);

        TripRequest result = tripContextBuilder.fillDefaults(request);

        assertEquals("舒适型", result.getBudgetLevel());
    }

    @Test
    void fillDefaults_MissingTransportMode_ShouldDefaultToPublicTransport() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);

        TripRequest result = tripContextBuilder.fillDefaults(request);

        assertEquals("公共交通+步行", result.getTransportMode());
    }

    @Test
    void fillDefaults_MissingPaceLevel_ShouldDefaultToModerate() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);

        TripRequest result = tripContextBuilder.fillDefaults(request);

        assertEquals("适中", result.getPaceLevel());
    }

    @Test
    void fillDefaults_MissingPeopleCount_ShouldDefaultTo2() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);

        TripRequest result = tripContextBuilder.fillDefaults(request);

        assertEquals(2, result.getPeopleCount());
    }

    @Test
    void fillDefaults_MissingPreferences_ShouldDefaultToClassic() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);

        TripRequest result = tripContextBuilder.fillDefaults(request);

        assertNotNull(result.getPreferences());
        assertTrue(result.getPreferences().contains("第一次到访经典路线"));
    }

    @Test
    void fillDefaults_AllFieldsPresent_ShouldNotChange() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);
        request.setBudgetLevel("高预算");
        request.setTransportMode("自驾");
        request.setPaceLevel("紧凑");
        request.setPeopleCount(4);
        request.setPreferences(List.of("美食", "购物"));

        TripRequest result = tripContextBuilder.fillDefaults(request);

        assertEquals("高预算", result.getBudgetLevel());
        assertEquals("自驾", result.getTransportMode());
        assertEquals("紧凑", result.getPaceLevel());
        assertEquals(4, result.getPeopleCount());
        assertEquals(List.of("美食", "购物"), result.getPreferences());
    }

    @Test
    void fillDefaults_NullRequest_ShouldReturnNull() {
        TripRequest result = tripContextBuilder.fillDefaults(null);

        assertNull(result);
    }

    @Test
    void fillDefaults_EmptyPreferences_ShouldDefault() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);
        request.setPreferences(new ArrayList<>());

        TripRequest result = tripContextBuilder.fillDefaults(request);

        assertNotNull(result.getPreferences());
        assertFalse(result.getPreferences().isEmpty());
    }

    @Test
    void build_ShouldCreateContext() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);

        TripContext context = tripContextBuilder.build(1L, "帮我规划去上海的三日游", request);

        assertNotNull(context);
        assertEquals(1L, context.getSessionId());
        assertEquals("帮我规划去上海的三日游", context.getUserInput());
        assertNotNull(context.getTripRequest());
        assertNotNull(context.getContextSummary());
    }

    @Test
    void build_ShouldFillDefaults() {
        TripRequest request = new TripRequest();
        request.setDestination("上海");
        request.setDurationDays(3);

        TripContext context = tripContextBuilder.build(1L, "帮我规划去上海的三日游", request);

        assertEquals("舒适型", context.getTripRequest().getBudgetLevel());
        assertEquals("公共交通+步行", context.getTripRequest().getTransportMode());
        assertEquals("适中", context.getTripRequest().getPaceLevel());
        assertEquals(2, context.getTripRequest().getPeopleCount());
    }
}
