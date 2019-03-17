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
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link InterledgerResponsePacketMapper}.
 */
public class InterledgerResponsePacketMapperTest {

  private InterledgerResponsePacket fulfillPacket;
  private InterledgerResponsePacket rejectPacket;
  private InterledgerResponsePacket expiredPacket;

  @Before
  public void setup() {
    fulfillPacket = InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();

    rejectPacket =
        InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
            .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
            .message("rejected!")
            .build();

    expiredPacket = InterledgerRejectPacket.builder().triggeredBy(InterledgerAddress.of("test.foo"))
        .code(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT)
        .message("Timed out!")
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void mapNullPacket() {
    new InterledgerResponsePacketMapper<Boolean>() {
      @Override
      protected Boolean mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected Boolean mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }
    }.map(null);
    fail();
  }

  @Test
  public void mapFulfillPacket() {
    final Boolean result = new InterledgerResponsePacketMapper<Boolean>() {
      @Override
      protected Boolean mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        return true;
      }

      @Override
      protected Boolean mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        throw new RuntimeException("Should not reject!");
      }
    }.map(fulfillPacket);

    assertThat(result, is(Boolean.TRUE));
  }

  @Test
  public void mapRejectPacket() {
    final Boolean result = new InterledgerResponsePacketMapper<Boolean>() {
      @Override
      protected Boolean mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected Boolean mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.T00_INTERNAL_ERROR));
        return Boolean.TRUE;
      }

    }.map(rejectPacket);

    assertThat(result, is(Boolean.TRUE));
  }

  @Test
  public void mapExpiredPacket() {
    final Boolean result = new InterledgerResponsePacketMapper<Boolean>() {
      @Override
      protected Boolean mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
        throw new RuntimeException("Should not fulfill!");
      }

      @Override
      protected Boolean mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
        assertThat(interledgerRejectPacket.getCode(), is(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT));
        return Boolean.TRUE;
      }

    }.map(expiredPacket);

    assertThat(result, is(Boolean.TRUE));
  }
}
