// Copyright 2015 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.query2.query;

import com.google.devtools.build.lib.packages.PackageSpecification.PackageGroupContents;
import com.google.devtools.build.lib.packages.Target;
import com.google.devtools.build.lib.query2.engine.QueryVisibility;

/** An adapter from {@link PackageGroupContents} to a {@link QueryVisibility}. */
public class BlazeQueryVisibility extends QueryVisibility<Target> {

  private final PackageGroupContents packageSpecifications;

  public BlazeQueryVisibility(PackageGroupContents packageSpecifications) {
    this.packageSpecifications = packageSpecifications;
  }

  @Override
  public boolean contains(Target target) {
    return packageSpecifications.containsPackage(target.getLabel().getPackageIdentifier());
  }

  @Override
  public String toString() {
    return packageSpecifications.toString();
  }
}
