// Copyright 2025 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.starlark.java.eval;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import net.starlark.java.eval.compiler.FunctionDescriptor;
import net.starlark.java.syntax.Identifier;
import net.starlark.java.syntax.Resolver;
import net.starlark.java.syntax.StarlarkType;
import net.starlark.java.syntax.TokenKind;
import net.starlark.java.syntax.TypeTable;
import net.starlark.java.syntax.Types;

/**
 * The semantics of the bytecode instructions that do more than move values, shared by the VMs
 * ({@link AbstractBytecodeVM}), so that they behave exactly like each other and like the
 * tree-walker.
 *
 * <p>Operands arrive as arguments (in stack order: deepest first) and results are returned.
 */
public final class BcOps {

  private BcOps() {}

  static final TokenKind[] TOKEN_KINDS = TokenKind.values();

  /** Wraps a loaded module with its name, for LOAD_ATTR's "does not contain symbol" error. */
  static final class ModuleWithName {
    final Module module;
    final String moduleName;

    ModuleWithName(Module module, String moduleName) {
      this.module = module;
      this.moduleName = moduleName;
    }
  }

  // ---- limits ----

  /** Checks the step limit, thread interrupt and expiry, as Eval does per statement and loop. */
  public static void checkpoint(StarlarkThread thread) throws EvalException, InterruptedException {
    thread.checkInterrupt();
    if (thread.steps >= thread.stepLimit) {
      throw new EvalException("Starlark computation cancelled: too many steps");
    }
    thread.checkExpired();
  }

  /** Counts {@code n} executed instructions (compiled code adds a basic block at a time). */
  public static void addSteps(StarlarkThread thread, long n) {
    thread.steps += n;
  }

  /** Records the frame's current instruction as the error location, as the tree-walker does. */
  public static EvalException withLocation(BcFrame f, EvalException ex) {
    StarlarkThread thread = f.thread();
    if (!thread.getCallStack().isEmpty()) {
      thread.frame(0).setErrorLocation(f.currentLocation());
    }
    return ex.ensureStack(thread);
  }

  // ---- variables ----

  public static Object loadBytes(BcFrame f, Object bytes) {
    return StarlarkBytes.wrap(f.thread().mutability(), (byte[]) bytes);
  }

  public static Object checkLocal(BcFrame f, Object value, int index) throws EvalException {
    if (value == null) {
      String varName = "?";
      if (index < f.chunk().getLocalNames().size()) {
        varName = f.chunk().getLocalNames().get(index);
      }
      throw Starlark.errorf("local variable '%s' is referenced before assignment.", varName);
    }
    return value;
  }

  public static Object loadGlobal(BcFrame f, String name) throws EvalException {
    Object value = f.globals().get(name);
    if (value == null) {
      throw Starlark.errorf("global variable '%s' is referenced before assignment.", name);
    }
    return value;
  }

  public static Object loadBuiltin(BcFrame f, String name) throws EvalException {
    return BytecodeGlobals.lookupBuiltin(f.globals(), name);
  }

  public static void storeGlobal(BcFrame f, String name, Object value) {
    f.globals().put(name, value);
  }

  public static Object loadFree(BcFrame f, int index) {
    return ((BytecodeFunction.Cell) f.freevars().get(index)).x;
  }

  public static void storeFree(BcFrame f, int index, Object value) {
    ((BytecodeFunction.Cell) f.freevars().get(index)).x = value;
  }

  public static Object loadCell(Object cell) {
    return ((BytecodeFunction.Cell) cell).x;
  }

  public static void storeCell(Object cell, Object value) {
    ((BytecodeFunction.Cell) cell).x = value;
  }

  // ---- operators ----

  public static Object binary(BcFrame f, int op, Object x, Object y) throws EvalException {
    return EvalUtils.binaryOp(TOKEN_KINDS[op], x, y, f.thread());
  }

  public static Object inplace(BcFrame f, int op, Object x, Object y) throws EvalException {
    return Eval.inplaceBinaryOp(f.thread(), TOKEN_KINDS[op], x, y);
  }

  public static Object unary(int op, Object x) throws EvalException {
    return EvalUtils.unaryOp(TOKEN_KINDS[op], x);
  }

  public static boolean truth(Object x) {
    return Starlark.truth(x);
  }

  public static Object and(Object a, Object b) {
    return Starlark.truth(a) && Starlark.truth(b);
  }

  public static Object or(Object a, Object b) {
    return Starlark.truth(a) || Starlark.truth(b);
  }

