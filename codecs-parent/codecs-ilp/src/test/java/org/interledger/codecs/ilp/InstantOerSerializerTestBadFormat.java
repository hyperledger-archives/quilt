package org.interledger.codecs.ilp;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core Codecs
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

import org.interledger.encoding.asn.framework.CodecContext;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;

/**
 * Test cases specifically dealing with reading badly formatted {@link AsnTimestampCodec}
 * instances.
 */
public class InstantOerSerializerTestBadFormat {

  @Test(expected = IOException.class)
  public void test_NoTime() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = "20170101".getBytes();
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoMinutes() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = "2017010112".getBytes();
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoSeconds() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = "201701011213".getBytes();
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_NoMillis() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = "20170101121314".getBytes();
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

  @Test(expected = IOException.class)
  public void test_MillisShort() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final byte[] encoded = "2017010112131401".getBytes();
    final ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
    context.read(Instant.class, bis);
  }

}
