package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.aus.LarkyAccountUpdater;
import com.verygood.security.larky.modules.vgs.aus.MockAccountUpdaterService;
import com.verygood.security.larky.modules.vgs.aus.NoopAccountUpdaterService;
import com.verygood.security.larky.modules.vgs.aus.spi.AccountUpdaterService;
import java.util.List;
import java.util.ServiceLoader;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.*;

@StarlarkBuiltin(
    name = "native_au",
    category = "BUILTIN",
    doc = "Overridable Account Updater API in Larky")
public class AccountUpdaterModule implements LarkyAccountUpdater {
  public static final AccountUpdaterModule INSTANCE = new AccountUpdaterModule();

  public static final String ENABLE_MOCK_PROPERTY =
      "larky.modules.vgs.nts.enableMockAccountUpdater";

  private final AccountUpdaterService AccountUpdaterService;

  public AccountUpdaterModule() {
    final ServiceLoader<AccountUpdaterService> loader =
        ServiceLoader.load(AccountUpdaterService.class);
    final List<AccountUpdaterService> AccountUpdaterProviders =
        ImmutableList.copyOf(loader.iterator());

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
      name = "lookup_card",
      doc = "Lookup account updates for a given PAN.",
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
            allowedTypes = {@ParamType(type = StarlarkInt.class)}),
        @Param(
            name = "exp_year",
            named = true,
            doc =
                "Card expiration year. Used to pass to the network for retrieving the corresponding card information",
            allowedTypes = {@ParamType(type = StarlarkInt.class)}),
        @Param(
            name = "name",
            named = true,
            doc =
                "Card owner name. Used to pass to the network for retrieving the corresponding card information",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "client_id",
            named = true,
            doc = "Client ID of service account to access calm API server",
            allowedTypes = {@ParamType(type = String.class)}),
        @Param(
            name = "client_secret",
            named = true,
            doc = "Client secret of service account to access calm API server",
            allowedTypes = {@ParamType(type = String.class)}),
      })
  @Override
  public Dict<String, Object> lookupCard(
      String number,
      StarlarkInt expireMonth,
      StarlarkInt expireYear,
      String name,
      String clientId,
      String clientSecret,
      StarlarkThread thread)
      throws EvalException {
    if (number.trim().isEmpty()) {
      throw Starlark.errorf("card number argument cannot be blank");
    }
    final AccountUpdaterService.Card card;
    try {
      card =
          AccountUpdaterService.lookupCard(
              number,
              expireMonth.toInt("invalid int range"),
              expireYear.toInt("invalid int range"),
              name,
              clientId,
              clientSecret);
    } catch (UnsupportedOperationException exception) {
      throw Starlark.errorf("au.lookup_updates operation must be overridden");
    }
    return Dict.<String, Object>builder()
        .put("number", card.getNumber())
        .put("expireMonth", StarlarkInt.of(card.getExpireMonth()))
        .put("expireYear", StarlarkInt.of(card.getExpireYear()))
        .put("name", card.getName())
        .build(thread.mutability());
  }
}
