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

import org.interledger.encoding.asn.codecs.AsnCharStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.framework.AsnObjectSerializationContext;
import org.interledger.encoding.asn.framework.AsnObjectSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * An extension of {@link AsnObjectSerializer} for reading and writing an ASN.1 OER
 * object that is represented by a character string.
 */
public class AsnCharStringOerSerializer implements
    AsnObjectSerializer<AsnCharStringBasedObjectCodec> {

  @Override
  public void read(final AsnObjectSerializationContext context,
                   final AsnCharStringBasedObjectCodec instance,
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
    
    /* beware the 0-length string */
    final String result = (length == 0 ? "" :
        this.toString(inputStream, length, instance.getCharacterSet()));

    instance.setCharString(result);
  }

  @Override
  public void write(final AsnObjectSerializationContext context,
                    final AsnCharStringBasedObjectCodec instance,
                    final OutputStream outputStream) throws IOException {

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

  /**
   * Convert an {@link InputStream} into a {@link String}. Reference the SO below for an interesting
   * performance comparison of various InputStream to String methodologies.
   *
   * @param inputStream An instance of {@link InputStream}.
   * @return A {@link String}
   * @throws IOException If the {@code inputStream} is unable to be read properly.
   * @see "http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string"
   */
  private String toString(final InputStream inputStream, final int lengthToRead, Charset charset)
      throws IOException {
    Objects.requireNonNull(inputStream);
    ByteArrayOutputStream result = new ByteArrayOutputStream();

    // Read lengthToRead bytes from the inputStream into the buffer...
    byte[] buffer = new byte[lengthToRead];
    int read = inputStream.read(buffer);

    if (read != lengthToRead) {
      throw new IOException(
          "error reading " + lengthToRead + " bytes from stream, only read " + read);
    }

    result.write(buffer, 0, lengthToRead);
    return result.toString(charset.name());
  }

}
