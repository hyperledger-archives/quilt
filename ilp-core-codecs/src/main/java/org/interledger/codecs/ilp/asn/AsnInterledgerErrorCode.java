package org.interledger.codecs.ilp.asn;

import org.interledger.codecs.asn.AsnIA5StringBasedObject;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.ilp.InterledgerErrorCode;

public class AsnInterledgerErrorCode extends AsnIA5StringBasedObject<InterledgerErrorCode> {

  public AsnInterledgerErrorCode() {
    super(new AsnSizeConstraint(3));
  }

  @Override
  protected InterledgerErrorCode decode() {
    return InterledgerErrorCode.valueOf(getCharString());
  }

  @Override
  protected void encode(InterledgerErrorCode value) {
    setCharString(value.getCode());
  }
}
