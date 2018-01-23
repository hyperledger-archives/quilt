package org.interledger.codecs.ilp.asn;

import org.interledger.codecs.asn.AsnOctetStringBasedObject;
import org.interledger.codecs.asn.AsnSizeConstraint;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;

import java.util.Base64;

public class AsnCondition extends AsnOctetStringBasedObject<Condition> {

  public AsnCondition() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  protected Condition decode() {
    return new PreimageSha256Condition(32, getBytes());
  }

  @Override
  protected void encode(Condition value) {
    //TODO Review after https://github.com/interledger/java-crypto-conditions/issues/75 is closed
    if(value instanceof PreimageSha256Condition) {
      String hashBase64Url = value.getFingerprintBase64Url();
      setBytes(Base64.getUrlDecoder().decode(hashBase64Url));
    } else {
      throw new IllegalArgumentException("Only PreimageSha256Condition instances can be encoded");
    }
  }
}
