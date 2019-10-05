package org.interledger.encoding;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.math.BigInteger;

public interface MyCustomObject {

  static MyCustomObjectBuilder builder() {
    return new MyCustomObjectBuilder();
  }

  String getUtf8StringProperty();

  /**
   * Fixed size of 4 chars.
   */
  String getFixedLengthUtf8StringProperty();

  short getUint8Property();

  int getUint16Property();

  long getUint32Property();

  UnsignedLong getUint64Property();

  byte[] getOctetStringProperty();

  BigInteger getUintProperty();

  /**
   * Fixed size of 32 bytes.
   */
  byte[] getFixedLengthOctetStringProperty();

  @Value.Immutable
  @Value.Style(
      typeBuilder = "*Builder",
      visibility = Value.Style.ImplementationVisibility.PRIVATE,
      builderVisibility = Value.Style.BuilderVisibility.PUBLIC,
      redactedMask = "********",
      defaults = @Value.Immutable()
  )
  abstract class AbstractMyCustomObject implements MyCustomObject {

  }

}
