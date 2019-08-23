package org.interledger.encoding.asn.serializers.oer;

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

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Tests that the {@link OerLengthSerializer} correctly fails if the required length indicator
 * cannot be fully read.
 */
public class OerLengthPrefixCodecTestBadLength {

  @Test(expected = IOException.class)
  public void test_BadLengthIndicator() throws IOException {
    /*
     * we create an incorrect length indicator that says that the next two bytes encode the length
     * of the actual data. however, we'll only supply one byte of information. the codec should
     * detect this and fail, since it cannot read the length indicator.
     */
    byte lengthOfLength = (byte) ((1 << 7) | 2);

    byte[] lengthIndicator = new byte[] {lengthOfLength, 0};

    final ByteArrayInputStream inputStream = new ByteArrayInputStream(lengthIndicator);

    OerLengthSerializer.readLength(inputStream);
  }

}
