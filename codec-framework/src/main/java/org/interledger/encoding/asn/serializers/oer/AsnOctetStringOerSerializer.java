package org.interledger.encoding.asn.serializers.oer;

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

import static java.lang.String.format;

import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;
import org.interledger.encoding.asn.framework.CodecException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER
 * object that is represented by an octet string.
 */
public class AsnOctetStringOerSerializer
    implements AsnObjectSerializer<AsnOctetStringBasedObjectCodec> {

  @Override
  public void read(final AsnObjectSerializationContext context,
                   final AsnOctetStringBasedObjectCodec instance,
                   final InputStream inputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);

    int length;
    final AsnSizeConstraint sizeConstraint = instance.getSizeConstraint();
    if (sizeConstraint.isFixedSize()) {
      length = sizeConstraint.getMax();
    } else {
      // Read the length of the encoded OctetString...
      length = OerLengthSerializer.readLength(inputStream);
    }

    final byte[] returnable = new byte[length];

    if (length == 0) {
      instance.setBytes(returnable);
    } else {

      int bytesRead = inputStream.read(returnable);
      if (bytesRead < length) {
        throw new CodecException(
            format("Unexpected end of stream. Expected %s bytes but only read %s.",
                length, bytesRead));
      }
      instance.setBytes(returnable);
    }

  }

  @Override
  public void write(final AsnObjectSerializationContext context,
                    final AsnOctetStringBasedObjectCodec instance,
                    final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    final byte[] bytes = instance.getBytes();
    final AsnSizeConstraint sizeConstraint = instance.getSizeConstraint();

    if (!sizeConstraint.isFixedSize()) {
      // Write the length of the encoded OctetString...
      OerLengthSerializer.writeLength(bytes.length, outputStream);
    }

    // Write the OctetString bytes to the buffer.
    if (bytes.length > 0) {
      outputStream.write(bytes);
    }
  }

}
