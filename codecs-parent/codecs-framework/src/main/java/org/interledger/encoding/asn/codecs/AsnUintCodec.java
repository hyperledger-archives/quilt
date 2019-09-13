package org.interledger.encoding.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

/**
 * <p>An ASN.1 codec for a variable-size ASN.1 OER integer type, backed by {@link BigInteger}.</p>
 *
 * <p>Per the OER definitions, the integer value is encoded as a length prefix, followed by an
 * unsigned binary integer occupying a variable number of octets; the length prefix contains the number of subsequent
 * octets.</p>
 */
public class AsnUintCodec extends AsnOctetStringBasedObjectCodec<BigInteger> {

  public AsnUintCodec() {
    super(AsnSizeConstraint.UNCONSTRAINED);
  }

  @Override
  public BigInteger decode() {
    return new BigInteger(1, getBytes());
  }

  @Override
  public void encode(final BigInteger value) {

    if (value.compareTo(BigInteger.ZERO) < 0) {
      throw new IllegalArgumentException("value must be positive or zero");
    }

    byte[] bytes = value.toByteArray();

    // BigInteger's toByteArray writes data in two's complement, so positive values may have a
    // leading 0x00 byte, which we want to strip off. However, we only want to strip these
    // leading values off if the number of bytes is greater than 1 so that we don't produce an
    // empty array when the BigInteger value is 0.
    if (bytes[0] == 0x00 && bytes.length > 1) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      baos.write(bytes, 1, bytes.length - 1);
      setBytes(baos.toByteArray());
      return;
    }

    setBytes(bytes);

  }

}
