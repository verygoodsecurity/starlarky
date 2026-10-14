/*
 * Copyright 2026 Very Good Security Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verygood.security.larky.wasm;

import com.verygood.security.larky.wasm.WasmRuntime.WasmException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.SandboxPolicy;

/**
 * Reuses an UNTRUSTED isolate engine only within one host-supplied trust domain. Every validation
 * and run still gets a fresh context; an idle engine is closed, not reassigned, on eviction.
 */
final class GraalPooledIsolateWasmRuntime implements WasmRuntime {
  private static final Pool POOL = new Pool(GraalIsolateSettings.configured(),
      positiveProperty("larky.wasm.isolate.pool.maxEngines", 2),
      positiveProperty("larky.wasm.isolate.pool.maxActiveContexts", 4),
      positiveProperty("larky.wasm.isolate.pool.maxActiveContextsPerTenant", 2));

  private final String trustDomain;

  static Engine engineForTests(String trustDomain) throws WasmException {
    try (Lease lease = POOL.acquire(trustDomain)) {
      return lease.entry.engine;
    }
  }

  GraalPooledIsolateWasmRuntime() throws WasmException {
    trustDomain = WasmTrustDomain.require();
  }

  @Override
  public String name() {
    return "graal-isolate-pooled";
  }

  @Override
  public String compilePolicyKey() {
    // WasmModule's process-wide program cache must never return another tenant's Program.
    return name() + ":" + trustDomain + ":" + POOL.settings + ":" + POOL.hostLimits;
  }

