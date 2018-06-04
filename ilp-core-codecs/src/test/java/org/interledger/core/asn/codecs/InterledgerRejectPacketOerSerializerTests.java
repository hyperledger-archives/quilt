package org.interledger.core.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core Codecs
 * %%
 * Copyright (C) 2017 - 2018 Interledger
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

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Unit tests to validate the serializer functionality for all {@link InterledgerRejectPacket}
 * packets.
 */
@RunWith(Parameterized.class)
public class InterledgerRejectPacketOerSerializerTests {

  private static final InterledgerAddress FOO = InterledgerAddress.of("test1.foo.foo");
  private static final InterledgerAddress BAR = InterledgerAddress.of("test1.bar.bar");
  private static final InterledgerAddress BAZ = InterledgerAddress.of("test1.baz.baz");

  // first data value (0) is default
  @Parameter
  public InterledgerPacket packet;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() throws IOException {

    final Random r = new Random();

    // This ByteArrayOutputStream contains a random amount of 32kb for testing purposes.
    final ByteArrayOutputStream byteArrayOutputStream1 = new ByteArrayOutputStream();
    IntStream.range(1, 32769)
        .map(r::nextInt)
        .forEach(byteArrayOutputStream1::write);

    // This ByteArrayOutputStream contains a random amount of 32kb for testing purposes.
    final ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
    IntStream.range(1, 32769)
        .map(r::nextInt)
        .forEach(byteArrayOutputStream2::write);

    // This ByteArrayOutputStream contains a random amount of 32kb for testing purposes.
    final ByteArrayOutputStream byteArrayOutputStream3 = new ByteArrayOutputStream();
    IntStream.range(1, 32769)
        .map(r::nextInt)
        .forEach(byteArrayOutputStream3::write);

    return Arrays.asList(new Object[][] {
        {InterledgerRejectPacket.builder()
            .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
            .triggeredBy(FOO)
            .message("Internal Error")
            .data(byteArrayOutputStream1.toByteArray())
            .build()
        },

        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.T01_LEDGER_UNREACHABLE)
                .triggeredBy(BAR)
                .message("Ledger Unreachable")
                .data(byteArrayOutputStream2.toByteArray())
                .build()
        },

        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.T02_LEDGER_BUSY)
                .triggeredBy(BAZ)
                .message("Ledger Busy")
                .data(byteArrayOutputStream3.toByteArray())
                .build()
        },

    });
  }

  /**
   * The primary difference between this test and {@link #testInterledgerErrorCodec()} is that this
   * context call specifies the type, whereas the test below determines the type from the payload.
   */
  @Test
  public void testIndividualRead() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerErrorBytes = constructInterledgerRejectPacketAsn1OerBytes();

    final InterledgerRejectPacket error = context
        .read(InterledgerRejectPacket.class, asn1OerErrorBytes);
    MatcherAssert.assertThat(error, CoreMatchers.is(packet));
  }

  /**
   * The primary difference between this test and {@link #testIndividualRead()} is that this context
   * determines the ipr type from the payload, whereas the test above specifies the type in the
   * method call.
   */
  @Test
  public void testInterledgerErrorCodec() throws Exception {
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerErrorBytes = constructInterledgerRejectPacketAsn1OerBytes();

    final InterledgerPacket decodedPacket = context.read(InterledgerPacket.class,
        asn1OerErrorBytes);
    MatcherAssert.assertThat(decodedPacket.getClass().getName(),
        CoreMatchers.is(packet.getClass().getName()));
    MatcherAssert.assertThat(decodedPacket, CoreMatchers.is(packet));
  }

  private ByteArrayInputStream constructInterledgerRejectPacketAsn1OerBytes() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(packet, outputStream);

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

}
