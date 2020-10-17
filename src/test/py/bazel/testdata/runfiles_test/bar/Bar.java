// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import com.google.devtools.build.runfiles.Runfiles;
import java.io.IOException;

/** A mock Java binary only used in tests, to exercise the Java Runfiles library. */
public class Bar {
  public static void main(String[] args) throws IOException {
    System.out.println("Hello Java Bar!");
    Runfiles r = Runfiles.create();
    System.out.println("rloc=" + r.rlocation("foo_ws/bar/bar-java-data.txt"));
  }
}