  @Override
  public Program compile(byte[] wasm) throws WasmException {
    requireSameDomain();
    if (wasm.length > POOL.hostLimits.maxModuleBytes()) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "module is " + wasm.length + " bytes; the limit is "
              + POOL.hostLimits.maxModuleBytes());
    }
    // Reject malformed or unsupported modules before a cold tenant allocates an isolate engine.
    EndivePreflight.check(wasm, POOL.hostLimits);
    WasmBinaries.checkWasiCommand(wasm, POOL.hostLimits);
    byte[] retained = wasm.clone();
    try (Lease lease = POOL.acquire(trustDomain)) {
      try {
        Program compiled = lease.entry.runtime.compile(retained);
        return new PooledProgram(trustDomain, retained, lease.entry, compiled);
      } catch (WasmException e) {
        if (e.kind() == WasmException.Kind.MEMORY_LIMIT || e.kind() == WasmException.Kind.TIMEOUT) {
          POOL.retire(lease.entry);
        }
        throw e;
      } catch (RuntimeException | Error e) {
        POOL.retire(lease.entry);
        throw e;
      }
    }
  }

  private void requireSameDomain() throws WasmException {
    if (!trustDomain.equals(WasmTrustDomain.require())) {
      throw new WasmException(WasmException.Kind.INVALID_MODULE,
          "a WASM module cannot cross trust domains");
    }
  }

  private static int positiveProperty(String name, int fallback) {
    String raw = System.getProperty(name);
    if (raw == null) return fallback;
    try {
      int value = Integer.parseInt(raw);
      if (value > 0) return value;
    } catch (NumberFormatException ignored) {
      // Report one consistent configuration error below.
    }
    throw new IllegalArgumentException(name + " must be a positive integer");
  }

  private static final class PooledProgram implements Program {
    private final String trustDomain;
    private final byte[] wasm;
    private Entry compiledEntry;
    private Program compiled;

    PooledProgram(String trustDomain, byte[] wasm, Entry entry, Program compiled) {
      this.trustDomain = trustDomain;
      this.wasm = wasm;
      this.compiledEntry = entry;
      this.compiled = compiled;
    }

    @Override
    public Result run(byte[] stdin, Limits limits) throws WasmException, InterruptedException {
      if (!trustDomain.equals(WasmTrustDomain.require())) {
        throw new WasmException(WasmException.Kind.INVALID_MODULE,
            "a WASM module cannot cross trust domains");
      }
      try (Lease lease = POOL.acquire(trustDomain)) {
        try {
          Program current;
          synchronized (this) {
            if (compiledEntry != lease.entry) {
              compiled = lease.entry.runtime.compile(wasm);
              compiledEntry = lease.entry;
            }
            current = compiled;
          }
          return current.run(stdin, limits);
        } catch (WasmException e) {
          if (e.kind() == WasmException.Kind.MEMORY_LIMIT || e.kind() == WasmException.Kind.TIMEOUT) {
            POOL.retire(lease.entry);
          }
          throw e;
        } catch (InterruptedException | RuntimeException | Error e) {
          POOL.retire(lease.entry);
          throw e;
        }
      }
    }
  }

  private static final class Entry {
    final String trustDomain;
    final Engine engine;
    final EngineRuntime runtime;
    int active;
    boolean retired;

    Entry(String trustDomain, GraalIsolateSettings settings, WasmHostLimits hostLimits) {
      this.trustDomain = trustDomain;
      Engine built = Engine.newBuilder("wasm")
          .sandbox(SandboxPolicy.UNTRUSTED)
          .spawnIsolate(true)
          .out(OutputStream.nullOutputStream())
          .err(OutputStream.nullOutputStream())
          .option("engine.MaxIsolateMemory", settings.maxIsolateMemoryBytes() + "B")
          .build();
      try {
        runtime = new EngineRuntime(settings, hostLimits, built);
      } catch (RuntimeException | Error e) {
        built.close();
        throw e;
      }
      engine = built;
    }
  }

  private static final class EngineRuntime extends GraalWasmRuntime {
    private final Engine pooledEngine;

    EngineRuntime(GraalIsolateSettings settings, WasmHostLimits hostLimits, Engine pooledEngine) {
      super(settings, false, hostLimits);
      this.pooledEngine = pooledEngine;
    }

    @Override
    Context.Builder newContextBuilder(long cpuMillis, int outputBytes) {
      return isolatedContextBuilder(pooledEngine, cpuMillis, outputBytes);
    }
  }

  private static final class Lease implements AutoCloseable {
    private final Pool pool;
    final Entry entry;
    private boolean closed;

    Lease(Pool pool, Entry entry) {
      this.pool = pool;
      this.entry = entry;
    }

    @Override
    public void close() {
      if (closed) return;
      closed = true;
      pool.release(entry);
    }
  }

  private static final class Pool {
    final GraalIsolateSettings settings;
    final WasmHostLimits hostLimits;
    private final int maxEngines;
    private final int maxActiveContexts;
    private final int maxActiveContextsPerTenant;
    // Access order makes the first idle entry the least recently used one.
    private final Map<String, Entry> entries = new LinkedHashMap<>(8, 0.75f, true);
    private final Set<String> creating = new HashSet<>();
    private int activeContexts;

    Pool(GraalIsolateSettings settings, int maxEngines, int maxActiveContexts,
         int maxActiveContextsPerTenant) {
      this.settings = settings;
      this.hostLimits = WasmHostLimits.configured();
      this.maxEngines = maxEngines;
      this.maxActiveContexts = maxActiveContexts;
      this.maxActiveContextsPerTenant = maxActiveContextsPerTenant;
    }

    Lease acquire(String trustDomain) throws WasmException {
      synchronized (this) {
        if (activeContexts >= maxActiveContexts) {
          throw exhausted("too many concurrent WASM contexts");
        }
        Entry existing = entries.get(trustDomain);
        if (existing != null && existing.retired) {
          if (existing.active != 0) throw exhausted("WASM isolate is retiring");
          entries.remove(trustDomain);
          existing.engine.close();
          existing = null;
        }
        if (existing != null) {
          if (existing.active >= maxActiveContextsPerTenant) {
            throw exhausted("too many concurrent WASM contexts for one trust domain");
          }
          existing.active++;
          activeContexts++;
          return new Lease(this, existing);
        }
        if (creating.contains(trustDomain)) {
          throw exhausted("WASM isolate is initializing");
        }
        if (entries.size() + creating.size() >= maxEngines) evictIdle();
        if (entries.size() + creating.size() >= maxEngines) {
          throw exhausted("all WASM isolate engines are active");
        }
        // Reserve both slots while the expensive isolate creation runs outside the pool lock.
        creating.add(trustDomain);
        activeContexts++;
      }

      Entry created;
      try {
        created = new Entry(trustDomain, settings, hostLimits);
      } catch (RuntimeException | Error e) {
        synchronized (this) {
          creating.remove(trustDomain);
          activeContexts--;
        }
        if (e instanceof Error error) throw error;
        throw new WasmException(WasmException.Kind.INVALID_MODULE,
            "the pooled GraalWasm isolate is unavailable", e);
      }
      synchronized (this) {
        creating.remove(trustDomain);
        created.active = 1;
        entries.put(trustDomain, created);
        return new Lease(this, created);
      }
    }

    private void evictIdle() {
      Iterator<Entry> it = entries.values().iterator();
      while (it.hasNext()) {
        Entry candidate = it.next();
        if (candidate.active == 0) {
          it.remove();
          candidate.engine.close();
          return;
        }
      }
    }

    synchronized void retire(Entry entry) {
      entry.retired = true;
    }

    synchronized void release(Entry entry) {
      entry.active--;
      activeContexts--;
      if (entry.retired && entry.active == 0) {
        entries.remove(entry.trustDomain, entry);
        entry.engine.close();
      }
    }

    private static WasmException exhausted(String message) {
      return new WasmException(WasmException.Kind.CAPACITY_LIMIT, message);
    }
  }
}
