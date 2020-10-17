// Copyright 2018 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.remote.worker.http;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A simple HTTP REST in-memory cache used during testing the LRE. */
@Sharable
public class HttpCacheServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private static final Pattern URI_PATTERN = Pattern.compile("^/?(.*/)?(ac/|cas/)([a-f0-9]{64})$");

  private final ConcurrentMap<String, byte[]> cache;

  @VisibleForTesting
  public HttpCacheServerHandler(ConcurrentMap<String, byte[]> cache) {
    this.cache = Preconditions.checkNotNull(cache);
  }

  HttpCacheServerHandler() {
    this(new ConcurrentHashMap<>());
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
    if (!request.decoderResult().isSuccess()) {
      sendError(ctx, request, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    if (request.method().equals(HttpMethod.GET)) {
      handleGet(ctx, request);
    } else if (request.method().equals(HttpMethod.PUT)) {
      handlePut(ctx, request);
    } else {
      sendError(ctx, request, HttpResponseStatus.METHOD_NOT_ALLOWED);
    }
  }

  @VisibleForTesting
  static boolean isUriValid(String uri) {
    Matcher matcher = URI_PATTERN.matcher(uri);
    return matcher.matches();
  }

  private void handleGet(ChannelHandlerContext ctx, FullHttpRequest request) {
    if (!isUriValid(request.uri())) {
      sendError(ctx, request, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    byte[] contents = cache.get(request.uri());

    if (contents == null) {
      sendError(ctx, request, HttpResponseStatus.NOT_FOUND);
      return;
    }

    FullHttpResponse response =
        new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(contents));
    HttpUtil.setContentLength(response, contents.length);
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
    ChannelFuture lastContentFuture = ctx.writeAndFlush(response);

    if (!HttpUtil.isKeepAlive(request)) {
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void handlePut(ChannelHandlerContext ctx, FullHttpRequest request) {
    if (!request.decoderResult().isSuccess()) {
      sendError(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR);
      return;
    }
    if (!isUriValid(request.uri())) {
      sendError(ctx, request, HttpResponseStatus.BAD_REQUEST);
      return;
    }

    byte[] contentBytes = new byte[request.content().readableBytes()];
    request.content().readBytes(contentBytes);
    cache.putIfAbsent(request.uri(), contentBytes);

    FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
    ChannelFuture lastContentFuture = ctx.writeAndFlush(response);

    if (!HttpUtil.isKeepAlive(request)) {
      lastContentFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  private static void sendError(
      ChannelHandlerContext ctx, FullHttpRequest request, HttpResponseStatus status) {
    ByteBuf data = Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8);
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, data);
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, data.readableBytes());
    ChannelFuture future = ctx.writeAndFlush(response);

    if (!HttpUtil.isKeepAlive(request)) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }
}
