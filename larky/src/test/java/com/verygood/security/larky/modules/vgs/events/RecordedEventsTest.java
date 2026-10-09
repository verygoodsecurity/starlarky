package com.verygood.security.larky.modules.vgs.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RecordedEventsTest {

  @Test
  void validEventAccepted() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("payment", Map.of("k", "v"));
    assertEquals(0, rec.dropped());
    assertEquals(1, rec.events().size());
    assertEquals("payment", rec.events().get(0).name());
    assertEquals("v", rec.events().get(0).fields().get("k"));
  }

  @Test
  void capAtTenEvents() {
    RecordedEvents rec = new RecordedEvents();
    for (int i = 0; i < 11; i++) {
      rec.tryRecord("evt", Map.of());
    }
    assertEquals(10, rec.events().size());
    assertEquals(1, rec.dropped());
  }

  @Test
  void dottedCardNumberDropped() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("evt", Map.of("pan", "4111.1111.1111.1111"));
    assertEquals(0, rec.events().size());
    assertEquals(1, rec.dropped());
  }

  @Test
  void nineDigitsInOneValueDropped() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("evt", Map.of("n", "123456789"));
    assertEquals(0, rec.events().size());
    assertEquals(1, rec.dropped());
  }

  @Test
  void twelveDigitsInOneEventDropped() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("evt", Map.of("a", "41111111", "b", "1111"));
    assertEquals(0, rec.events().size());
    assertEquals(1, rec.dropped());
  }

  @Test
  void twoEventsOfEightDigitsSecondDropped() {
    RecordedEvents rec = new RecordedEvents();
    Map<String, String> eight = Map.of("a", "12345678");
    rec.tryRecord("e1", eight);
    rec.tryRecord("e2", eight);
    assertEquals(1, rec.events().size());
    assertEquals(1, rec.dropped());
  }

  @Test
  void exactlyElevenDigitsAcrossTwoEventsAccepted() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("e1", Map.of("a", "123456"));
    rec.tryRecord("e2", Map.of("a", "12345"));
    assertEquals(2, rec.events().size());
    assertEquals(0, rec.dropped());
  }

  @Test
  void twelfthDigitAcrossEventsDropped() {
    RecordedEvents rec = new RecordedEvents();
    Map<String, String> six = Map.of("a", "123456");
    rec.tryRecord("e1", six);
    rec.tryRecord("e2", six);
    assertEquals(1, rec.events().size());
    assertEquals(1, rec.dropped());
  }

  @Test
  void valueExactly64CharsAccepted() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("evt", Map.of("msg", "x".repeat(64)));
    assertEquals(1, rec.events().size());
    assertEquals(0, rec.dropped());
  }

  @Test
  void valueOver64CharsDropped() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("evt", Map.of("msg", "x".repeat(65)));
    assertEquals(0, rec.events().size());
    assertEquals(1, rec.dropped());
  }

  @Test
  void eightAndThreeDigitsAccepted() {
    RecordedEvents rec = new RecordedEvents();
    rec.tryRecord("evt", Map.of("a", "12345678", "b", "123"));
    assertEquals(1, rec.events().size());
    assertEquals(0, rec.dropped());
  }
}
