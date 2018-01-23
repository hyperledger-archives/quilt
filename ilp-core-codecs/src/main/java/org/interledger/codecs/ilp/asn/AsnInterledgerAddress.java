package org.interledger.codecs.ilp.asn;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.asn.AsnIA5StringBasedObject;
import org.interledger.codecs.asn.AsnSizeConstraint;

public class AsnInterledgerAddress extends AsnIA5StringBasedObject<InterledgerAddress> {

  public AsnInterledgerAddress() {
    super(new AsnSizeConstraint(1, 1023));
  }

  @Override
  protected InterledgerAddress decode() {
    return InterledgerAddress.of(getCharString());
  }

  @Override
  protected void encode(InterledgerAddress value) {
    setCharString(value.getValue());
  }
}
