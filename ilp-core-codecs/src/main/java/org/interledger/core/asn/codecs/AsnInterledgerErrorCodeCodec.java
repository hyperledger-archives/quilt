package org.interledger.core.asn.codecs;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.encoding.asn.codecs.AsnIA5StringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnInterledgerErrorCodeCodec
    extends AsnIA5StringBasedObjectCodec<InterledgerErrorCode> {

  public AsnInterledgerErrorCodeCodec() {
    super(new AsnSizeConstraint(3));
  }

  @Override
  public InterledgerErrorCode decode() {
    return InterledgerErrorCode.valueOf(getCharString());
  }

  @Override
  public void encode(InterledgerErrorCode value) {
    setCharString(value.getCode());
  }
}
