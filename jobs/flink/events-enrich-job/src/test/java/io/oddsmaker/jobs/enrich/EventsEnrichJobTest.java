package io.oddsmaker.jobs.enrich;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventsEnrichJobTest {

    @Test
    void fieldReturnsNullForMissingField() {
        // 创建一个模拟的GenericRecord
        var record = new org.apache.avro.generic.GenericData.Record(
            org.apache.avro.Schema.createRecord("TestRecord", null, null, false)
        );
        
        Object result = EventsEnrichJob.field(record, "nonexistent");
        assertNull(result);
    }

    @Test
    void strReturnsEmptyStringForNull() {
        String result = EventsEnrichJob.str(null);
        assertEquals("", result);
    }

    @Test
    void strReturnsStringForNonNull() {
        String result = EventsEnrichJob.str("test");
        assertEquals("test", result);
    }

    @Test
    void longOrNullReturnsNullForNonNumeric() {
        Long result = EventsEnrichJob.longOrNull("not_a_number");
        assertNull(result);
    }

    @Test
    void longOrNullReturnsLongForNumeric() {
        Long result = EventsEnrichJob.longOrNull(12345L);
        assertEquals(12345L, result);
    }

    @Test
    void longOrNullReturnsLongForStringNumber() {
        Long result = EventsEnrichJob.longOrNull("12345");
        assertEquals(12345L, result);
    }

    @Test
    void doubleOrNullReturnsNullForNonNumeric() {
        Double result = EventsEnrichJob.doubleOrNull("not_a_number");
        assertNull(result);
    }

    @Test
    void doubleOrNullReturnsDoubleForNumeric() {
        Double result = EventsEnrichJob.doubleOrNull(123.45);
        assertEquals(123.45, result);
    }

    @Test
    void doubleOrNullReturnsDoubleForStringNumber() {
        Double result = EventsEnrichJob.doubleOrNull("123.45");
        assertEquals(123.45, result);
    }

    @Test
    void toDlqJsonReturnsValidJson() {
        var record = new org.apache.avro.generic.GenericData.Record(
            org.apache.avro.Schema.createRecord("TestRecord", null, null, false)
        );
        record.put("event_id", "test_event_123");
        record.put("game_id", "game_demo");
        record.put("environment", "prod");
        
        String json = EventsEnrichJob.toDlqJson(record, "invalid_schema");
        
        assertNotNull(json);
        assertTrue(json.contains("test_event_123"));
        assertTrue(json.contains("game_demo"));
        assertTrue(json.contains("prod"));
        assertTrue(json.contains("invalid_schema"));
    }
}