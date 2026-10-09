package com.verygood.security.larky.modules.vgs.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RecordedEventsTest {

  @Test
  void validEventAccepted() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("payment", java.util.Map.of("k", "v"));
    assertEquals(0, rec.dropped());
    assertEquals(1, rec.events().size());
    assertEquals("payment", rec.events().get(0).name());
    assertEquals("v", rec.events().get(0).fields().get("k"));
  }

  @Test
  void capAtTenEvents() {
    RecordedEvents rec = new RecordedEvents();
    for (int i = 0; i < 11; i++) {
      rec.tryRecord("evt", java.util.Map.of());
    }
    assertEquals(10, rec.events().size());
    assertEquals(1, rec.dropped());
  }
}
