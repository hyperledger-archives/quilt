package org.interledger.core.asn.codecs;

import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

import java.util.Base64;

public class AsnFulfillmentCodec extends AsnOctetStringBasedObjectCodec<PreimageSha256Fulfillment> {

  public AsnFulfillmentCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public PreimageSha256Fulfillment decode() {
    return PreimageSha256Fulfillment.from(getBytes());
  }

  @Override
  public void encode(PreimageSha256Fulfillment value) {
    setBytes(Base64.getUrlDecoder().decode(value.getEncodedPreimage()));
  }
}
