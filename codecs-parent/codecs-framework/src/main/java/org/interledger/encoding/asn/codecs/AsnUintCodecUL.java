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

import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Optional;

/**
 * <p>An ASN.1 codec for a variable-size ASN.1 OER integer type, backed by {@link BigInteger}.</p>
 *
 * <p>Per the OER definitions, the integer value is encoded as a length prefix, followed by an
 * unsigned binary integer occupying a variable number of octets; the length prefix contains the number of subsequent
 * octets.</p>
 *
 * @see "http://www.oss.com/asn1/resources/books-whitepapers-pubs/Overview_of_OER.pdf"
 */
public class AsnUintCodecUL extends AsnOctetStringBasedObjectCodec<UnsignedLong> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Optional<UnsignedLong> defaultValue;

  public AsnUintCodecUL() {
    super(AsnSizeConstraint.UNCONSTRAINED);
    this.defaultValue = Optional.empty();
  }

  /**
   * Required-args Constructor.
   *
   * @param defaultValue An {@link UnsignedLong} that can be used if the ASN.1 OER bytes cannot be decoded into an
   *                     Unsigned Long.
   */
  public AsnUintCodecUL(final UnsignedLong defaultValue) {
    super(AsnSizeConstraint.UNCONSTRAINED);
    this.defaultValue = Optional.of(defaultValue);
  }

  @Override
  public UnsignedLong decode() {

    // If the number is unable to be decoded or otherwise doesn't fit into an UnsignedLong, then an exception will be
    // thrown. Optionally-present default values can be used, or if absent, an exception will be thrown.
    try {
      return UnsignedLong.valueOf(new BigInteger(1, getBytes()));
    } catch (Exception e) {
      if (defaultValue.isPresent()) {
        logger.warn(
          "Variable Unsigned Integer was too big for VarUInt: {}. Returning default value: {}",
          Base64.getEncoder().encodeToString(getBytes()), defaultValue.get()
        );
        return defaultValue.get();
      } else {
        logger.warn(
          "AsnUintCodecUL had no default specified. "
            + "This may be incorrect for protocols like STREAM (e.g., StreamMaxMoney frames).");
        throw e;
      }
    }
  }

  @Override
  public void encode(final UnsignedLong value) {

    byte[] bytes = value.bigIntegerValue().toByteArray();

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
