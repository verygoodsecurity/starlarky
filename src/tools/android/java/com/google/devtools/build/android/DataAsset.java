// Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.android;

import java.io.IOException;

/** Represents an Asset created from a binary file. */
public interface DataAsset extends DataValue {
  /** Write the asset value to mergedDataWriter. */
  void writeAsset(RelativeAssetPath key, AndroidDataWritingVisitor mergedDataWriter)
      throws IOException;

  /** Overwrite another {@link DataAsset}. */
  DataAsset overwrite(DataAsset other);
}
