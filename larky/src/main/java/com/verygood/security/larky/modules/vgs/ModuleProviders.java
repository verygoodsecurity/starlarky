package com.verygood.security.larky.modules.vgs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.verygood.security.larky.modules.vgs.agentgateway.spi.AgentGatewaySession;
import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import java.util.Optional;
import javax.annotation.Nullable;
import net.starlark.java.eval.Printer;
import net.starlark.java.eval.StarlarkSemantics;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;

/**
 * Providers a host gives one run of a script for the {@code @vgs//} modules. A {@code @vgs//} module finds its
 * provider with {@link java.util.ServiceLoader} once per process, as before; a provider given here takes its place for
 * this run only:
 *
 * <ul>
 *   <li>{@link LarkyVault}: what {@code load("@vgs//vault", "vault")} calls in this run.</li>
 *   <li>{@link AgentGatewaySession}: the {@code session} of {@code load("@vgs//agent_gateway", "session")} in this
 *   run.</li>
 * </ul>
 *
 * <p>The run's {@code StarlarkThread} carries them, so they reach exactly the calls of that run, also when a script
 * loaded once is called many times: give them with {@code LarkyLoadedScript.invoke} or the bindings of an evaluation,
 * under {@link com.verygood.security.larky.parser.LarkyEvaluator#MODULE_PROVIDERS}.
 */
public final class ModuleProviders implements StarlarkValue {

  public static final ModuleProviders NONE = new ModuleProviders(ImmutableMap.of());

  /** The provider types the {@code @vgs//} modules look up per run. */
  public static final ImmutableSet<Class<?>> TYPES = ImmutableSet.of(LarkyVault.class, AgentGatewaySession.class);

  private final ImmutableMap<Class<?>, Object> providers;

  private ModuleProviders(ImmutableMap<Class<?>, Object> providers) {
    this.providers = providers;
  }

  /** No providers: every module uses its own. */
  public static ModuleProviders of() {
    return NONE;
  }

  /**
   * These providers, and {@code provider} for {@code type}.
   *
   * @throws IllegalArgumentException if {@code type} is not one of {@link #TYPES}
   */
  public <T> ModuleProviders with(Class<T> type, T provider) {
    if (!TYPES.contains(type)) {
      throw new IllegalArgumentException("no @vgs// module takes a provider of type " + type.getName() + "; types: "
          + TYPES.stream().map(Class::getSimpleName).toList());
    }
    if (provider == null) {
      throw new IllegalArgumentException("no provider given for " + type.getSimpleName());
    }
    final ImmutableMap.Builder<Class<?>, Object> builder = ImmutableMap.builder();
    providers.forEach((key, value) -> {
      if (key != type) {
        builder.put(key, value);
      }
    });
    return new ModuleProviders(builder.put(type, provider).buildOrThrow());
  }

  /**
   * The providers a host gave under {@link com.verygood.security.larky.parser.LarkyEvaluator#MODULE_PROVIDERS}: none
   * for {@code null}.
   *
   * @throws IllegalArgumentException if {@code value} is anything else than providers
   */
  public static ModuleProviders given(@Nullable Object value) {
    if (value == null) {
      return NONE;
    }
    if (value instanceof ModuleProviders providers) {
      return providers;
    }
    throw new IllegalArgumentException(com.verygood.security.larky.parser.LarkyEvaluator.MODULE_PROVIDERS
        + " must be a " + ModuleProviders.class.getName() + ", not " + value.getClass().getName());
  }

  /** The provider given for {@code type}, if any. */
  public <T> Optional<T> get(Class<T> type) {
    return Optional.ofNullable(type.cast(providers.get(type)));
  }

  public boolean isEmpty() {
    return providers.isEmpty();
  }

  /** Gives these providers to the run of {@code thread}. */
  public void installOn(StarlarkThread thread) {
    if (!providers.isEmpty()) {
      thread.setThreadLocal(ModuleProviders.class, this);
    }
  }

  /** The provider of {@code type} given to the run of {@code thread}, if any. */
  public static <T> Optional<T> forRun(@Nullable StarlarkThread thread, Class<T> type) {
    final ModuleProviders given = thread == null ? null : thread.getThreadLocal(ModuleProviders.class);
    return given == null ? Optional.empty() : given.get(type);
  }

  @Override
  public boolean isImmutable() {
    return true;
  }

  @Override
  public void repr(Printer printer, StarlarkSemantics semantics) {
    printer.append("<module providers>");
  }
}