  public static Object not(Object a) {
    return !Starlark.truth(a);
  }

  // ---- collections ----

  public static Object buildList(BcFrame f, Object[] elements) {
    return StarlarkList.wrap(f.thread().mutability(), elements);
  }

  public static Object buildTuple(Object[] elements) {
    return Tuple.wrap(elements);
  }

  /** Builds a dict from key, value, key, value... in source order, rejecting duplicate keys. */
  public static Object buildDict(BcFrame f, Object[] pairs) throws EvalException {
    Dict<Object, Object> dict = Dict.of(f.thread().mutability());
    for (int i = 0; i < pairs.length; i += 2) {
      Object key = pairs[i];
      int before = dict.size();
      dict.putEntry(key, pairs[i + 1]);
      if (dict.size() == before) {
        throw Starlark.errorf(
            "dictionary expression has duplicate key: %s",
            Starlark.repr(key, f.thread().getSemantics()));
      }
    }
    return dict;
  }

  /** Checks {@code x} for an n-element sequence assignment, as Eval.assignSequence does. */
  public static Object[] unpack(Object x, int count) throws EvalException {
    int n = Starlark.len(x);
    if (n < 0 || x instanceof String) { // strings are not iterable
      throw Starlark.errorf(
          "got '%s' in sequence assignment (want %d-element sequence)", Starlark.type(x), count);
    }
    if (n != count) {
      throw Starlark.errorf(
          "too %s values to unpack (got %d, want %d)", n < count ? "few" : "many", n, count);
    }
    return Iterables.toArray(Starlark.toIterable(x), Object.class);
  }

  public static Object index(BcFrame f, Object object, Object key) throws EvalException {
    return EvalUtils.index(f.thread(), object, key);
  }

  public static void setIndex(BcFrame f, Object value, Object object, Object key)
      throws EvalException {
    EvalUtils.setIndex(f.thread(), object, key, value);
  }

  public static Object slice(BcFrame f, Object object, Object start, Object stop, Object step)
      throws EvalException {
    return Starlark.slice(f.thread().mutability(), object, start, stop, step);
  }

  public static void listAppend(Object list, Object value) throws EvalException {
    @SuppressWarnings("unchecked")
    StarlarkList<Object> l = (StarlarkList<Object>) list;
    l.addElement(value);
  }

  public static void dictAdd(Object dict, Object key, Object value) throws EvalException {
    @SuppressWarnings("unchecked")
    Dict<Object, Object> d = (Dict<Object, Object>) dict;
    d.putEntry(key, value);
  }

  // ---- attributes ----

  public static Object getattr(BcFrame f, Object object, String name)
      throws EvalException, InterruptedException {
    if (object instanceof ModuleWithName mwn) {
      Object value = mwn.module.getGlobal(name);
      if (value == null) {
        throw Starlark.errorf("file '%s' does not contain symbol '%s'", mwn.moduleName, name);
      }
      return value;
    }
    StarlarkThread thread = f.thread();
    return Starlark.getattr(thread.mutability(), thread.getSemantics(), object, name, null);
  }

  public static void setattr(Object value, Object object, String name) throws EvalException {
    EvalUtils.setField(object, name, value);
  }

  // ---- calls ----

  /**
   * Calls {@code function} with already-evaluated arguments ({@code named} holds name/value pairs),
   * the way {@code Eval.evalCall} does: positional-only calls go through {@link
   * Starlark#positionalOnlyCall}, all others through the callee's {@link
   * StarlarkCallable.ArgumentProcessor}.
   */
  public static Object call(BcFrame f, Object function, Object[] positional, Object[] named)
      throws EvalException, InterruptedException {
    StarlarkThread thread = f.thread();
    checkpoint(thread);
    thread.frame(0).setLocation(f.currentLocation());
    StarlarkCallable callable = Starlark.getStarlarkCallable(thread, function);
    if (named.length == 0) {
      return Starlark.positionalOnlyCall(thread, callable, positional);
    }
    StarlarkCallable.ArgumentProcessor argumentProcessor =
        Starlark.requestArgumentProcessor(thread, callable);
    for (Object value : positional) {
      argumentProcessor.addPositionalArg(value);
    }
    for (int i = 0; i < named.length; i += 2) {
      argumentProcessor.addNamedArg((String) named[i], named[i + 1]);
    }
    return Starlark.callViaArgumentProcessor(thread, callable, argumentProcessor);
  }

