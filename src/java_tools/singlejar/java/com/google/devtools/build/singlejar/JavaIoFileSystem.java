// Copyright 2014 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.singlejar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An implementation based on java.io.
 */
public final class JavaIoFileSystem implements SimpleFileSystem {

  @Override
  public InputStream getInputStream(String filename) throws IOException {
    return new FileInputStream(filename);
  }

  @Override
  public OutputStream getOutputStream(String filename) throws IOException {
    return new FileOutputStream(filename);
  }

  @Override
  public File getFile(String filename) throws IOException {
    return new File(filename);
  }

  @Override
  public boolean delete(String filename) {
    return new File(filename).delete();
  }
}