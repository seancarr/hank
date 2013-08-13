/**
 *  Copyright 2013 LiveRamp
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

package com.liveramp.hank.util;

public class SynchronizedCache<K, V> {

  private final LruHashMap<K, V> cache;

  // A cache capacity <= 0 will disable the cache and will not add any synchronization overhead
  public SynchronizedCache(int cacheCapacity) {
    if (cacheCapacity <= 0) {
      cache = null;
    } else {
      cache = new LruHashMap<K, V>(0, cacheCapacity);
    }
  }

  public boolean isActive() {
    return cache != null;
  }

  public V get(K key) {
    if (cache == null) {
      return null;
    } else {
      V cachedValue;
      synchronized (cache) {
        cachedValue = cache.get(key);
      }
      if (cachedValue != null) {
        return cachedValue;
      } else {
        return null;
      }
    }
  }

  public void put(K key, V value) {
    if (cache != null) {
      if (value == null) {
        throw new IllegalArgumentException("Value to cache should not be null.");
      }
      synchronized (cache) {
        cache.put(key, value);
      }
    }
  }
}
