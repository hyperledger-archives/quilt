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

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPacket;
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
import java.util.Collection;
import java.util.Random;

/**
 * Unit tests to validate the functionality for all {@link InterledgerFulfillPacket} packets.
 */
@RunWith(Parameterized.class)
public class InterledgerFulfillPacketOerSerializerTests {

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

    final byte[] fulfillmentBytes = new byte[32];
    new Random().nextBytes(fulfillmentBytes);

    return Arrays.asList(new Object[][] {

        {
            InterledgerFulfillPacket.builder()
                .fulfillment(InterledgerFulfillment.of(fulfillmentBytes))
                .build()
        },
        {
            InterledgerFulfillPacket.builder()
                .fulfillment(InterledgerFulfillment.of(fulfillmentBytes))
                .data(new byte[] {1, 2, 3, 4, 5, 6, 7, 8})
                .build()
        },
        {
            InterledgerFulfillPacket.builder()
                .fulfillment(InterledgerFulfillment.of(fulfillmentBytes))
                .data(byteArrayOutputStream.toByteArray())
                .build()
        },

    });
  }

  /**
   * The primary difference between this test and {@link #testInterledgerPacketCodec()} is that this context call
   * specifies the type, whereas the test below determines the type from the payload.
   */
  @Test
  public void testIndividualRead() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerPaymentBytes = constructAsn1OerPacketBytes();

    final InterledgerFulfillPacket packet = context.read(InterledgerFulfillPacket.class,
        asn1OerPaymentBytes);
    assertThat(packet).isEqualTo(this.packet);
  }

  /**
   * The primary difference between this test and {@link #testIndividualRead()} is that this context determines the ipr
   * type from the payload, whereas the test above specifies the type in the method call.
   */
  @Test
  public void testInterledgerPacketCodec() throws Exception {
    final CodecContext context = InterledgerCodecContextFactory.oer();
    final ByteArrayInputStream asn1OerPaymentBytes = constructAsn1OerPacketBytes();

    final InterledgerPacket decodedPacket = context.read(InterledgerPacket.class,
        asn1OerPaymentBytes);
    assertThat(decodedPacket.getClass().getName()).isEqualTo(packet.getClass().getName());
    assertThat(decodedPacket).isEqualTo(packet);
  }

  private ByteArrayInputStream constructAsn1OerPacketBytes() throws IOException {
    final CodecContext context = InterledgerCodecContextFactory.oer();

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    context.write(packet, outputStream);

    return new ByteArrayInputStream(outputStream.toByteArray());
  }

}
