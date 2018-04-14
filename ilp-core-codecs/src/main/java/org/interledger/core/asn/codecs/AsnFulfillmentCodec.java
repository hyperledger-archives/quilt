package org.interledger.core.asn.codecs;

import org.interledger.core.InterledgerFulfillment;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

public class AsnFulfillmentCodec extends AsnOctetStringBasedObjectCodec<InterledgerFulfillment> {

  public AsnFulfillmentCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public InterledgerFulfillment decode() {
    return InterledgerFulfillment.from(getBytes());
  }

  @Override
  public void encode(InterledgerFulfillment value) {
    setBytes(value.getBytes());
  }
}
