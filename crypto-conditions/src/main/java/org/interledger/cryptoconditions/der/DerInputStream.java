package org.interledger.cryptoconditions.der;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An input stream for reading DER encoded data.
 */
public class DerInputStream extends FilterInputStream {

  public DerInputStream(InputStream in) {
    super(in);
  }

  /**
   * Reads a tagged, constructed DER object from the stream.
   * 
   * @param expectedTagNumber The tag number expected to be read.
   * @param limit The maximum allowable object length.
   * @param bytesRead Will be updated with the number of bytes read from the stream.
   * @return A DER object read from the stream.
   * @throws IOException IOException
   * @throws DerEncodingException DER Object encoding error
   */
  public DerObject readTaggedConstructedObject(int expectedTagNumber, int limit,
      AtomicInteger bytesRead) throws IOException, DerEncodingException {

    DerObject object = readObject(limit, bytesRead);
    int expectedTag = DerTag.TAGGED.getTag() + DerTag.CONSTRUCTED.getTag() + expectedTagNumber;
    if (object.getTag() != expectedTag) {
      throw new DerEncodingException(
          "Expected tag: " + expectedTag + " but got: " + object.getTag());
    }

    return object;
  }

  /**
   * Reads a tagged DER object from the stream.
   * 
   * @param expectedTagNumber The tag number expected to be read.
   * @param limit The maximum allowable object length.
   * @param bytesRead Will be updated with the number of bytes read from the stream.
   * @return A DER object read from the stream.
   * @throws IOException IOException
   * @throws DerEncodingException DER Object encoding error
   */
  public DerObject readTaggedObject(int expectedTagNumber, int limit, AtomicInteger bytesRead)
      throws IOException, DerEncodingException {

    DerObject object = readObject(limit, bytesRead);
    if (object.getTag() != (DerTag.TAGGED.getTag() + expectedTagNumber)) {
      throw new DerEncodingException(
          "Expected tag: " + Integer.toHexString(DerTag.TAGGED.getTag() + expectedTagNumber)
              + " but got: " + Integer.toHexString(object.getTag()));
    }
    return object;

  }

  /**
   * Reads a DER object from the stream.
   * 
   * @param limit The maximum allowable object length.
   * @param bytesRead Will be updated with the number of bytes read from the stream.
   * @return A DER object read from the stream.
   * @throws IOException IOException
   * @throws DerEncodingException DER Object encoding error
   */
  public DerObject readObject(int limit, AtomicInteger bytesRead)
      throws IOException, DerEncodingException {

    AtomicInteger innerBytesRead = new AtomicInteger(0);
    DerObject obj = new DerObject();
    obj.setTag(readTag(innerBytesRead));
    obj.setLength(readLength(innerBytesRead));
    if (innerBytesRead.get() + obj.getLength() > limit) {
      throw new DerEncodingException(
          "Object length [" + obj.getLength() + "] is larger than allowed.");
    }
    bytesRead.addAndGet(innerBytesRead.get());

    if (obj.getLength() > 0) {
      obj.setValue(readValue(obj.getLength(), bytesRead));
    } else {
      obj.setValue(new byte[] {});
    }
    return obj;
  }

  /**
   * Reads a DER tag from the stream.
   * 
   * @param expectedTag The expected tag.
   * @param bytesRead Will be updated with the number of bytes read from the stream.
   * @param flags A set of expected DER tags.
   * @return The tag read from the stream.
   * @throws IOException IOException
   * @throws DerEncodingException DER Object encoding error
   */
  public int readTag(int expectedTag, AtomicInteger bytesRead, DerTag... flags)
      throws DerEncodingException, IOException {
    int tag = readTag(bytesRead, flags);

    if (tag != expectedTag) {
      throw new DerEncodingException("Expected tag: " + Integer.toHexString(expectedTag) + ", got: "
          + Integer.toHexString(tag));
    }
    return tag;
  }

  /**
   * Reads a DER tag from the stream.
   * 
   * @param bytesRead Will be updated with the number of bytes read from the stream.
   * @param expectedFlags A set of DER tags that are expected.
   * @return A DER tag read from the stream.
   * @throws IOException IOException
   * @throws DerEncodingException DER Object encoding error
   */
  public int readTag(AtomicInteger bytesRead, DerTag... expectedFlags)
      throws DerEncodingException, IOException {

    int tag = in.read();
    bytesRead.incrementAndGet();

    if (tag < 0) {
      throw new DerEncodingException("Expected tag, got end of stream.");
    }

    for (DerTag expected : expectedFlags) {
      tag -= expected.getTag();
    }

    if (tag < 0) {
      throw new DerEncodingException("Some flags are missing resulting in a tag value of < 0.");
    }

    return tag;
  }

  /**
   * Reads a length indicator from the DER encoded stream.
   * 
   * @param bytesRead Will be updated with the number of bytes read.
   * @return The value of the length indicator.
   * @throws IOException IOException
   * @throws DerEncodingException DER Object encoding error
   */
  public int readLength(AtomicInteger bytesRead) throws DerEncodingException, IOException {

    int lengthOfLength = 1;
    int length = in.read();
    bytesRead.incrementAndGet();

    if (length > 127) {
      lengthOfLength = length & 0x7f;
      if (lengthOfLength > 4) {
        throw new DerEncodingException("DER length more than 4 bytes: " + lengthOfLength);
      }
      length = 0;
      for (int i = 0; i < lengthOfLength; i++) {
        int next = in.read();
        bytesRead.incrementAndGet();
        if (next < 0) {
          throw new DerEncodingException("End of stream found reading length.");
        }

        length = (length << 8) + next;
      }
      if (length < 0) {
        throw new DerEncodingException("Negative length found: " + length);
      }
    }

    return length;
  }


  /**
   * Reads a raw value from the DER encoded stream.
   * 
   * @param length The number of bytes to read.
   * @param bytesRead Will be updated with the number of bytes read.
   * @return The raw value read from the stream.
   * @throws IOException IOException
   * @throws DerEncodingException DER Object encoding error
   */
  public byte[] readValue(int length, AtomicInteger bytesRead)
      throws IOException, DerEncodingException {

    byte[] buffer = new byte[length];
    if (in.read(buffer, 0, length) < length) {
      throw new DerEncodingException("End of stream found reading value.");
    }
    bytesRead.addAndGet(length);

    return buffer;
  }
}
