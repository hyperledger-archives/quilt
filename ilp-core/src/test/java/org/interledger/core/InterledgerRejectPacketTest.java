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

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.util.Optional;

/**
 * Unit tests for {@link InterledgerRejectPacket} and {@link InterledgerRejectPacketBuilder}.
 */
public class InterledgerRejectPacketTest {

  private static final InterledgerAddress FOO = InterledgerAddress.of("test1.foo.foo");

  @Test
  public void testBuild() {
    final InterledgerErrorCode errorCode = InterledgerErrorCode.T00_INTERNAL_ERROR;
    final String message = "Test Error";
    final InterledgerAddress triggeredBy = FOO;
    final byte[] data = new byte[] {127};

    final InterledgerRejectPacket interledgerProtocolError =
        InterledgerRejectPacket.builder()
            .code(errorCode)
            .message(message)
            .triggeredBy(triggeredBy)
            .data(data)
            .build();

    assertThat(interledgerProtocolError.getCode(), is(errorCode));
    assertThat(interledgerProtocolError.getTriggeredBy().get(), is(triggeredBy));
    assertThat(interledgerProtocolError.getData(), is(data));
  }

  @Test
  public void testBuildWithoutOptionalData() {
    final InterledgerErrorCode errorCode = InterledgerErrorCode.T00_INTERNAL_ERROR;

    final InterledgerRejectPacket interledgerProtocolError =
        InterledgerRejectPacket.builder()
            .code(errorCode)
            .build();

    assertThat(interledgerProtocolError.getCode(), is(errorCode));
    assertThat(interledgerProtocolError.getTriggeredBy().isPresent(), is(false));
    assertThat(interledgerProtocolError.getMessage(), is(""));
    assertThat(interledgerProtocolError.getData(), is(new byte[0]));
  }

  @Test
  public void testBuildWithUnintializedValues() {
    try {
      InterledgerRejectPacket.builder().build();
      fail("Builder should have thrown an exception but did not!");
    } catch (Exception e) {
      assertTrue(e instanceof IllegalStateException);
      assertTrue(e.getMessage().startsWith("Cannot build InterledgerRejectPacket, "
          + "some of required attributes are not set"));
    }
  }

  @Test
  public void testBuilderWithNullValues() {
    final InterledgerRejectPacketBuilder builder = InterledgerRejectPacket.builder();

    try {
      builder.code(null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is("code"));
    }

    try {
      builder.triggeredBy((Optional<InterledgerAddress>) null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
      assertThat(e.getMessage(), is(nullValue()));
    }

    try {
      builder.data((byte[]) null);
      fail();
    } catch (Exception e) {
      assertTrue(e instanceof NullPointerException);
    }
  }

  @Test
  public void testEqualsHashCode() {
    final String message = "Test Message";
    final InterledgerRejectPacket interledgerProtocolError1
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message(message)
        .triggeredBy(FOO)
        .build();

    final InterledgerRejectPacket interledgerProtocolError2
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message(message)
        .triggeredBy(FOO)
        .build();

    assertThat(interledgerProtocolError1, is(interledgerProtocolError2));
    assertThat(interledgerProtocolError2, is(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.equals(interledgerProtocolError2));
    assertTrue(interledgerProtocolError2.equals(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.hashCode()
        == interledgerProtocolError2.hashCode());

    final InterledgerRejectPacket interledgerProtocolErrorOther
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T99_APPLICATION_ERROR)
        .message(message)
        .triggeredBy(FOO)
        .build();

    assertFalse(interledgerProtocolError1.equals(interledgerProtocolErrorOther));
    assertFalse(interledgerProtocolErrorOther.equals(interledgerProtocolError1));
    assertFalse(interledgerProtocolError1.hashCode()
        == interledgerProtocolErrorOther.hashCode());
  }

  @Test
  public void testCopyBuilder() {
    final InterledgerRejectPacket interledgerProtocolError1
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message("TEST")
        .triggeredBy(FOO)
        .data(new byte[32])
        .build();

    final InterledgerRejectPacket interledgerProtocolError2
        = InterledgerRejectPacket.builder().from(
        interledgerProtocolError1).build();

    assertTrue(interledgerProtocolError1.equals(interledgerProtocolError2));
    assertTrue(interledgerProtocolError2.equals(interledgerProtocolError1));
    assertTrue(interledgerProtocolError1.hashCode()
        == interledgerProtocolError2.hashCode());

    final InterledgerRejectPacket interledgerProtocolErrorOther
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T99_APPLICATION_ERROR)
        .message("TEST")
        .triggeredBy(FOO)
        .data(new byte[32])
        .build();

    assertFalse(interledgerProtocolError1.equals(interledgerProtocolErrorOther));
    assertFalse(interledgerProtocolErrorOther.equals(interledgerProtocolError1));
    assertFalse(interledgerProtocolError1.hashCode()
        == interledgerProtocolErrorOther.hashCode());
  }

  @Test
  public void testToString() {
    final InterledgerRejectPacket interledgerProtocolError1
        = InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .message("TEST")
        .triggeredBy(FOO)
        .data(new byte[32])
        .build();
    assertThat(interledgerProtocolError1.toString(),
        is("InterledgerRejectPacket{, code=InterledgerErrorCode{code='T00', name='INTERNAL ERROR', errorFamily=T}, triggeredBy=Optional[InterledgerAddress{value=test1.foo.foo}], message=TEST, data=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=}"));
  }

}
