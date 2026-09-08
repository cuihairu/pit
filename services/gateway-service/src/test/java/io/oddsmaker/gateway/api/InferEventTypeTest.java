package io.oddsmaker.gateway.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 事件类型推断测试：P3 九类 session/user/business/resource/progression/design/error/ad/risk。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("事件类型推断测试")
class InferEventTypeTest {

    @InjectMocks
    private BatchController controller;

    @Test
    void infersNineCategories() {
        assertEquals("session", invoke("session_start"));
        assertEquals("user", invoke("user_login"));
        assertEquals("user", invoke("register"));
        assertEquals("user", invoke("auth_token_refresh"));
        assertEquals("business", invoke("purchase_complete"));
        assertEquals("resource", invoke("currency_source"));
        assertEquals("resource", invoke("item_consume"));
        assertEquals("progression", invoke("level_complete"));
        assertEquals("progression", invoke("quest_accept"));
        assertEquals("design", invoke("design_tutorial_step"));
        assertEquals("error", invoke("crash_report"));
        assertEquals("ad", invoke("ad_impression"));
        assertEquals("risk", invoke("fraud_signal"));
    }

    @Test
    void nullAndUnknownFallbackToBusiness() {
        assertEquals("business", invoke(null));
        assertEquals("business", invoke("some_random_event"));
    }

    @Test
    void typeKeywordPriority() {
        // risk 优先级最高（安全语义优先）
        assertEquals("risk", invoke("ad_risk_probe"));
        // ad 前缀优先于其他弱关键词
        assertEquals("ad", invoke("ad_level_boost"));
    }

    private String invoke(String eventName) {
        try {
            var method = BatchController.class.getDeclaredMethod("inferEventType", String.class);
            method.setAccessible(true);
            return (String) method.invoke(controller, eventName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
