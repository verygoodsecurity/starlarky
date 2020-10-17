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
package com.google.devtools.build.lib.util.io;

import com.google.protobuf.Message;
import java.io.IOException;

/**
 * A variation of OutputStream for protobuf messages.
 */
public interface MessageOutputStream {
  /**
   * Writes a delimited protocol buffer message in the same format as {@link
   * MessageLite#writeDelimitedTo(java.io.OutputStream)}.
   */
  void write(Message m) throws IOException;

  /** Closes the underlying stream, following writes will fail. */
  void close() throws IOException;
}
