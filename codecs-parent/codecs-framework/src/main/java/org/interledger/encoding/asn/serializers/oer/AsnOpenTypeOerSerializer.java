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

import org.interledger.encoding.asn.codecs.AsnOpenTypeCodec;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER object that is represented by an
 * octet string.
 */
public class AsnOpenTypeOerSerializer implements AsnObjectSerializer<AsnOpenTypeCodec> {

  @Override
  public void read(
      final AsnObjectSerializationContext context, final AsnOpenTypeCodec instance, final InputStream inputStream
  ) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);

    // WARNING: This length can be maliciously specified by the packet creator, so be careful not to use it for unsafe
    // operations, such as creating a new array of initial size `length`. This usage is safe because it merely caps the
    // InputStream to the specified packet-length, although the InputStream is authoritative for when it actually ends,
    // and this limit may be well smaller than `length`.
    int length = OerLengthSerializer.readLength(inputStream);
    context.read(instance.getInnerCodec(), ByteStreams.limit(inputStream, length));
  }

  @Override
  public void write(
      final AsnObjectSerializationContext context, final AsnOpenTypeCodec instance, final OutputStream outputStream
  ) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    //Serialize the inner Asn.1 object
    final byte[] bytes = context.write(instance.getInnerCodec());

    //Write a length prefix
    OerLengthSerializer.writeLength(bytes.length, outputStream);

    //Write the object
    if (bytes.length > 0) {
      outputStream.write(bytes);
    }
  }

}
