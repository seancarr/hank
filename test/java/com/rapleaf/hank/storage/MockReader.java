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
package com.rapleaf.hank.storage;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.rapleaf.hank.config.PartservConfigurator;

public class MockReader implements Reader {

  private final PartservConfigurator configurator;
  private final int partNum;
  private final byte[] returnValue;

  public MockReader(PartservConfigurator configurator, int partNum, byte[] returnValue) {
    this.configurator = configurator;
    this.partNum = partNum;
    this.returnValue = returnValue;
  }

  @Override
  public void get(ByteBuffer key, Result result) throws IOException {
    result.requiresBufferSize(returnValue.length);
    result.getBuffer().position(0).limit(returnValue.length);
    result.getBuffer().put(returnValue);
    result.getBuffer().flip();
    result.found();
  }

  public PartservConfigurator getConfigurator() {
    return configurator;
  }

  public int getPartNum() {
    return partNum;
  }
}