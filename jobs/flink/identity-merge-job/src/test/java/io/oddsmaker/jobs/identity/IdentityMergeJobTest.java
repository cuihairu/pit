package io.oddsmaker.jobs.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IdentityMergeJob 序列化测试。
 * 验证 toJson 输出对齐 control IdentityEventDto 契约（snake_case + 数组字段）。
 */
@DisplayName("IdentityMergeJob 序列化测试")
class IdentityMergeJobTest {

    private final ObjectMapper om = new ObjectMapper();

    private static IdentityMergeJob.IdentityRecord record(String identityId,
                                                          LinkedHashSet<String> devices,
                                                          LinkedHashSet<String> players,
                                                          LinkedHashSet<String> chars) {
        IdentityMergeJob.IdentityRecord r = new IdentityMergeJob.IdentityRecord();
        r.gameId = "game_demo";
        r.environment = "prod";
        r.identityId = identityId;
        r.userId = "user_1";
        r.playerId = players.isEmpty() ? "" : players.iterator().next();
        r.playerIds = new LinkedHashSet<>(players);
        r.deviceIds = new LinkedHashSet<>(devices);
        r.characterIds = new LinkedHashSet<>(chars);
        r.firstSeen = new Timestamp(1730000000000L);
        r.lastSeen = new Timestamp(1730000100000L);
        return r;
    }

    @Test
    @DisplayName("jsonArray：空集→[]、单元素、多元素保序")
    void jsonArray_cases() {
        assertEquals("[]", IdentityMergeJob.jsonArray(null));
        assertEquals("[]", IdentityMergeJob.jsonArray(new LinkedHashSet<>()));
        assertEquals("[\"d1\"]", IdentityMergeJob.jsonArray(new LinkedHashSet<>(Arrays.asList("d1"))));
        assertEquals("[\"d1\",\"d2\"]", IdentityMergeJob.jsonArray(new LinkedHashSet<>(Arrays.asList("d1", "d2"))));
    }

    @Test
    @DisplayName("toJson：输出可被 ObjectMapper 解析，字段契约对齐 control IdentityEventDto")
    void toJson_parsableAndContractAligned() throws Exception {
        String id = "idt_" + "a".repeat(28);  // 32 字符
        IdentityMergeJob.IdentityRecord r = record(id,
                new LinkedHashSet<>(Arrays.asList("dev_a", "dev_b")),
                new LinkedHashSet<>(Arrays.asList("player_1")),
                new LinkedHashSet<>(Arrays.asList("char_1")));

        Map<?, ?> parsed = om.readValue(IdentityMergeJob.toJson(r), Map.class);

        assertEquals("game_demo", parsed.get("game_id"));
        assertEquals("prod", parsed.get("environment"));
        assertEquals(id, parsed.get("identity_id"));
        assertEquals("user_1", parsed.get("user_id"));
        assertEquals("player_1", parsed.get("player_id"));
        assertEquals(Arrays.asList("dev_a", "dev_b"), parsed.get("device_ids"));
        assertEquals(Arrays.asList("player_1"), parsed.get("player_ids"));
        assertEquals(Arrays.asList("char_1"), parsed.get("character_ids"));
        assertEquals(1730000000000L, parsed.get("first_seen"));
        assertEquals(1730000100000L, parsed.get("last_seen"));
    }

    @Test
    @DisplayName("toJson：转义双引号和反斜杠（能被正确解析即证明转义无误）")
    void toJson_escapesSpecialChars() throws Exception {
        IdentityMergeJob.IdentityRecord r = record("idt_" + "0".repeat(28),
                new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
        r.gameId = "game\"x\\y";
        Map<?, ?> parsed = om.readValue(IdentityMergeJob.toJson(r), Map.class);
        assertEquals("game\"x\\y", parsed.get("game_id"));
        assertTrue(((java.util.List<?>) parsed.get("device_ids")).isEmpty());
    }
}
