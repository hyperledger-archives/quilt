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
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.encoding.asn.framework.CodecContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests to validate the functionality for all {@link InterledgerPreparePacket} packets.
 */
@RunWith(Parameterized.class)
public class InterledgerPreparePacketOerSerializerTests {

  // first data value (0) is default
  @Parameter
  public InterledgerPacket packet;

  /**
   * The data for this test...
   */
  @Parameters
  public static Collection<Object[]> data() {

    // This ByteArrayOutputStream contains a random amount of 32kb for testing purposes.
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    for (int i = 0; i < 32768; i++) {
      byteArrayOutputStream.write(i);
    }

    final byte[] conditionBytes = new byte[32];
    new Random().nextBytes(conditionBytes);

    return Arrays.asList(new Object[][] {

        {InterledgerPreparePacket.builder()
            .destination(InterledgerAddress.of("test3.foo.bar"))
            .amount(BigInteger.valueOf(100L))
            .executionCondition(InterledgerCondition.of(conditionBytes))
            .expiresAt(Instant.now()).build()},

        {InterledgerPreparePacket.builder()
            .destination(InterledgerAddress.builder().value("test1.bar.baz").build())
            .amount(BigInteger.valueOf(50L))
            .executionCondition(InterledgerCondition.of(conditionBytes))
            .expiresAt(Instant.now())
            .data(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}).build()},

        {InterledgerPreparePacket.builder()
            .destination(InterledgerAddress.builder().value("test1.bar.baz").build())
            .amount(BigInteger.valueOf(50L))
            .executionCondition(InterledgerCondition.of(conditionBytes))
            .expiresAt(Instant.now())
            .data(byteArrayOutputStream.toByteArray()).build()},

    });
  }

  /**
   * The primary difference between this test and {@link #testInterledgerPaymentCodec()} is that
   * this context call specifies the type, whereas the test below determines the type from the
   * payload.
   */
  @Test
  public void testIndividualRead() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerPaymentBytes = constructAsn1OerPaymentBytes();

    final InterledgerPreparePacket payment = context.read(InterledgerPreparePacket.class,
        asn1OerPaymentBytes);
    assertThat(payment).isEqualTo(packet);
  }

  /**
   * The primary difference between this test and {@link #testIndividualRead()} is that this context
   * determines the ipr type from the payload, whereas the test above specifies the type in the
   * method call.
   */
  @Test
  public void testInterledgerPaymentCodec() throws Exception {
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerPaymentBytes = constructAsn1OerPaymentBytes();

    final InterledgerPacket decodedPacket = context.read(InterledgerPacket.class,
        asn1OerPaymentBytes);
    assertThat(decodedPacket.getClass().getName()).isEqualTo(packet.getClass().getName());
    assertThat(decodedPacket).isEqualTo(packet);
  }

  private ByteArrayInputStream constructAsn1OerPaymentBytes() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(packet, outputStream);

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

}
