package org.interledger.encoding;

import org.interledger.annotations.Immutable;

import java.math.BigInteger;

public interface MyCustomObject {

  static MyCustomObjectBuilder builder() {
    return new MyCustomObjectBuilder();
  }

  String getUtf8StringProperty();

  /**
   * Fixed size of 4 chars.
   */
  String getFixedLengthUtf8StringProperty();

  int getUint8Property();

  long getUint32Property();

  BigInteger getUint64Property();

  byte[] getOctetStringProperty();

  BigInteger getUintProperty();

  /**
   * Fixed size of 32 bytes.
   */
  byte[] getFixedLengthOctetStringProperty();

  @Immutable
  abstract class AbstractMyCustomObject implements MyCustomObject {

  }

}
