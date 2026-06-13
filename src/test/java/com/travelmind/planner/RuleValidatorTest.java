package com.travelmind.planner;

import com.travelmind.domain.Itinerary;
import com.travelmind.domain.TripRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleValidatorTest {

    private RuleValidator ruleValidator;

    @BeforeEach
    void setUp() {
        ruleValidator = new RuleValidator();
    }

    @Test
    void validate_EmptyMarkdown_ShouldFail() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown("");

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertFalse(result.isPassed());
        assertTrue(result.getWarnings().contains("行程内容为空"));
    }

    @Test
    void validate_NullItinerary_ShouldFail() {
        RuleValidator.ValidationResult result = ruleValidator.validate(null);

        assertFalse(result.isPassed());
    }

    @Test
    void validate_MissingTimeSlots_ShouldWarn() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown("# 行程\n\n## 第 1 天：测试\n- 活动1");

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("时间段")));
    }

    @Test
    void validate_MissingReminders_ShouldWarn() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown("# 行程\n\n## 第 1 天：测试\n- 上午：测试\n- 下午：测试");

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("注意事项") || w.contains("提醒")));
    }

    @Test
    void validate_ValidItinerary_ShouldPass() {
        Itinerary itinerary = new Itinerary();
        TripRequest request = new TripRequest();
        request.setDurationDays(1);
        itinerary.setRequest(request);
        itinerary.setMarkdown("""
                # 行程

                ## 第 1 天：测试

                - 上午：测试活动
                - 中午：午餐
                - 下午：测试活动2
                - 晚上：晚餐

                ## 注意事项
                - 测试提醒
                """);

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.isPassed());
    }

    @Test
    void validate_ChineseDayFormat_ShouldWork() {
        Itinerary itinerary = new Itinerary();
        TripRequest request = new TripRequest();
        request.setDurationDays(2);
        itinerary.setRequest(request);
        itinerary.setMarkdown("""
                # 行程

                ## 第一天：测试

                - 上午：测试
                - 下午：测试
                - 晚上：测试

                ## 第二天：测试

                - 上午：测试
                - 下午：测试
                - 晚上：测试

                ## 注意事项
                - 测试提醒
                """);

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.isPassed());
    }

    @Test
    void validate_DayFormat_ShouldWork() {
        Itinerary itinerary = new Itinerary();
        TripRequest request = new TripRequest();
        request.setDurationDays(1);
        itinerary.setRequest(request);
        itinerary.setMarkdown("""
                # Trip

                ## Day 1

                - Morning: test
                - Afternoon: test

                ## Tips
                - reminder
                """);

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.isPassed());
    }
}
