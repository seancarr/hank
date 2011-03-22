/**
 *  Copyright 2011 Rapleaf
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.rapleaf.hank.partitioner;

import java.nio.ByteBuffer;

import com.rapleaf.hank.hasher.Murmur64Hasher;

/**
 * Implementation of Partitioner that takes a 64-bit Murmur hash to produce the
 * partition number.
 */
public class Murmur64Partitioner implements Partitioner {
  @Override
  public int partition(ByteBuffer key) {
    return Math.abs((int) Murmur64Hasher.murmurHash64(key.array(),
        key.arrayOffset() + key.position(),
        key.remaining(),
        1));
  }
}