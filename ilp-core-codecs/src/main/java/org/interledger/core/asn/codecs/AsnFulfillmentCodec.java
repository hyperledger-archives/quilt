package org.interledger.core.asn.codecs;

import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

import java.util.Base64;

public class AsnFulfillmentCodec extends AsnOctetStringBasedObjectCodec<Fulfillment> {

  public AsnFulfillmentCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public Fulfillment decode() {
    return new PreimageSha256Fulfillment(getBytes());
  }

  @Override
  public void encode(Fulfillment value) {
    //TODO Review after https://github.com/interledger/java-crypto-conditions/issues/75 is closed
    if (value instanceof PreimageSha256Fulfillment) {
      String preimageBase64 = ((PreimageSha256Fulfillment) value).getPreimage();
      setBytes(Base64.getUrlDecoder().decode(preimageBase64));
    } else {
      throw new IllegalArgumentException("Only PreimageSha256Fulfillment instances can be encoded");
    }
  }
}