  /** CALL with a trailing {@code **kwargs} value: its entries follow the explicit keywords. */
  public static Object callStarStar(
      BcFrame f, Object function, Object[] positional, Object[] named, Object starStar)
      throws EvalException, InterruptedException {
    return call(f, function, positional, appendStarStar(named, starStar));
  }

  /** CALL_EX: {@code f(*args)} / {@code f(**kwargs)} forms, built by the compiler as a list/dict. */
  public static Object callEx(
      BcFrame f,
      Object function,
      Object posList,
      Object kwDict,
      Object starArg,
      Object starStarArg)
      throws EvalException, InterruptedException {
    ArrayList<Object> positional = new ArrayList<>();
    Iterables.addAll(positional, (Iterable<?>) posList);
    if (starArg != null) {
      if (!(starArg instanceof StarlarkIterable)) {
        throw Starlark.errorf(
            "argument after * must be an iterable, not %s", Starlark.type(starArg));
      }
      Iterables.addAll(positional, (Iterable<?>) starArg);
    }
    Dict<?, ?> kw = (Dict<?, ?>) kwDict;
    Object[] named = new Object[2 * kw.size()];
    int j = 0;
    for (Map.Entry<?, ?> e : kw.entrySet()) {
      named[j++] = e.getKey();
      named[j++] = e.getValue();
    }
    if (starStarArg != null) {
      named = appendStarStar(named, starStarArg);
    }
    return call(f, function, positional.toArray(), named);
  }

  static Object[] appendStarStar(Object[] named, Object value) throws EvalException {
    if (!(value instanceof Dict)) {
      throw Starlark.errorf("argument after ** must be a dict, not %s", Starlark.type(value));
    }
    Dict<?, ?> kwargs = (Dict<?, ?>) value;
    int j = named.length;
    named = Arrays.copyOf(named, j + 2 * kwargs.size());
    for (Map.Entry<?, ?> e : kwargs.entrySet()) {
      if (!(e.getKey() instanceof String)) {
        throw Starlark.errorf("keywords must be strings, not %s", Starlark.type(e.getKey()));
      }
      named[j++] = e.getKey();
      named[j++] = e.getValue();
    }
    return named;
  }

  // ---- iteration ----

  /** GET_ITER: starts iterating {@code iterable} and takes its iteration lock. */
  public static Object getIter(BcFrame f, Object iterable) throws EvalException {
    if (iterable instanceof String) {
      throw new EvalException("type 'string' is not iterable");
    }
    Iterator<?> iterator = Starlark.toIterable(iterable).iterator();
    EvalUtils.addIterator(iterable);
    f.iterators().put(iterator, iterable);
    return iterator;
  }

  /** FOR_ITER's test: a checkpoint, then whether the iterator has another element. */
  public static boolean hasNext(BcFrame f, Object iterator)
      throws EvalException, InterruptedException {
    checkpoint(f.thread());
    return ((Iterator<?>) iterator).hasNext();
  }

  public static Object next(Object iterator) {
    return ((Iterator<?>) iterator).next();
  }

  /** END_FOR: releases the iteration lock of a loop that completed. */
  public static void endFor(BcFrame f, Object iterator) {
    Object iterable = f.iterators().remove(iterator);
    if (iterable != null) {
      EvalUtils.removeIterator(iterable);
    }
  }

  /** Releases the iteration locks of loops left by return or by an exception. */
  public static void releaseIterators(BcFrame f) {
    Map<Iterator<?>, Object> iterators = f.iterators();
    if (!iterators.isEmpty()) {
      for (Object iterable : iterators.values()) {
        EvalUtils.removeIterator(iterable);
      }
      iterators.clear();
    }
  }

  // ---- definitions and modules ----

  /** Mirrors Eval.execStatements' post-assign hook for Bazel's "export" semantics. */
  public static void postAssign(BcFrame f, String name, Object value) throws EvalException {
    StarlarkThread thread = f.thread();
    if (thread.postAssignHook != null) {
      if (value instanceof StarlarkFunction func) {
        func.export(thread, name);
      } else if (value instanceof BytecodeFunction func) {
        func.export(thread, name);
      } else {
        thread.postAssignHook.assign(name, f.currentLocation(), value);
      }
    }
  }

