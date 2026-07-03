package io.oddsmaker.jobs.dimension;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DimensionSyncJobTest {

    @Test
    void parsePropsSupportsResourceDimensionEvent() {
        String json = """
                {
                  "dim_type": "resource",
                  "item_code": "gold_coin",
                  "display_name": "Gold Coin",
                  "quality": "common",
                  "updated_at": "2026-06-30T10:00:00Z"
                }
                """;

        DimensionSyncJob.DimRecord rec = DimensionSyncJob.parseProps("game_demo", "production", json);

        assertNotNull(rec);
        assertEquals("game_demo", rec.gameId);
        assertEquals("prod", rec.environment);
        assertEquals("item", rec.dimType);
        assertEquals("gold_coin", rec.id);
        assertEquals("Gold Coin", rec.attributes.get("name"));
        assertEquals("common", rec.attributes.get("rarity"));
        assertTrue(rec.isCurrent);
    }

    @Test
    void parseDebeziumUsesAfterForUpsert() {
        String json = """
                {
                  "payload": {
                    "op": "u",
                    "ts_ms": 1782813600000,
                    "source": {
                      "table": "items"
                    },
                    "after": {
                      "item_code": "sword_001",
                      "display_name": "Iron Sword",
                      "quality": "rare"
                    }
                  }
                }
                """;

        DimensionSyncJob.DimRecord rec = DimensionSyncJob.parseDebezium(json, "game_demo", "prod", "item");

        assertNotNull(rec);
        assertEquals("game_demo", rec.gameId);
        assertEquals("prod", rec.environment);
        assertEquals("item", rec.dimType);
        assertEquals("sword_001", rec.id);
        assertEquals("Iron Sword", rec.attributes.get("name"));
        assertEquals("rare", rec.attributes.get("rarity"));
        assertTrue(rec.isCurrent);
    }

    @Test
    void parseDebeziumUsesBeforeForDelete() {
        String json = """
                {
                  "payload": {
                    "op": "d",
                    "ts_ms": 1782813600000,
                    "source": {
                      "table": "levels",
                      "game_id": "game_demo",
                      "environment": "staging"
                    },
                    "before": {
                      "level_id": "level_10",
                      "level_name": "Frozen Gate",
                      "level_difficulty": "hard"
                    }
                  }
                }
                """;

        DimensionSyncJob.DimRecord rec = DimensionSyncJob.parseDebezium(json, "", "", "");

        assertNotNull(rec);
        assertEquals("game_demo", rec.gameId);
        assertEquals("staging", rec.environment);
        assertEquals("level", rec.dimType);
        assertEquals("level_10", rec.id);
        assertEquals("Frozen Gate", rec.attributes.get("name"));
        assertEquals("hard", rec.attributes.get("difficulty"));
        assertFalse(rec.isCurrent);
    }
}
