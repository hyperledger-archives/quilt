package org.interledger.core.asn.codecs;

import org.interledger.core.InterledgerCondition;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnConditionCodec extends AsnOctetStringBasedObjectCodec<InterledgerCondition> {

  public AsnConditionCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public InterledgerCondition decode() {
    return InterledgerCondition.from(getBytes());
  }

  @Override
  public void encode(InterledgerCondition value) {
    setBytes(value.getBytes());
  }
}
