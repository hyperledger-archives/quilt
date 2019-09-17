package org.interledger.encoding.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
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

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.encoding.asn.framework.CodecException;

import org.junit.Test;

/**
 * Unit tests for {@link AsnOctetStringBasedObjectCodec}.
 */
public class AsnOctetStringBasedObjectCodecTest {

  @Test
  public void getAndSetBytesUnconstrained() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(AsnSizeConstraint.UNCONSTRAINED);
    final byte[] bytes = new byte[1];
    codec.setBytes(bytes);
    assertThat(codec.getBytes()).isEqualTo(bytes);
  }

  @Test
  public void setBytesToZero() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(0));
    final byte[] bytes = new byte[0];
    codec.setBytes(bytes);
    assertThat(codec.getBytes()).isEqualTo(bytes);
  }

  @Test(expected = CodecException.class)
  public void setBytesTooBig() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(0, 1));
    final byte[] bytes = new byte[2];
    try {
      codec.setBytes(bytes);
    } catch (CodecException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid octet string length. Expected < 1, got 2");
      throw e;
    }
  }

  @Test(expected = CodecException.class)
  public void setBytesTooBigForFixedSize() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(1, 1));
    final byte[] bytes = new byte[2];
    try {
      codec.setBytes(bytes);
    } catch (CodecException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid octet string length. Expected 1, got 2");
      throw e;
    }
  }

  @Test(expected = CodecException.class)
  public void setBytesTooSmall() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(1, 2));
    final byte[] bytes = new byte[0];
    try {
      codec.setBytes(bytes);
    } catch (CodecException e) {

      assertThat(e.getMessage()).isEqualTo("Invalid octet string length. Expected > 1, got 0");
      throw e;
    }
  }

  @Test(expected = CodecException.class)
  public void setBytesTooSmallForFixedSize() {
    final AsnOctetStringBasedObjectCodec codec = createCodec(new AsnSizeConstraint(1, 1));
    final byte[] bytes = new byte[0];
    try {
      codec.setBytes(bytes);
    } catch (CodecException e) {
      assertThat(e.getMessage()).isEqualTo("Invalid octet string length. Expected 1, got 0");
      throw e;
    }
  }

  @Test
  public void testSetEquals() {
    final AsnOctetStringBasedObjectCodec codec1 = createCodec(new AsnSizeConstraint(1, 1));
    final AsnOctetStringBasedObjectCodec codec2 = createCodec(new AsnSizeConstraint(1));
    final AsnOctetStringBasedObjectCodec codec3 = createCodec(new AsnSizeConstraint(0));

    assertThat(codec1).isEqualTo(codec2);
    assertThat(codec3).isNotEqualTo(codec1);
    assertThat(codec1).isNotEqualTo(codec3);
    assertThat(codec2).isNotEqualTo(codec3);
    assertThat(codec3).isNotEqualTo(codec2);
  }

  private AsnOctetStringBasedObjectCodec createCodec(AsnSizeConstraint constraint) {
    return new AsnOctetStringBasedObjectCodec(constraint) {
      @Override
      public Object decode() {
        return "foo";
      }

      @Override
      public void encode(Object value) {
        // No-op.
      }
    };
  }
}
