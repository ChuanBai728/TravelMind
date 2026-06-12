package com.travelmind.planner;

import com.travelmind.domain.Itinerary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则校验器，对行程进行基础兜底校验
 */
@Component
public class RuleValidator {

    // 中文数字映射
    private static final String[] CN_NUMS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    /**
     * 校验行程
     *
     * @param itinerary 行程对象
     * @return 校验结果
     */
    public ValidationResult validate(Itinerary itinerary) {
        ValidationResult result = new ValidationResult();
        List<String> warnings = new ArrayList<>();

        if (itinerary == null) {
            result.setPassed(false);
            warnings.add("行程对象为空");
            result.setWarnings(warnings);
            return result;
        }

        String markdown = itinerary.getMarkdown();

        // 1. Markdown 不能为空
        if (markdown == null || markdown.trim().isEmpty()) {
            result.setPassed(false);
            warnings.add("行程内容为空");
            result.setWarnings(warnings);
            return result;
        }

        // 2. 检查天数完整性（支持多种格式）
        if (itinerary.getRequest() != null && itinerary.getRequest().getDurationDays() != null) {
            int days = itinerary.getRequest().getDurationDays();
            for (int i = 1; i <= days; i++) {
                if (!containsDay(markdown, i)) {
                    warnings.add("行程可能缺少第 " + i + " 天的安排");
                }
            }
        }

        // 3. 检查时间段
        String[] timeSlots = {"上午", "中午", "下午", "晚上", "Morning", "Afternoon", "Evening"};
        int foundSlots = 0;
        for (String slot : timeSlots) {
            if (markdown.contains(slot)) {
                foundSlots++;
            }
        }
        if (foundSlots < 2) {
            warnings.add("行程可能缺少时间段安排（上午/下午/晚上）");
        }

        // 4. 检查是否有注意事项
        if (!markdown.contains("注意事项") && !markdown.contains("提醒") &&
                !markdown.contains("Tips") && !markdown.contains("注意")) {
            warnings.add("行程缺少注意事项或提醒");
        }

        result.setPassed(warnings.isEmpty());
        result.setWarnings(warnings);
        return result;
    }

    /**
     * 检查 Markdown 是否包含某一天的内容
     * 支持格式：第 1 天、第1天、第一天、Day 1、day1
     */
    private boolean containsDay(String markdown, int dayIndex) {
        String lower = markdown.toLowerCase();

        // 第 1 天 / 第1天
        if (lower.contains("第 " + dayIndex + " 天") || lower.contains("第" + dayIndex + "天")) {
            return true;
        }

        // 第一天、第二天...
        if (dayIndex <= 10 && lower.contains("第" + CN_NUMS[dayIndex] + "天")) {
            return true;
        }

        // Day 1 / day 1 / Day1
        if (lower.contains("day " + dayIndex) || lower.contains("day" + dayIndex)) {
            return true;
        }

        return false;
    }

    /**
     * 校验结果
     */
    public static class ValidationResult {
        private boolean passed;
        private List<String> warnings;

        public ValidationResult() {
            this.warnings = new ArrayList<>();
        }

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
}
