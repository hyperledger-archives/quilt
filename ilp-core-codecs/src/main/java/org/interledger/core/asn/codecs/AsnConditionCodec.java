package org.interledger.core.asn.codecs;

import org.interledger.core.Condition;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnConditionCodec extends AsnOctetStringBasedObjectCodec<Condition> {

  public AsnConditionCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public Condition decode() {
    return Condition.of(getBytes());
  }

  @Override
  public void encode(Condition value) {
    setBytes(value.getHash());
  }
}
