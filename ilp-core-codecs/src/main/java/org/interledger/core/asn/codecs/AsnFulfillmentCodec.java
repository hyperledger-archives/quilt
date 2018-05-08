package org.interledger.core.asn.codecs;

import org.interledger.core.Fulfillment;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnFulfillmentCodec extends AsnOctetStringBasedObjectCodec<Fulfillment> {

  public AsnFulfillmentCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public Fulfillment decode() {
    return Fulfillment.of(getBytes());
  }

  @Override
  public void encode(Fulfillment value) {
    setBytes(value.getPreimage());
  }
}
