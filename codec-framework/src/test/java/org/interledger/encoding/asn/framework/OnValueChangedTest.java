package org.interledger.encoding.asn.framework;

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

import org.interledger.encoding.asn.codecs.AsnSequenceCodec;
import org.interledger.encoding.asn.codecs.AsnUint8Codec;

import org.hamcrest.MatcherAssert;
import org.junit.Test;


public class OnValueChangedTest {

  @Test
  public void testEvent() {

    final int[] values = new int[]{0};

    AsnUint8Codec codec = new AsnUint8Codec();
    codec.setValueChangedEventListener(source -> {
      values[0] = source.decode();
    });

    codec.encode(1);

    MatcherAssert.assertThat("Event was fired.", values[0] == 1);

  }

  @Test(expected = IllegalStateException.class)
  public void testSetterWithExistingListener() {

    AsnUint8Codec codec = new AsnUint8Codec();
    codec.setValueChangedEventListener(source -> { });
    codec.setValueChangedEventListener(source -> { });

  }

  @Test
  public void testRemove() {

    AsnUint8Codec codec = new AsnUint8Codec();
    codec.setValueChangedEventListener(source -> { });
    codec.removeEncodeEventListener();

    MatcherAssert.assertThat("Listener was removed.", !codec.hasValueChangedEventListener());

  }

}
