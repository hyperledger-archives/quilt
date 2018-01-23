package org.interledger.codecs.ilp.asn;

import org.interledger.codecs.asn.AsnOctetStringBasedObject;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;

import java.util.Base64;

public class AsnFulfillment extends AsnOctetStringBasedObject<Fulfillment> {

  public AsnFulfillment() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  protected Fulfillment decode() {
    return new PreimageSha256Fulfillment(getBytes());
  }

  @Override
  protected void encode(Fulfillment value) {
    //TODO Review after https://github.com/interledger/java-crypto-conditions/issues/75 is closed
    if(value instanceof PreimageSha256Fulfillment) {
      String preimageBase64 = ((PreimageSha256Fulfillment) value).getPreimage();
      setBytes(Base64.getUrlDecoder().decode(preimageBase64));
    } else {
      throw new IllegalArgumentException("Only PreimageSha256Fulfillment instances can be encoded");
    }
  }
}
