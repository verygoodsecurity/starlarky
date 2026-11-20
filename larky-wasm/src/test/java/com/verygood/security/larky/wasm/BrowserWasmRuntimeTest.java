/*
 * Copyright 2026 Very Good Security Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.verygood.security.larky.wasm;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import run.endive.wabt.Wat2Wasm;

public class BrowserWasmRuntimeTest {
  private static final URI ALLOWED = URI.create("https://checkout.example");
  private static final URI DENIED = URI.create("https://outside.example");
  private static final URI RECEIPT = URI.create("https://checkout.example/receipt?id=demo");

  @Test
  public void ordinaryWasmCannotImportBrowserHttp() {
    assertThrows(WasmRuntime.WasmException.class,
        () -> new EndiveWasmRuntime().compile(fetchGuest(ALLOWED)));
  }

  @Test
  public void theGetOnlyGrantDeniesBrowserPostBeforeTransport() throws Exception {
    var urls = new ArrayList<URI>();
    var result = fetch(BrowserWasmRuntime.compile(postGuest(RECEIPT, 512, 7)), session(urls));
    assertThat(result.exitCode()).isEqualTo(100);
    assertThat(urls).isEmpty();
  }

  @Test
  public void browserPostSendsTheBodyOnlyToAnExactGrantedUrl() throws Exception {
    var posts = new ArrayList<PostedRequest>();
    var grant = postSession(new ArrayList<>(), posts);
    for (URI denied : List.of(URI.create("https://checkout.example/receipt"),
        URI.create("https://checkout.example/receipt?id=other"),
        URI.create("https://checkout.example/receipt?id=demo#fragment"),
        URI.create("https://outside.example/receipt?id=demo"))) {
      assertThat(fetch(BrowserWasmRuntime.compile(postGuest(denied, 512, 7)), grant).exitCode())
          .isEqualTo(100);
    }
    assertThat(posts).isEmpty();
    assertThat(fetch(BrowserWasmRuntime.compile(postGuest(RECEIPT, 512, 7)), grant).exitCode())
        .isEqualTo(2);
    assertThat(posts).containsExactly(new PostedRequest(RECEIPT, "receipt", 256));
  }

  @Test
  public void browserPostRejectsAnOutOfBoundsBodyBeforeTransport() throws Exception {
    var posts = new ArrayList<PostedRequest>();
    var grant = postSession(new ArrayList<>(), posts);
    for (byte[] wasm : new byte[][] {
        postGuest(RECEIPT, -1, 7), postGuest(RECEIPT, 512, -1),
        postGuest(RECEIPT, 65530, 7)}) {
      assertThat(fetch(BrowserWasmRuntime.compile(wasm), grant).exitCode()).isEqualTo(100);
    }
    assertThat(posts).isEmpty();
  }

  @Test
  public void allowedGetAndPostShareTheRequestBudget() throws Exception {
    var urls = new ArrayList<URI>();
    var posts = new ArrayList<PostedRequest>();
    var grant = postSession(urls, posts);
    assertThat(fetch(BrowserWasmRuntime.compile(fetchGuest(ALLOWED)), grant).exitCode())
        .isEqualTo(2);
    assertThat(fetch(BrowserWasmRuntime.compile(postGuest(RECEIPT, 512, 7)), grant).exitCode())
        .isEqualTo(100);
    assertThat(urls).containsExactly(ALLOWED);
    assertThat(posts).isEmpty();
  }

  @Test
  public void browserFetchUsesTheInvocationGrant() throws Exception {
    var urls = new ArrayList<URI>();
    var session = session(urls);
    var result = fetch(BrowserWasmRuntime.compile(fetchGuest(ALLOWED)), session);
    assertThat(result.exitCode()).isEqualTo(2);
    assertThat(urls).containsExactly(ALLOWED);
  }

  @Test
  public void deniedOriginNeverReachesTheHostTransport() throws Exception {
    for (URI destination : List.of(DENIED, URI.create("https://checkout.example:444/"),
        URI.create("https://outside.example@checkout.example/"))) {
      var urls = new ArrayList<URI>();
      var result = fetch(BrowserWasmRuntime.compile(fetchGuest(destination)), session(urls));
      assertThat(result.exitCode()).isEqualTo(100);
      assertThat(urls).isEmpty();
    }
  }

  @Test
  public void requestBudgetIsSharedByAllRunsUsingTheGrant() throws Exception {
    var urls = new ArrayList<URI>();
    var session = session(urls);
    var guest = BrowserWasmRuntime.compile(fetchGuest(ALLOWED));
    assertThat(fetch(guest, session).exitCode()).isEqualTo(2);
    assertThat(fetch(guest, session).exitCode()).isEqualTo(100);
    assertThat(urls).containsExactly(ALLOWED);
  }

  @Test
  public void guestStatePersistsAcrossStepsAndStartsFreshInAnotherSession() throws Exception {
    var program = BrowserWasmRuntime.compile(Wat2Wasm.parse("""
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func $write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)
          (global $count (mut i32) (i32.const 0))
          (func (export "_initialize")
            (i32.store (i32.const 0) (i32.const 16))
            (i32.store (i32.const 4) (i32.const 1)))
          (func (export "vgs_browser_step")
            (global.set $count (i32.add (global.get $count) (i32.const 1)))
            (i32.store8 (i32.const 16) (i32.add (global.get $count) (i32.const 48)))
            (drop (call $write (i32.const 1) (i32.const 0) (i32.const 1) (i32.const 8)))))
        """));
    try (var first = program.open(limits(), session(new ArrayList<>()));
         var second = program.open(limits(), session(new ArrayList<>()))) {
      assertThat(stepOutput(first)).isEqualTo("1");
      assertThat(stepOutput(first)).isEqualTo("2");
      assertThat(stepOutput(second)).isEqualTo("1");
    }
  }

  @Test
  public void aTrappedGuestCannotBeUsedAgain() throws Exception {
    var program = BrowserWasmRuntime.compile(Wat2Wasm.parse("""
        (module
          (memory (export "memory") 1)
          (func (export "_initialize"))
          (func (export "vgs_browser_step") unreachable))
        """));
    try (var guest = program.open(limits(), session(new ArrayList<>()))) {
      var failure = assertThrows(WasmRuntime.WasmException.class, () -> guest.step(new byte[0]));
      assertThat(failure.kind()).isEqualTo(WasmRuntime.WasmException.Kind.TRAP);
      assertThrows(IllegalStateException.class, () -> guest.step(new byte[0]));
    }
  }

  private static String stepOutput(BrowserWasmRuntime.Reactor guest) throws Exception {
    return new String(guest.step(new byte[0]).stdout(), StandardCharsets.UTF_8);
  }

  private static WasmRuntime.Result fetch(BrowserWasmRuntime.Program program,
      BrowserWasmRuntime.Session session) throws Exception {
    try (var guest = program.open(limits(), session)) {
      return guest.step(new byte[0]);
    }
  }

  private static BrowserWasmRuntime.Session session(List<URI> urls) {
    return new BrowserWasmRuntime.Session(Set.of(ALLOWED), 1, (uri, deadline, maxBytes) -> {
      urls.add(uri);
      return "ok".getBytes(StandardCharsets.UTF_8);
    });
  }

  private record PostedRequest(URI url, String body, int maxBytes) {}

  private static BrowserWasmRuntime.Session postSession(List<URI> urls, List<PostedRequest> posts) {
    return new BrowserWasmRuntime.Session(Set.of(ALLOWED), 1, (uri, deadline, maxBytes) -> {
      urls.add(uri);
      return "ok".getBytes(StandardCharsets.UTF_8);
    }, Set.of(RECEIPT), (uri, body, deadline, maxBytes) -> {
      posts.add(new PostedRequest(uri, new String(body, StandardCharsets.UTF_8), maxBytes));
      return "ok".getBytes(StandardCharsets.UTF_8);
    });
  }

  private static WasmRuntime.Limits limits() {
    return new WasmRuntime.Limits(1 << 20, System.currentTimeMillis() + 10_000, 1024, null);
  }

  private static byte[] fetchGuest(URI url) {
    return Wat2Wasm.parse("""
        (module
          (import "vgs_browser_v1" "http_get" (func $get (param i32 i32 i32 i32) (result i32)))
          (import "wasi_snapshot_preview1" "proc_exit" (func $exit (param i32)))
          (memory (export "memory") 1)
          (data (i32.const 0) "%s")
          (func (export "_initialize"))
          (func (export "vgs_browser_step") (local $result i32)
            (local.set $result (call $get (i32.const 0) (i32.const %d) (i32.const 500) (i32.const 256)))
            (if (i32.lt_s (local.get $result) (i32.const 0))
              (then (call $exit (i32.const 100))))
            (if (i32.ne (i32.load8_u (i32.const 500)) (i32.const 111))
              (then (call $exit (i32.const 101))))
            (call $exit (local.get $result))))
        """.formatted(url, url.toString().length()));
  }

  private static byte[] postGuest(URI url, int bodyAddress, int bodyLength) {
    return Wat2Wasm.parse("""
        (module
          (import "vgs_browser_v1" "http_post"
            (func $post (param i32 i32 i32 i32 i32 i32) (result i32)))
          (import "wasi_snapshot_preview1" "proc_exit" (func $exit (param i32)))
          (memory (export "memory") 1)
          (data (i32.const 0) "%s")
          (data (i32.const 512) "receipt")
          (func (export "_initialize"))
          (func (export "vgs_browser_step") (local $result i32)
            (local.set $result (call $post (i32.const 0) (i32.const %d)
              (i32.const %d) (i32.const %d) (i32.const 1024) (i32.const 256)))
            (if (i32.lt_s (local.get $result) (i32.const 0))
              (then (call $exit (i32.const 100))))
            (if (i32.ne (i32.load8_u (i32.const 1024)) (i32.const 111))
              (then (call $exit (i32.const 101))))
            (call $exit (local.get $result))))
        """.formatted(url, url.toString().length(), bodyAddress, bodyLength));
  }
}
