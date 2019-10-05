package org.interledger.core;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.time.Instant;

/**
 * Unit tests for {@link InterledgerPreparePacket} and {@link InterledgerPreparePacketBuilder}.
 */
public class InterledgerPreparePacketTest {

  @Test
  public void testBuild() {
    final InterledgerAddress destination = mock(InterledgerAddress.class);
    byte[] data = new byte[] {127};
    UnsignedLong amount = UnsignedLong.valueOf(10L);
    InterledgerCondition interledgerCondition = InterledgerCondition.of(
        new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 01, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
            7, 8, 9, 0, 1, 2}
    );
    Instant expiry = Instant.now().plusSeconds(30);

    final InterledgerPreparePacket interledgerPreparePacket =
        InterledgerPreparePacket.builder()
            .destination(destination)
            .amount(amount)
            .executionCondition(interledgerCondition)
            .expiresAt(expiry)
            .data(data)
            .build();

    assertThat(interledgerPreparePacket.getDestination()).isEqualTo(destination);
    assertThat(interledgerPreparePacket.getAmount()).isEqualTo(amount);
    assertThat(interledgerPreparePacket.getExecutionCondition()).isEqualTo(interledgerCondition);
    assertThat(interledgerPreparePacket.getExpiresAt()).isEqualTo(expiry);
    assertThat(interledgerPreparePacket.getData()).isEqualTo(data);
  }

  @Test
  public void testBuildWithNullValues() throws Exception {
    try {
      InterledgerPreparePacket.builder().build();
      fail("cannot build packet");
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    //No data
    try {
      InterledgerPreparePacket.builder()
          .destination(mock(InterledgerAddress.class))
          .amount(UnsignedLong.ZERO)
          .executionCondition(mock(InterledgerCondition.class))
          .expiresAt(Instant.now())
          .build();
    } catch (IllegalStateException e) {
      fail("cannot build packet");
    }

    //No expiry
    try {
      InterledgerPreparePacket.builder()
          .destination(mock(InterledgerAddress.class))
          .amount(UnsignedLong.ZERO)
          .executionCondition(mock(InterledgerCondition.class))
          .build();
      fail("cannot build packet");
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    //No condition
    try {
      InterledgerPreparePacket.builder()
          .destination(mock(InterledgerAddress.class))
          .amount(UnsignedLong.ZERO)
          .expiresAt(Instant.now())
          .build();
      fail("cannot build packet");
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    //No amount
    final InterledgerPreparePacket preparePacket = InterledgerPreparePacket
        .builder()
        .destination(mock(InterledgerAddress.class))
        .executionCondition(mock(InterledgerCondition.class))
        .expiresAt(Instant.now())
        .build();
    assertThat(preparePacket).isNotNull();
    assertThat(preparePacket.getAmount()).isEqualTo(UnsignedLong.ZERO);

    //No destination
    try {
      InterledgerPreparePacket.builder()
          .amount(UnsignedLong.ZERO)
          .executionCondition(mock(InterledgerCondition.class))
          .expiresAt(Instant.now())
          .build();
      fail("cannot build packet");
    } catch (IllegalStateException e) {
      assert (e.getMessage().startsWith("Cannot build InterledgerPreparePacket, "
          + "some of required attributes are not set"));
    }

    final InterledgerPreparePacket interledgerPreparePacket =
        InterledgerPreparePacket.builder()
            .destination(mock(InterledgerAddress.class))
            .amount(UnsignedLong.ZERO)
            .executionCondition(mock(InterledgerCondition.class))
            .expiresAt(Instant.now())
            .build();
    assertThat(interledgerPreparePacket).isNotNull();
  }

  @Test
  public void testEqualsHashCode() {
    final InterledgerAddress destination = mock(InterledgerAddress.class);
    byte[] data = new byte[] {127};
    UnsignedLong amount = UnsignedLong.valueOf(10L);
    InterledgerCondition interledgerCondition = InterledgerCondition.of(
        new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 01, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
            7, 8, 9, 0, 1, 2}
    );
    Instant expiry = Instant.now().plusSeconds(30);

    final InterledgerPreparePacket interledgerPreparePacket1 =
        InterledgerPreparePacket.builder()
            .destination(destination)
            .amount(amount)
            .executionCondition(interledgerCondition)
            .expiresAt(expiry)
            .data(data)
            .build();

    final InterledgerPreparePacket interledgerPreparePacket2 =
        InterledgerPreparePacket.builder()
            .destination(destination)
            .amount(amount)
            .executionCondition(interledgerCondition)
            .expiresAt(expiry)
            .data(data)
            .build();

    assertThat(interledgerPreparePacket1).isEqualTo(interledgerPreparePacket2);
    assertThat(interledgerPreparePacket2).isEqualTo(interledgerPreparePacket1);
    assertThat(interledgerPreparePacket1.hashCode()).isEqualTo(interledgerPreparePacket2.hashCode());

    final InterledgerPreparePacket interledgerPreparePacket3 = InterledgerPreparePacket.builder()
        .destination(destination)
        .amount(amount.plus(UnsignedLong.ONE))
        .executionCondition(interledgerCondition)
        .expiresAt(expiry)
        .data(data)
        .build();

    assertThat(interledgerPreparePacket1).isNotEqualTo(interledgerPreparePacket3);
    assertThat(interledgerPreparePacket3).isNotEqualTo(interledgerPreparePacket1);
    assertThat(interledgerPreparePacket1.hashCode()).isNotEqualTo(interledgerPreparePacket3.hashCode());
  }

  @Test
  public void testToString() {
    final InterledgerAddress destination = InterledgerAddress.of("test.foo");
    byte[] data = new byte[] {127};
    UnsignedLong amount = UnsignedLong.valueOf(10L);
    InterledgerCondition interledgerCondition = InterledgerCondition.of(
        new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 01, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6,
            7, 8, 9, 0, 1, 2}
    );
    Instant expiry = Instant.parse("2007-12-03T10:15:30.00Z");

    final InterledgerPreparePacket interledgerPreparePacket = InterledgerPreparePacket.builder()
        .destination(destination)
        .amount(amount.plus(UnsignedLong.ONE))
        .executionCondition(interledgerCondition)
        .expiresAt(expiry)
        .data(data)
        .build();

    assertThat(interledgerPreparePacket.toString())
        .isEqualTo("InterledgerPreparePacket{, amount=11, expiresAt=2007-12-03T10:15:30Z, "
            + "executionCondition=Condition{hash=AAECAwQFBgcICQECAwQFBgcICQABAgMEBQYHCAkAAQI=}, "
            + "destination=InterledgerAddress{value=test.foo}, data=fw==}");
  }

}
