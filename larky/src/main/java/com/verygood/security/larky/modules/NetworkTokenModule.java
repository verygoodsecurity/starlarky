package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.nts.LarkyNetworkToken;
import com.verygood.security.larky.modules.vgs.nts.MockNetworkTokenService;
import com.verygood.security.larky.modules.vgs.nts.NoopNetworkTokenService;
import com.verygood.security.larky.modules.vgs.nts.spi.NetworkTokenService;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import javax.annotation.Nullable;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.Dict;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;

@StarlarkBuiltin(
    name = "native_nts",
    category = "BUILTIN",
    doc = "Overridable Network Token API in Larky")
public class NetworkTokenModule implements LarkyNetworkToken {
  public static final NetworkTokenModule INSTANCE = new NetworkTokenModule();

  public static final String ENABLE_MOCK_PROPERTY = "larky.modules.vgs.nts.enableMockNetworkToken";

  private final NetworkTokenService networkTokenService;

  public NetworkTokenModule() {
    ServiceLoader<NetworkTokenService> loader = ServiceLoader.load(NetworkTokenService.class);
    List<NetworkTokenService> networkTokenProviders = ImmutableList.copyOf(loader.iterator());

    if (Boolean.getBoolean(ENABLE_MOCK_PROPERTY)) {
      networkTokenService = new MockNetworkTokenService();
    } else if (networkTokenProviders.isEmpty()) {
      networkTokenService = new NoopNetworkTokenService();
    } else {
      if (networkTokenProviders.size() != 1) {
        throw new IllegalArgumentException(
            String.format(
                "NetworkTokenModule expecting only 1 network token provider of type NetworkTokenService, found %d",
                networkTokenProviders.size()));
      }
      networkTokenService = networkTokenProviders.get(0);
    }
  }

  @StarlarkMethod(
      name = "get_network_token",
      doc = "Retrieves a network token for the given PAN alias.",
      useStarlarkThread = true,
      parameters = {
        @Param(
            name = "pan",
            named = true,
            doc = "PAN alias. Used to look up the corresponding network token to be returned",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "cvv",
            named = true,
            doc =
                "CVV of the credit card. Used to pass to the network for retrieving the corresponding network token "
                    + "and cryptogram to be returned",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "amount",
            named = true,
            doc =
                "The amount of payment for the transaction to be made with the network token. Used to pass to the "
                    + "network for retrieving the corresponding network token and cryptogram to be returned",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "currency_code",
            named = true,
            doc =
                "The currency code of payment amount for the transaction to be made with the network token. Used to "
                    + "pass to the network for retrieving the corresponding network token and cryptogram to be "
                    + "returned",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "cryptogram_type",
            named = true,
            doc = "Type of cryptogram to get for the network token",
            defaultValue = "'TAVV'",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "vgs_merchant_id",
            named = true,
            doc = "VGS merchant id to get a network token for",
            defaultValue = "''",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "transaction_type",
            named = true,
            defaultValue = "None",
            doc = "Type of payment transaction for requesting cryptogram",
            allowedTypes = {@ParamType(type = NoneType.class), @ParamType(type = String.class)}),
      })
  @Override
  public Dict<String, Object> getNetworkToken(
      String pan,
      String cvv,
      String amount,
      String currencyCode,
      String cryptogramType,
      String vgsMerchantId,
      @Nullable Object transactionType,
      StarlarkThread thread)
      throws EvalException {
    if (pan.trim().isEmpty()) {
      throw Starlark.errorf("pan argument cannot be blank");
    }
    final Optional<NetworkTokenService.NetworkToken> networkTokenOptional;
    try {
      networkTokenOptional =
          networkTokenService.getNetworkTokenV2(
              NetworkTokenService.GetNetworkTokenRequest.builder()
                  .panAlias(pan)
                  .cvv(cvv)
                  .amount(amount)
                  .currencyCode(currencyCode)
                  .cryptogramType(cryptogramType)
                  .merchantId(vgsMerchantId)
                  .transactionType(
                      transactionType instanceof String ? (String) transactionType : null)
                  .build());
    } catch (UnsupportedOperationException exception) {
      throw Starlark.errorf("nts.get_network_token operation must be overridden");
    }
    if (!networkTokenOptional.isPresent()) {
      throw Starlark.errorf("network token is not found");
    }
    final NetworkTokenService.NetworkToken networkToken = networkTokenOptional.get();
    return Dict.<String, Object>builder()
        .put("token", networkToken.getToken())
        .put("exp_month", StarlarkInt.of(networkToken.getExpireMonth()))
        .put("exp_year", StarlarkInt.of(networkToken.getExpireYear()))
        .put("cryptogram_value", networkToken.getCryptogramValue())
        .put("cryptogram_eci", networkToken.getCryptogramEci())
        .put("cryptogram_type", networkToken.getCryptogramType())
        .build(thread.mutability());
  }
}
