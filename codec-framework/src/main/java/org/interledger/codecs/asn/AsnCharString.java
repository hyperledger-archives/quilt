package org.interledger.codecs.asn;

import java.nio.charset.Charset;

/**
 * ASN.1 object that extends an Octet String.
 */
public abstract class AsnCharString extends AsnCharStringBasedObject<String> {

  public AsnCharString(AsnSizeConstraint sizeConstraint, Charset characterSet) {
    super(sizeConstraint, characterSet);
  }

  @Override
  protected String decode() {
    return getCharString();
  }

  @Override
  protected void encode(String value) {
    setCharString(value);
  }

}