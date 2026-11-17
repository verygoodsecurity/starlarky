package com.verygood.security.larky.modules.vgs.nts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService.GetNetworkTokenRequest;
import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService.NetworkToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class MockNetworkTokenServiceTest {

  private static final String DEFAULT_MERCHANT = "MC8SWErAVLuooPFYz9WTx5W1";
  private static final String OTHER_MERCHANT = "MCdAhTydCJMZEzxgqhvVdkgo";

  private final MockNetworkTokenService service = new MockNetworkTokenService();

  private static GetNetworkTokenRequest request(
      String panAlias, String merchantId, String cryptogramType, String transactionType) {
    return GetNetworkTokenRequest.builder()
        .panAlias(panAlias)
        .merchantId(merchantId)
        .cryptogramType(cryptogramType)
        .transactionType(transactionType)
        .build();
  }

  private static String expectedValue(String cryptogramType) {
    return "DTVV".equals(cryptogramType) ? "MOCK_DYNAMIC_CVV" : "MOCK_CRYPTOGRAM_VALUE";
  }

  @Test
  public void notFoundPanAliasIsEmpty() {
    assertFalse(service.getNetworkToken(request("NOT_FOUND", OTHER_MERCHANT, "DTVV", null)).isPresent());
    assertFalse(service.getNetworkToken(request("NOT_FOUND", null, "DTVV", "AFT")).isPresent());
  }

  @Test
  public void unknownMerchantIsEmpty() {
    assertFalse(service.getNetworkToken(request("alias", "unknown", "DTVV", null)).isPresent());
  }

  @Test
  public void blankMerchantUsesDefault() {
    for (String blank : new String[] {null, "", "   "}) {
      NetworkToken t = service.getNetworkToken(request("alias", blank, "DTVV", null)).get();
      assertEquals("4242424242424242", t.getToken());
      assertEquals(Integer.valueOf(12), t.getExpireMonth());
      assertEquals(Integer.valueOf(27), t.getExpireYear());
      assertEquals("MOCK_CRYPTOGRAM_ECI", t.getCryptogramEci());
      assertEquals("MOCK_DYNAMIC_CVV", t.getCryptogramValue());
      assertEquals("DTVV", t.getCryptogramType());
    }
  }

  @Test
  public void knownMerchants() {
    NetworkToken t = service.getNetworkToken(request("alias", OTHER_MERCHANT, "CRYPTOGRAM", null)).get();
    assertEquals("4111111111111111", t.getToken());
    assertEquals(Integer.valueOf(10), t.getExpireMonth());
    assertEquals(Integer.valueOf(27), t.getExpireYear());
    assertEquals("MOCK_CRYPTOGRAM_VALUE", t.getCryptogramValue());
    assertEquals("CRYPTOGRAM", t.getCryptogramType());

    t = service.getNetworkToken(request("alias", DEFAULT_MERCHANT, "DTVV", null)).get();
    assertEquals("4242424242424242", t.getToken());
    assertEquals("MOCK_DYNAMIC_CVV", t.getCryptogramValue());
  }

  @Test
  public void aftIgnoresMerchant() {
    for (String merchant : new String[] {null, "unknown", OTHER_MERCHANT}) {
      NetworkToken t = service.getNetworkToken(request("alias", merchant, "DTVV", "AFT")).get();
      assertEquals("5555555555554444", t.getToken());
      assertEquals(Integer.valueOf(12), t.getExpireMonth());
      assertEquals(Integer.valueOf(27), t.getExpireYear());
      assertEquals("MOCK_DYNAMIC_CVV", t.getCryptogramValue());
      assertEquals("DTVV", t.getCryptogramType());
    }
    NetworkToken t = service.getNetworkToken(request("alias", null, "OTHER", "AFT")).get();
    assertEquals("MOCK_CRYPTOGRAM_VALUE", t.getCryptogramValue());
    assertEquals("OTHER", t.getCryptogramType());
  }

  @Test
  public void concurrentRequestsDoNotShareCryptogramState() throws Exception {
    final int threads = 16;
    final int perThread = 20_000;
    final String[] merchants = {null, DEFAULT_MERCHANT, OTHER_MERCHANT};
    final CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Future<String>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        final int id = i;
        futures.add(
            pool.submit(
                (Callable<String>)
                    () -> {
                      start.await();
                      for (int n = 0; n < perThread; n++) {
                        // alternate type per request, offset per thread, so neighbours differ
                        String type = ((n + id) % 2 == 0) ? "DTVV" : (n % 4 < 2 ? "CRYPTOGRAM" : "OTHER");
                        String merchant = merchants[(n + id) % merchants.length];
                        boolean aft = n % 5 == 0;
                        Optional<NetworkToken> result =
                            service.getNetworkToken(
                                request("alias", merchant, type, aft ? "AFT" : null));
                        NetworkToken t = result.get();
                        if (!type.equals(t.getCryptogramType())
                            || !expectedValue(type).equals(t.getCryptogramValue())) {
                          return "thread "
                              + id
                              + " request "
                              + n
                              + ": asked "
                              + type
                              + " got type="
                              + t.getCryptogramType()
                              + " value="
                              + t.getCryptogramValue();
                        }
                      }
                      return null;
                    }));
      }
      start.countDown();
      for (Future<String> f : futures) {
        String failure = f.get(120, TimeUnit.SECONDS);
        assertTrue(failure, failure == null);
      }
    } finally {
      pool.shutdownNow();
    }
  }
}
