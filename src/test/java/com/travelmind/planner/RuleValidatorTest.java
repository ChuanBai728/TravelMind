package com.travelmind.planner;

import com.travelmind.domain.Itinerary;
import com.travelmind.domain.TripRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleValidator 单元测试
 */
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
    void validate_NullMarkdown_ShouldFail() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown(null);

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertFalse(result.isPassed());
        assertTrue(result.getWarnings().contains("行程内容为空"));
    }

    @Test
    void validate_NullItinerary_ShouldFail() {
        RuleValidator.ValidationResult result = ruleValidator.validate(null);

        assertFalse(result.isPassed());
        assertTrue(result.getWarnings().contains("行程对象为空"));
    }

    @Test
    void validate_MissingDay1_ShouldWarn() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown("# 行程\n\n## 第 2 天：测试\n- 上午：测试");

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.getWarnings().contains("行程缺少第 1 天的安排"));
    }

    @Test
    void validate_ThreeDaysTrip_MissingDay3_ShouldWarn() {
        Itinerary itinerary = new Itinerary();
        TripRequest request = new TripRequest();
        request.setDurationDays(3);
        itinerary.setRequest(request);
        itinerary.setMarkdown("# 行程\n\n## 第 1 天：测试\n## 第 2 天：测试");

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.getWarnings().contains("行程缺少第 3 天的安排"));
    }

    @Test
    void validate_MissingTimeSlots_ShouldWarn() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown("# 行程\n\n## 第 1 天：测试\n- 活动1");

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("上午")));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("中午")));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("下午")));
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("晚上")));
    }

    @Test
    void validate_MissingReminders_ShouldWarn() {
        Itinerary itinerary = new Itinerary();
        itinerary.setMarkdown("# 行程\n\n## 第 1 天：测试\n- 上午：测试\n- 中午：测试\n- 下午：测试\n- 晚上：测试");

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
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    void validate_TooManyActivities_ShouldWarn() {
        Itinerary itinerary = new Itinerary();
        TripRequest request = new TripRequest();
        request.setDurationDays(1);
        itinerary.setRequest(request);
        itinerary.setMarkdown("""
                # 行程

                ## 第 1 天：测试

                - 上午：活动1
                - 上午：活动2
                - 中午：活动3
                - 下午：活动4
                - 下午：活动5
                - 晚上：活动6
                - 晚上：活动7
                - 晚上：活动8

                ## 注意事项
                - 测试提醒
                """);

        RuleValidator.ValidationResult result = ruleValidator.validate(itinerary);

        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("安排可能偏满")));
    }
}