  /** TYPE_ALIAS: binds the alias to its statically computed type constructor (Eval.execTypeAlias). */
  public static void typeAlias(BcFrame f, Object identifierObj) {
    TypeTable typeTable = BytecodeGlobals.typeTableOf(f.globals());
    if (typeTable == null) {
      return;
    }
    Identifier id = (Identifier) identifierObj;
    Resolver.Binding binding = id.getBinding();
    Object value = TypeConstructorValue.of(typeTable.getTypeConstructor(binding));
    switch (binding.getScope()) {
      case LOCAL -> f.storeLocal(binding.getIndex(), value);
      case CELL -> storeCell(f.getLocal(binding.getIndex()), value);
      case GLOBAL -> storeGlobal(f, id.getName(), value);
      default -> throw new IllegalStateException(binding.getScope().toString());
    }
  }

  public static Object makeFunction(BcFrame f, Object descriptorObj, Object[] defaults)
      throws EvalException {
    FunctionDescriptor descriptor = (FunctionDescriptor) descriptorObj;
    StarlarkThread thread = f.thread();
    Types.CallableType functionType = null;
    TypeTable typeTable = BytecodeGlobals.typeTableOf(f.globals());
    if (typeTable != null && descriptor.getResolvedFunction() != null) {
      functionType = typeTable.getType(descriptor.getResolvedFunction());
      if (functionType != null
          && thread
              .getSemantics()
              .getBool(StarlarkSemantics.EXPERIMENTAL_STARLARK_DYNAMIC_TYPE_CHECKING)) {
        checkDefaultTypes(thread, descriptor, functionType, defaults);
      }
    }
    BytecodeFunction function =
        new BytecodeFunction(
            descriptor.getName(),
            descriptor.getLocation(),
            descriptor.getChunk(),
            descriptor.getParameterNames(),
            descriptor.hasVarargs(),
            descriptor.hasKwargs(),
            descriptor.getNumKeywordOnlyParams(),
            Tuple.wrap(defaults),
            descriptor.getLocalCount(),
            f.filename());
    function.setToken(thread.getNextIdentityToken());
    function.setGlobals(f.globals());
    function.setFunctionType(functionType);
    function.setCellIndices(descriptor.getCellIndices());

    // Capture free variables for closures.
    ImmutableList<FunctionDescriptor.FreevarInfo> freevarInfos = descriptor.getFreevarInfos();
    if (!freevarInfos.isEmpty()) {
      Object[] capturedCells = new Object[freevarInfos.size()];
      for (int i = 0; i < freevarInfos.size(); i++) {
        FunctionDescriptor.FreevarInfo info = freevarInfos.get(i);
        if (info.isFromEnclosingFreevars) {
          capturedCells[i] = f.freevars().get(info.index);
        } else {
          Object local = f.getLocal(info.index);
          if (local instanceof BytecodeFunction.Cell) {
            capturedCells[i] = local;
          } else {
            // Wrap the value in a cell, and update the local so later accesses see the cell.
            capturedCells[i] = new BytecodeFunction.Cell(local);
            f.storeLocal(info.index, capturedCells[i]);
          }
        }
      }
      function.setFreevars(Tuple.wrap(capturedCells));
    }
    return function;
  }

  /** Checks default values against their declared parameter types, as Eval.newFunction does. */
  private static void checkDefaultTypes(
      StarlarkThread thread,
      FunctionDescriptor descriptor,
      Types.CallableType functionType,
      Object[] defaults)
      throws EvalException {
    int nparams =
        descriptor.getParameterNames().size()
            - (descriptor.hasKwargs() ? 1 : 0)
            - (descriptor.hasVarargs() ? 1 : 0);
    int first = nparams - defaults.length; // index of the parameter of defaults[0]
    for (int j = 0; j < defaults.length; j++) {
      Object defaultValue = defaults[j];
      if (defaultValue == BytecodeFunction.MANDATORY) {
        continue;
      }
      StarlarkType parameterType = functionType.getParameterTypeByPos(first + j);
      if (!TypeChecker.isValueSubtypeOf(
          defaultValue, parameterType, thread.getSemantics(), thread.getTypeContext())) {
        throw Starlark.errorf(
            "%s(): parameter '%s' has default value of type '%s', declares '%s'",
            descriptor.getName(),
            descriptor.getParameterNames().get(first + j),
            Starlark.getStarlarkType(defaultValue, thread.getSemantics()),
            parameterType);
      }
    }
  }

  public static Object loadModule(BcFrame f, String moduleName) throws EvalException {
    StarlarkThread.Loader loader = f.thread().getLoader();
    if (loader == null) {
      throw Starlark.errorf("load statements may not be executed in this thread");
    }
    Module module = loader.load(moduleName);
    if (module == null) {
      throw Starlark.errorf("module '%s' not found", moduleName);
    }
    return new ModuleWithName(module, moduleName);
  }
}
