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
package com.google.devtools.build.lib.profiler;

import java.util.Arrays;

/**
 * Converts a set of ranges into a graph by counting the number of ranges that are active at any
 * point in time. Time is split into equal-sized buckets, and we compute one value per bucket. If a
 * range partially overlaps a bucket, then the bucket is incremented by the fraction of overlap.
 */
public class TimeSeries {
  private final long startTimeMillis;
  private final long bucketSizeMillis;
  private static final int INITIAL_SIZE = 100;
  private double[] data = new double[INITIAL_SIZE];

  public TimeSeries(long startTimeMillis, long bucketSizeMillis) {
    this.startTimeMillis = startTimeMillis;
    this.bucketSizeMillis = bucketSizeMillis;
  }

  public void addRange(long startTimeMillis, long endTimeMillis) {
    addRange(startTimeMillis, endTimeMillis, /* value= */ 1);
  }

  /** Adds a new range to the time series, by increasing every affected bucket by value. */
  public void addRange(long rangeStartMillis, long rangeEndMillis, double value) {
    // Compute times relative to start and their positions in the data array.
    rangeStartMillis -= startTimeMillis;
    rangeEndMillis -= startTimeMillis;
    int startPosition = (int) (rangeStartMillis / bucketSizeMillis);
    int endPosition = (int) (rangeEndMillis / bucketSizeMillis);

    // Assume we add the following range R:
    // ----------------------------------
    // |     |ssRRR|RRRRR|Reeee|      |
    // ----------------------------------
    // we cannot just add value to each affected bucket but have to correct the values for the first
    // and last bucket by calculating the size of 's' and 'e'.
    double missingStartFraction =
        ((double) (rangeStartMillis - bucketSizeMillis * startPosition)) / bucketSizeMillis;
    double missingEndFraction =
        ((double) (bucketSizeMillis * (endPosition + 1) - rangeEndMillis)) / bucketSizeMillis;

    if (startPosition < 0) {
      startPosition = 0;
      missingStartFraction = 0;
    }
    if (endPosition < startPosition) {
      endPosition = startPosition;
      missingEndFraction = 0;
    }

    // Resize data array if necessary so it can at least fit endPosition.
    if (endPosition >= data.length) {
      data = Arrays.copyOf(data, Math.max(endPosition + 1, 2 * data.length));
    }

    // Do the actual update.
    for (int i = startPosition; i <= endPosition; i++) {
      double fraction = 1;
      if (i == startPosition) {
        fraction -= missingStartFraction;
      }
      if (i == endPosition) {
        fraction -= missingEndFraction;
      }
      data[i] += fraction * value;
    }
  }

  public double[] toDoubleArray(int len) {
    return Arrays.copyOf(data, len);
  }
}
