package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.aus.LarkyAccountUpdater;
import com.verygood.security.larky.modules.vgs.aus.MockAccountUpdaterService;
import com.verygood.security.larky.modules.vgs.aus.NoopAccountUpdaterService;
import com.verygood.security.larky.modules.vgs.aus.spi.AccountUpdaterService;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.*;

import java.util.List;
import java.util.ServiceLoader;

@StarlarkBuiltin(
    name = "native_au",
    category = "BUILTIN",
    doc = "Overridable Account Updater API in Larky")
public class AccountUpdaterModule implements LarkyAccountUpdater {
  public static final AccountUpdaterModule INSTANCE = new AccountUpdaterModule();

  public static final String ENABLE_MOCK_PROPERTY = "larky.modules.vgs.nts.enableMockAccountUpdater";

  private final AccountUpdaterService AccountUpdaterService;

  public AccountUpdaterModule() {
    ServiceLoader<AccountUpdaterService> loader = ServiceLoader.load(AccountUpdaterService.class);
    List<AccountUpdaterService> AccountUpdaterProviders = ImmutableList.copyOf(loader.iterator());

    if (Boolean.getBoolean(ENABLE_MOCK_PROPERTY)) {
      AccountUpdaterService = new MockAccountUpdaterService();
    } else if (AccountUpdaterProviders.isEmpty()) {
      AccountUpdaterService = new NoopAccountUpdaterService();
    } else {
      if (AccountUpdaterProviders.size() != 1) {
        throw new IllegalArgumentException(
            String.format(
                "AccountUpdaterModule expecting only 1 network token provider of type AccountUpdaterService, found %d",
                AccountUpdaterProviders.size()));
      }
      AccountUpdaterService = AccountUpdaterProviders.get(0);
    }
  }

  @StarlarkMethod(
      name = "get_card",
      doc = "Retrieves a newer information for the provided card.",
      useStarlarkThread = true,
      parameters = {
        @Param(
            name = "number",
            named = true,
            doc = "Card PAN. Used to look up the corresponding card information to be returned",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "exp_month",
            named = true,
            doc =
                "Card expiration month. Used to pass to the network for retrieving the corresponding card information",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "exp_year",
            named = true,
            doc =
                "Card expiration year. Used to pass to the network for retrieving the corresponding card information",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "name",
            named = true,
            doc =
                "Card owner name. Used to pass to the network for retrieving the corresponding card information",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "vgs_merchant_id",
            named = true,
            doc = "VGS merchant id to get a network token for",
            defaultValue = "''",
            allowedTypes = {@ParamType(type = String.class)}),
      })
  @Override
  public Dict<String, Object> getCard(
      String number,
      Integer expireMonth,
      Integer expireYear,
      String name,
      String vgsMerchantId,
      StarlarkThread thread)
      throws EvalException {
    if (number.trim().isEmpty()) {
      throw Starlark.errorf("card number argument cannot be blank");
    }
    final AccountUpdaterService.Card card;
    try {
      card =
          AccountUpdaterService.getCard(number, expireMonth, expireYear, name, vgsMerchantId);
    } catch (UnsupportedOperationException exception) {
      throw Starlark.errorf("au.get_card operation must be overridden");
    }
    return Dict.<String, Object>builder()
        .put("number", card.getNumber())
        .put("expireMonth", StarlarkInt.of(card.getExpireMonth()))
        .put("expireYear", StarlarkInt.of(card.getExpireYear()))
        .put("name", card.getName())
        .build(thread.mutability());
  }
}
