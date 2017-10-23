package org.interledger.cryptoconditions.der;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream for writing DER encoded data.
 */
public class DerOutputStream extends FilterOutputStream {

  public DerOutputStream(OutputStream out) {
    super(out);
  }

  /**
   * Writes a DER encoded length indicator to the stream.
   * 
   * @param length The length value to writeFulfillment to the stream.
   */
  public void writeLength(int length) throws IOException {
    if (length > 127) {
      int size = 1;
      int val = length;

      while ((val >>>= 8) != 0) {
        size++;
      }

      write((byte) (size | 0x80));

      for (int i = (size - 1) * 8; i >= 0; i -= 8) {
        write((byte) (length >> i));
      }
    } else {
      write((byte) length);
    }
  }

  /**
   * Writes an encoded DER value to the stream.
   * 
   * @param tag The DER tag that should accompany the value.
   * @param bytes The value to writeFulfillment to the stream.
   */
  public void writeEncoded(int tag, byte[] bytes) throws IOException {
    write(tag);
    writeLength(bytes.length);
    write(bytes);
  }

  /**
   * Writes the value as a DER tagged object.
   * 
   * @param tagNumber The tag number for the object.
   * @param object The value to writeFulfillment to the stream.
   */
  public void writeTaggedObject(int tagNumber, byte[] object) throws IOException {
    writeEncoded(DerTag.TAGGED.getTag() + tagNumber, object);
  }

  /**
   * Writes the value as a DER tagged, constructed object.
   * 
   * @param tagNumber The tag number for the object.
   * @param object The value to writeFulfillment to the stream.
   */
  public void writeTaggedConstructedObject(int tagNumber, byte[] object) throws IOException {
    writeEncoded(DerTag.TAGGED.getTag() + DerTag.CONSTRUCTED.getTag() + tagNumber, object);
  }

}
