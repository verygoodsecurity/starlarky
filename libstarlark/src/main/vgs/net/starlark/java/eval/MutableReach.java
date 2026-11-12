package net.starlark.java.eval;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Finds a value that can still change among the values a Starlark value keeps reachable.
 *
 * <p>{@link Starlark#isImmutable} cannot answer that for a value many evaluations share. It is
 * shallow for a frozen list, dict or set, which may hold a Java object with state of its own (a
 * hash being updated, say), and it is true for every function, which keeps its default values, the
 * cells of the functions it is nested in, and the globals of the module that defined it. This
 * walks all of those.
 *
 * <p>A value is taken as unable to change if it is a string, number, bool or None; a tuple, or a
 * frozen list, dict or set, all of whose elements are; a boxed Java number or character; a function whose defaults, closure cells and
 * module globals are; a built-in function whose receiver is; a structure whose {@link
 * StarlarkValue#isImmutable} is true and whose fields are; or another Starlark value whose {@link
 * StarlarkValue#isImmutable} is true. Anything else, including a Java object that is not a
 * Starlark value, is reported.
 */
public final class MutableReach {

  private MutableReach() {}

  /** Where a value was found, for the report. */
  private record Item(String path, Object value) {}

  /**
   * Returns where the first value that can change was found, starting from the named values in
   * {@code roots}, or empty if there is none.
   *
   * @param roots the values to start from, by the name the report gives each
   * @param trusted values not looked into, such as values every evaluation shares anyway
   * @param skippedModule modules whose globals are not looked into, such as modules every evaluation
   *     shares anyway, or the caller's own module, whose globals it passes as {@code roots}
   * @return a description such as {@code STATE[0] (Hash)}
   */
  public static Optional<String> find(
      Map<String, Object> roots, Predicate<Object> trusted, Predicate<Module> skippedModule) {
    final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
    final Set<Module> modules = Collections.newSetFromMap(new IdentityHashMap<>());
    final Deque<Item> pending = new ArrayDeque<>();
    roots.forEach((name, value) -> pending.add(new Item(name, value)));
    while (!pending.isEmpty()) {
      final Item item = pending.removeFirst();
      final Object value = item.value();
      if (value == null || !visited.add(value) || trusted.test(value)) {
        continue;
      }
      final String path = item.path();
      switch (value) {
        case String s -> {}
        case Boolean b -> {}
        case StarlarkInt i -> {}
        case StarlarkFloat f -> {}
        case NoneType n -> {}
        // Java's own boxed values, such as the step count Larky keeps among a module's globals
        case Long l -> {}
        case Integer i -> {}
        case Short s -> {}
        case Byte b -> {}
        case Double d -> {}
        case Float f -> {}
        case Character c -> {}
        case Tuple tuple -> addElements(pending, path, tuple);
        case StarlarkList<?> list -> {
          if (!list.isImmutable()) {
            return Optional.of(report(path, value));
          }
          addElements(pending, path, list);
        }
        case StarlarkSet<?> set -> {
          if (!set.isImmutable()) {
            return Optional.of(report(path, value));
          }
          for (Object element : set) {
            pending.add(new Item(path + " element " + Starlark.repr(element, StarlarkSemantics.DEFAULT), element));
          }
        }
        case Dict<?, ?> dict -> {
          if (!dict.isImmutable()) {
            return Optional.of(report(path, value));
          }
          for (Map.Entry<?, ?> entry : dict.entrySet()) {
            final String key = Starlark.repr(entry.getKey(), StarlarkSemantics.DEFAULT);
            pending.add(new Item(path + " key " + key, entry.getKey()));
            pending.add(new Item(path + "[" + key + "]", entry.getValue()));
          }
        }
        case StarlarkFunction function -> addFunction(pending, path, function, modules, skippedModule);
        case BuiltinFunction builtin ->
            pending.add(new Item(path + "'s receiver", builtin.obj));
        case Structure structure -> {
          if (!structure.isImmutable()) {
            return Optional.of(report(path, value));
          }
          for (String field : structure.getFieldNames()) {
            final Object fieldValue;
            try {
              fieldValue = structure.getValue(field);
            } catch (EvalException e) {
              return Optional.of(report(path + "." + field, value));
            }
            pending.add(new Item(path + "." + field, fieldValue));
          }
        }
        case StarlarkValue starlarkValue -> {
          if (!starlarkValue.isImmutable()) {
            return Optional.of(report(path, value));
          }
        }
        default -> {
          return Optional.of(report(path, value));
        }
      }
    }
    return Optional.empty();
  }

  private static void addElements(Deque<Item> pending, String path, Iterable<?> elements) {
    int index = 0;
    for (Object element : elements) {
      pending.add(new Item(path + "[" + index++ + "]", element));
    }
  }

  private static void addFunction(
      Deque<Item> pending,
      String path,
      StarlarkFunction function,
      Set<Module> modules,
      Predicate<Module> skippedModule) {
    final var parameters = function.getParameterNames();
    for (int i = 0; i < parameters.size(); i++) {
      final Object defaultValue = function.getDefaultValue(i);
      if (defaultValue != null) {
        pending.add(new Item(path + "'s default for " + parameters.get(i), defaultValue));
      }
    }
    for (int i = 0; i < function.getNumFreeVars(); i++) {
      pending.add(new Item("a value " + path + " closes over", function.getFreeVar(i).x));
    }
    final Module module = function.getModule();
    if (modules.add(module) && !skippedModule.test(module)) {
      final String label = String.valueOf(module.getClientData());
      module.getGlobals().forEach(
          (name, global) -> pending.add(new Item(path + " -> " + label + ": " + name, global)));
    }
  }

  private static String report(String path, Object value) {
    return path + " (" + typeOf(value) + ")";
  }

  private static String typeOf(Object value) {
    return value instanceof StarlarkValue ? Starlark.type(value) : value.getClass().getName();
  }
}
