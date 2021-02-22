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

import org.interledger.encoding.asn.framework.CodecException;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * <p>A serializer with some static utility functions for reading and writing length prefixes.</p>
 *
 * <p>The length prefix that is part of the encoding of certain ASN.1 types specifies the length of an encoding in
 * terms of number of octets. A length prefix consists of one or more octets, as follows:</p>
 *
 * <p>1) If the length is less than 128, it is encoded in the lowest-order 7 bit positions of the first and only octet,
 * and the highest-order bit of the octet is set to zero.</p>
 *
 * <p>2) If the length is greater than 127, it is encoded in one or more subsequent octets, in big-endian order. The
 * number of those octets is indicated in the lowest-order 7 bit positions of the first octet, and the highest-order bit
 * of that octet is set to 1.</p>
 *
 * <p>For the second case, the length values are encoded as an unsigned binary integer using four octets, which allows
 * for a length of 4294967295. While OER lengths can technically support a length that can fit into 127 bytes, this
 * implementation caps length values at 4-byte numbers.</p>
 */
public class OerLengthSerializer {

  /**
   * Read a length prefix from the stream as a positive integer. Negative integer values are mapped to 0. Note that this
   * value will never exceed {@link Integer#MAX_VALUE}.
   *
   * @param inputStream the stream to read from.
   *
   * @return The length encoded in the length prefix.
   *
   * @throws IOException if there is an error reading from the stream.
   */
  public static int readLength(final InputStream inputStream) throws IOException {

    Objects.requireNonNull(inputStream);

    // The number of encoded octets that the encoded payload will be stored in.
    final int length;

    int initialLengthPrefixOctet = inputStream.read();
    if (initialLengthPrefixOctet >= 0 && initialLengthPrefixOctet < 128) {
      length = initialLengthPrefixOctet;
    } else {
      // Truncate the MSB and use the rest as a number...length-of-length may not exceed 127 since this can only be 1
      // signed byte. This represents the number of bytes to consume in order to determine the actual length.
      final int lengthOfLength = initialLengthPrefixOctet & 0x7F;

      // Convert the bytes into an integer...
      byte[] ba = new byte[lengthOfLength];
      int read = inputStream.read(ba, 0, lengthOfLength);

      if (read != lengthOfLength) {
        throw new IOException("Unable to read " + lengthOfLength + " bytes from stream, only read " + read);
      }
      // Ensure that this value is always unsigned...
      length = toInt(ba);
    }

    // Only allow positive lengths allowed. If negative, return 0.
    return length > 0 ? length : 0;
  }

  /**
   * Helper method to convert a byte array of varying length (assuming not larger than 4 bytes) into a positive integer.
   * This is necessary because most traditional libraries assume a 4-byte array when converting to an Integer, but in
   * ASN.1 OER we can encounter integers encoded into 1,2,3, or 4 bytes.
   *
   * @param bytes An array of up to 4 bytes representing an integer
   *
   * @return the int representation of the given bytes
   */
  @VisibleForTesting
  static int toInt(final byte[] bytes) {
    Objects.requireNonNull(bytes);
    switch (bytes.length) {
      case 0:
        return 0;
      case 1: {
        return (bytes[0]) & 0x000000FF;
      }
      case 2: {
        return (bytes[0] << 8) & 0x0000FF00
            | (bytes[1]) & 0x000000FF;
      }
      case 3: {
        return (bytes[0] << 16) & 0x00FF0000
            | (bytes[1] << 8) & 0x0000FF00
            | (bytes[2]) & 0x000000FF;
      }
      case 4: {
        return (bytes[0] << 24) & 0xFF000000
            | (bytes[1] << 16) & 0x00ff0000
            | (bytes[2] << 8) & 0x0000FF00
            | (bytes[3]) & 0x000000FF;
      }
      default: {
        throw new CodecException("This method only supports arrays up to length 4!");
      }
    }
  }

  /**
   * Write an OER length prefix to the stream.
   *
   * @param length       The length to encode and write to the stream.
   * @param outputStream the stream to write to
   *
   * @throws IOException if there is an error writing to the stream
   */
  public static void writeLength(final int length, final OutputStream outputStream) throws IOException {
    Objects.requireNonNull(outputStream);

    if (length <= 0) {
      outputStream.write(0);
    } else {
      if (length < 128) {
        // Write a single byte that contains the length (it will start with a 0, and not exceed
        // 127 in Base10.
        outputStream.write(length);
      } else {
        // Write the number of octets required to encode the length, and move the cursor to the
        // correct position.
        if (length <= 127) {
          // Write the first byte
          outputStream.write(length);
          // return 1;
        } else if (length <= 255) {
          // Write the first byte
          outputStream.write(128 + 1);
          outputStream.write(length);
          // return 2;
        } else if (length <= 65535) {
          outputStream.write(128 + 2);
          // Write the first byte, then the second byte.
          outputStream.write((length >> 8));
          outputStream.write(length);
          // return 3;
        } else if (length <= 16777215) {
          outputStream.write(128 + 3);
          // Write three bytes
          outputStream.write((length >> 16));
          outputStream.write((length >> 8));
          outputStream.write(length);
          // return 4;
        } else {
          outputStream.write(128 + 4);
          // Write four bytes,
          outputStream.write((length >> 24));
          outputStream.write((length >> 16));
          outputStream.write((length >> 8));
          outputStream.write(length);
        }
      }
    }
  }
}
