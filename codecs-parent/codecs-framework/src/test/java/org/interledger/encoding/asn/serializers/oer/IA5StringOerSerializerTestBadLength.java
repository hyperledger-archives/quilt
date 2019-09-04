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

import org.interledger.encoding.asn.codecs.AsnIA5StringCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Test to ensure that the {@link AsnCharStringOerSerializer} properly fails when it cannot read
 * the number of bytes indicated.
 */
public class IA5StringOerSerializerTestBadLength {

  @Test(expected = IOException.class)
  public void test_BadLengthIndicator() throws IOException {

    CodecContext context = CodecContextFactory.oer()
        .register(String.class, () -> new AsnIA5StringCodec(AsnSizeConstraint.UNCONSTRAINED));

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    context.write("A test string of reasonable length", outputStream);

    final byte[] correctData = outputStream.toByteArray();

    /*
     * we now remove a portion of the data, i.e. the length indicator should claim that there are 34
     * bytes, but we have truncated to 10 bytes
     */

    final byte[] truncated = Arrays.copyOfRange(correctData, 0, 10);

    /* create an input stream over the truncated data */
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(truncated);

    /*
     * try read the data, the codec should fail since it reaches EOF before consuming the 34 bytes
     * indicated.
     */
    context.read(String.class, inputStream);
  }

}
