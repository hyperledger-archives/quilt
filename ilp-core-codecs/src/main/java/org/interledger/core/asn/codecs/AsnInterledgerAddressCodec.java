package org.interledger.core.asn.codecs;

import org.interledger.core.InterledgerAddress;
import org.interledger.encoding.asn.codecs.AsnIA5StringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnInterledgerAddressCodec extends AsnIA5StringBasedObjectCodec<InterledgerAddress> {

  public AsnInterledgerAddressCodec() {
    super(new AsnSizeConstraint(1, 1023));
  }

  @Override
  public InterledgerAddress decode() {
    return InterledgerAddress.of(getCharString());
  }

  @Override
  public void encode(InterledgerAddress value) {
    setCharString(value.getValue());
  }
}
