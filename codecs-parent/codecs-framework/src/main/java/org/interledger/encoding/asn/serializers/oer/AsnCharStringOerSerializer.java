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

import org.interledger.encoding.asn.codecs.AsnCharStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;

import com.google.common.base.Utf8;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER object that is represented by a
 * character string.
 */
public class AsnCharStringOerSerializer implements AsnObjectSerializer<AsnCharStringBasedObjectCodec> {

  @Override
  public void read(
      final AsnObjectSerializationContext context,
      final AsnCharStringBasedObjectCodec instance,
      final InputStream inputStream
  ) throws IOException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(inputStream);

    // WARNING: This length can be maliciously specified by the packet creator, so be careful not to use it for unsafe
    // operations, such as creating a new array of initial size `length`. This usage is safe because it merely caps the
    // InputStream to the specified packet-length, whereas the InputStream is authoritative for when it actually ends,
    // and this limit may be well smaller than `length`.
    int lengthToRead;
    final AsnSizeConstraint sizeConstraint = instance.getSizeConstraint();
    if (sizeConstraint.isFixedSize()) {
      lengthToRead = sizeConstraint.getMax();
    } else {
      // Read the lengthToRead of the encoded OctetString...
      lengthToRead = OerLengthSerializer.readLength(inputStream);
    }

    final String result;
    /* beware the 0-lengthToRead string */
    if (lengthToRead == 0) {
      result = "";
    } else {
      // Use a limited input stream so we don't read too many bytes.
      final InputStream limitedInputStream = ByteStreams.limit(inputStream, lengthToRead);
      // WARNING: Don't close the InputStreamReader so that the underlying inputStream is not closed.
      result = CharStreams.toString(new InputStreamReader(limitedInputStream, instance.getCharacterSet().name()));

      // For UTF-8 characters, result.length() will report the viewable length (e.g., 3) but for certain encoded
      // characters, the actual byte-length will be larger (e.g., the String 元元元 is 3 viewable bytes, but 9 encoded
      // UTF-8 bytes). Thus, when we write the length-prefix, the code will write 9, so when we read, we need to
      // validate that 9 bytes were read, and not 3 (in this example).
      if (Utf8.encodedLength(result) != lengthToRead) {
        throw new IOException(
            format("Unable to properly decode %s bytes (could only read %s bytes)", lengthToRead, result.length())
        );
      }
    }

    instance.setCharString(result);
  }

  @Override
  public void write(
      final AsnObjectSerializationContext context,
      final AsnCharStringBasedObjectCodec instance,
      final OutputStream outputStream
  ) throws IOException {

    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    final byte[] data = instance.getCharString().getBytes(instance.getCharacterSet().name());
    final AsnSizeConstraint sizeConstraint = instance.getSizeConstraint();

    if (!sizeConstraint.isFixedSize()) {
      // Write the octet length of the string...
      OerLengthSerializer.writeLength(data.length, outputStream);
    }

    // Write the String bytes to the buffer.
    outputStream.write(data);
  }

}
