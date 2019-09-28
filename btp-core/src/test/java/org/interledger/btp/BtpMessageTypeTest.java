package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BtpMessageTypeTest {

  @Test
  public void fromCode() {

    for (BtpMessageType type : BtpMessageType.values()) {
      assertEquals(type, BtpMessageType.fromCode(type.getCode()));
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromNegativeCode() {
    BtpMessageType.fromCode((short) -1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromZeroCode() {
    BtpMessageType.fromCode((short) 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void fromInvalidCode() {
    BtpMessageType.fromCode(Short.MAX_VALUE);
  }

}
