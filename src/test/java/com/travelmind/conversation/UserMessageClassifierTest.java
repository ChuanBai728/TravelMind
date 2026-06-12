package com.travelmind.conversation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserMessageClassifier 单元测试
 */
class UserMessageClassifierTest {

    private UserMessageClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new UserMessageClassifier();
    }

    @Test
    void classify_NullInput_ShouldReturnUnknown() {
        ConversationIntent result = classifier.classify(null);

        assertEquals(ConversationIntent.UNKNOWN, result);
    }

    @Test
    void classify_EmptyInput_ShouldReturnUnknown() {
        ConversationIntent result = classifier.classify("");

        assertEquals(ConversationIntent.UNKNOWN, result);
    }

    @Test
    void classify_BlankInput_ShouldReturnUnknown() {
        ConversationIntent result = classifier.classify("   ");

        assertEquals(ConversationIntent.UNKNOWN, result);
    }

    @Test
    void classify_HelpCommand_ShouldReturnSystemCommand() {
        ConversationIntent result = classifier.classify("/help");

        assertEquals(ConversationIntent.SYSTEM_COMMAND, result);
    }

    @Test
    void classify_NewCommand_ShouldReturnSystemCommand() {
        ConversationIntent result = classifier.classify("/new");

        assertEquals(ConversationIntent.SYSTEM_COMMAND, result);
    }

    @Test
    void classify_HistoryCommand_ShouldReturnSystemCommand() {
        ConversationIntent result = classifier.classify("/history");

        assertEquals(ConversationIntent.SYSTEM_COMMAND, result);
    }

    @Test
    void classify_ExitCommand_ShouldReturnSystemCommand() {
        ConversationIntent result = classifier.classify("/exit");

        assertEquals(ConversationIntent.SYSTEM_COMMAND, result);
    }

    @Test
    void classify_ExportCommand_ShouldReturnExport() {
        ConversationIntent result = classifier.classify("/export");

        assertEquals(ConversationIntent.EXPORT, result);
    }

    @Test
    void classify_NewPlanRequest_ShouldReturnNewPlan() {
        ConversationIntent result = classifier.classify("帮我规划去上海的三日旅游的行程");

        assertEquals(ConversationIntent.NEW_PLAN, result);
    }

    @Test
    void classify_PlanRequest_ShouldReturnNewPlan() {
        ConversationIntent result = classifier.classify("我想去北京旅游");

        assertEquals(ConversationIntent.NEW_PLAN, result);
    }

    @Test
    void classify_ModifyDay2_ShouldReturnModifyPlan() {
        ConversationIntent result = classifier.classify("第二天不要去博物馆");

        assertEquals(ConversationIntent.MODIFY_PLAN, result);
    }

    @Test
    void classify_ChangeActivity_ShouldReturnModifyPlan() {
        ConversationIntent result = classifier.classify("把迪士尼换成东方明珠");

        assertEquals(ConversationIntent.MODIFY_PLAN, result);
    }

    @Test
    void classify_RemoveActivity_ShouldReturnModifyPlan() {
        ConversationIntent result = classifier.classify("去掉第三天的购物安排");

        assertEquals(ConversationIntent.MODIFY_PLAN, result);
    }

    @Test
    void classify_AdjustSchedule_ShouldReturnModifyPlan() {
        ConversationIntent result = classifier.classify("调整一下第二天的行程");

        assertEquals(ConversationIntent.MODIFY_PLAN, result);
    }

    @Test
    void classify_RandomText_ShouldReturnUnknown() {
        ConversationIntent result = classifier.classify("今天天气不错");

        assertEquals(ConversationIntent.UNKNOWN, result);
    }
}
