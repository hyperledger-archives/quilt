package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link InterledgerResponsePacketHandler}.
 */
public class InterledgerResponsePacketHandlerTest {

  private InterledgerResponsePacket fulfillPacket;
  private InterledgerResponsePacket rejectPacket;
  private InterledgerResponsePacket expiredPacket;

  @Before
  public void setup() {
    fulfillPacket = InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();

    rejectPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message("rejected!")
        .build();

    expiredPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT)
        .message("Timed out!")
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void handleNullPacket() {
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }

    }.handle(null);
  }

  @Test
  public void handleFulfillPacket() {
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        // No-op, success!
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }

    }.handle(fulfillPacket);
  }

  @Test
  public void handleRejectPacket() {
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.T00_INTERNAL_ERROR));
        // No-op, success!
      }

    }.handle(rejectPacket);
  }

  @Test
  public void handleExpiredPacket() {
    new InterledgerResponsePacketHandler() {
      @Override
      protected void handleFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected void handleRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT));
        // No-op
      }

    }.handle(expiredPacket);
  }

}
