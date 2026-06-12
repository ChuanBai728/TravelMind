package com.travelmind.planner;

import com.travelmind.domain.Itinerary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则校验器，对行程进行基础兜底校验
 */
@Component
public class RuleValidator {

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

        // 2. 必须包含"第 1 天"
        if (!markdown.contains("第 1 天")) {
            warnings.add("行程缺少第 1 天的安排");
        }

        // 3. 检查天数完整性
        if (itinerary.getRequest() != null && itinerary.getRequest().getDurationDays() != null) {
            int days = itinerary.getRequest().getDurationDays();
            for (int i = 1; i <= days; i++) {
                if (!markdown.contains("第 " + i + " 天")) {
                    warnings.add("行程缺少第 " + i + " 天的安排");
                }
            }
        }

        // 4. 检查时间段
        String[] timeSlots = {"上午", "中午", "下午", "晚上"};
        for (String slot : timeSlots) {
            if (!markdown.contains(slot)) {
                warnings.add("行程可能缺少" + slot + "的安排");
            }
        }

        // 5. 检查是否有注意事项
        if (!markdown.contains("注意事项") && !markdown.contains("提醒")) {
            warnings.add("行程缺少注意事项或提醒");
        }

        // 6. 检查每天活动数量
        checkDailyActivities(markdown, warnings);

        result.setPassed(warnings.isEmpty());
        result.setWarnings(warnings);
        return result;
    }

    private void checkDailyActivities(String markdown, List<String> warnings) {
        // 简单检查：如果某一天出现超过 6 个列表项，添加提醒
        Pattern dayPattern = Pattern.compile("第 \\d+ 天[：:].*?(?=第 \\d+ 天|$)", Pattern.DOTALL);
        Matcher matcher = dayPattern.matcher(markdown);

        while (matcher.find()) {
            String dayContent = matcher.group();
            long listItemCount = dayContent.lines()
                    .filter(line -> line.trim().startsWith("- ") || line.trim().startsWith("* "))
                    .count();

            if (listItemCount > 6) {
                // 提取天数
                Pattern dayIndexPattern = Pattern.compile("第 (\\d+) 天");
                Matcher dayIndexMatcher = dayIndexPattern.matcher(dayContent);
                if (dayIndexMatcher.find()) {
                    warnings.add("第 " + dayIndexMatcher.group(1) + " 天安排可能偏满，建议适当精简");
                }
            }
        }
    }

    /**
     * 校验结果
     */
    public static class ValidationResult {
        /**
         * 是否通过
         */
        private boolean passed;

        /**
         * 警告信息列表
         */
        private List<String> warnings;

        public ValidationResult() {
            this.warnings = new ArrayList<>();
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }
    }
}
