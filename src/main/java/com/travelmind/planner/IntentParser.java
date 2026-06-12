package com.travelmind.planner;

import com.travelmind.domain.TripRequest;

import java.util.List;

/**
 * 意图解析器接口
 */
public interface IntentParser {

    /**
     * 解析用户输入
     *
     * @param userInput 用户输入文本
     * @param context   规划上下文
     * @return 解析后的意图
     */
    ParsedIntent parse(String userInput, TripContext context);

    /**
     * 解析后的意图
     */
    class ParsedIntent {
        /**
         * 意图类型：NEW_PLAN, MODIFY_PLAN, UNKNOWN
         */
        private String intent;

        /**
         * 旅行需求
         */
        private TripRequest tripRequest;

        /**
         * 是否需要追问
         */
        private boolean needClarification;

        /**
         * 追问问题列表
         */
        private List<String> questions;

        /**
         * 修改指令（当 intent 为 MODIFY_PLAN 时）
         */
        private String modificationInstruction;

        public ParsedIntent() {
        }

        public String getIntent() {
            return intent;
        }

        public void setIntent(String intent) {
            this.intent = intent;
        }

        public TripRequest getTripRequest() {
            return tripRequest;
        }

        public void setTripRequest(TripRequest tripRequest) {
            this.tripRequest = tripRequest;
        }

        public boolean isNeedClarification() {
            return needClarification;
        }

        public void setNeedClarification(boolean needClarification) {
            this.needClarification = needClarification;
        }

        public List<String> getQuestions() {
            return questions;
        }

        public void setQuestions(List<String> questions) {
            this.questions = questions;
        }

        public String getModificationInstruction() {
            return modificationInstruction;
        }

        public void setModificationInstruction(String modificationInstruction) {
            this.modificationInstruction = modificationInstruction;
        }
    }
}
