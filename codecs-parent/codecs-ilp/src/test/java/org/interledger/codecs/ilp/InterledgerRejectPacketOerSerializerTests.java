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

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.encoding.asn.framework.CodecContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests to validate the serializer functionality for all {@link InterledgerRejectPacket} packets.
 */
@RunWith(Parameterized.class)
public class InterledgerRejectPacketOerSerializerTests {

  private static final InterledgerAddress FOO = InterledgerAddress.of("test1.foo.foo");
  private static final InterledgerAddress BAR = InterledgerAddress.of("test1.bar.bar");
  private static final InterledgerAddress BAZ = InterledgerAddress.of("test1.baz.baz");

  // first data value (0) is default
  @Parameter
  public InterledgerPacket packet;

  @Parameter(1)
  public String expectedB64Bytes;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {

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

    // This ByteArrayOutputStream contains a small amount of bytes
    final ByteArrayOutputStream byteArrayOutputStream4 = new ByteArrayOutputStream();
    IntStream.range(1, 2)
        .map(r::nextInt)
        .forEach(byteArrayOutputStream4::write);

    return Arrays.asList(new Object[][] {
        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
                .triggeredBy(FOO)
                .message("Internal Error")
                .data(byteArrayOutputStream1.toByteArray())
                .build(),
            "un-checked"
        },

        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.T01_PEER_UNREACHABLE)
                .triggeredBy(BAR)
                .message("Ledger Unreachable")
                .data(byteArrayOutputStream2.toByteArray())
                .build(),
            "un-checked"
        },

        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.T02_PEER_BUSY)
                .triggeredBy(BAZ)
                .message("Ledger Busy")
                .data(byteArrayOutputStream3.toByteArray())
                .build(),
            "un-checked"
        },

        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.T02_PEER_BUSY)
                .triggeredBy(BAZ)
                .message("Ledger Busy")
                .data(byteArrayOutputStream4.toByteArray())
                .build(),
            "Dh9UMDINdGVzdDEuYmF6LmJhegtMZWRnZXIgQnVzeQEA"
        },

        // Missing TriggeredBy
        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.T02_PEER_BUSY)
                .message("Ledger Busy")
                .build(),
            "DhFUMDIAC0xlZGdlciBCdXN5AA=="
        },

        // Missing Message
        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
                .triggeredBy(BAZ)
                .build(),
            "DhNGMDgNdGVzdDEuYmF6LmJhegAA"
        },

        // Missing TriggeredBy & Message
        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.T02_PEER_BUSY)
                .build(),
            "DgZUMDIAAAA="
        },

        {
            InterledgerRejectPacket.builder()
                .code(InterledgerErrorCode.F02_UNREACHABLE)
                .build(),
            "DgZGMDIAAAA=" // Hex: 0e 06 46 30 32 0 0 0
        }

    });
  }

  /**
   * The primary difference between this test and {@link #testInterledgerErrorCodec()} is that this context call
   * specifies the type, whereas the test below determines the type from the payload.
   */
  @Test
  public void testIndividualRead() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayInputStream asn1OerErrorBytesStream = constructInterledgerRejectPacketAsn1OerInputStream();
    final InterledgerRejectPacket error = context.read(InterledgerRejectPacket.class, asn1OerErrorBytesStream);
    assertThat(error).isEqualTo(packet);

    // Only verify small packets...
    if (packet.getData().length < 2) {
      final byte[] asn1OerErrorBytes = constructInterledgerRejectPacketAsn1OerBytes();
      assertThat(Base64.getEncoder().encodeToString(asn1OerErrorBytes)).isEqualTo(expectedB64Bytes);
    }

  }

  /**
   * The primary difference between this test and {@link #testIndividualRead()} is that this context determines the ipr
   * type from the payload, whereas the test above specifies the type in the method call.
   */
  @Test
  public void testInterledgerErrorCodec() throws Exception {
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerErrorBytes = constructInterledgerRejectPacketAsn1OerInputStream();

    final InterledgerPacket decodedPacket = context.read(InterledgerPacket.class,
        asn1OerErrorBytes);
    assertThat(decodedPacket.getClass().getName()).isEqualTo(packet.getClass().getName());
    assertThat(decodedPacket).isEqualTo(packet);
  }

  private byte[] constructInterledgerRejectPacketAsn1OerBytes() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(packet, outputStream);

    return outputStream.toByteArray();
  }

  private ByteArrayInputStream constructInterledgerRejectPacketAsn1OerInputStream() throws IOException {
    return new ByteArrayInputStream(constructInterledgerRejectPacketAsn1OerBytes());
  }

}
