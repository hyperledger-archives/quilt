package org.interledger.core.asn.codecs;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

import java.util.Base64;

public class AsnConditionCodec extends AsnOctetStringBasedObjectCodec<Condition> {

  public AsnConditionCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public Condition decode() {
    return new PreimageSha256Condition(32, getBytes());
  }

  @Override
  public void encode(Condition value) {
    //TODO Review after https://github.com/interledger/java-crypto-conditions/issues/75 is closed
    if (value instanceof PreimageSha256Condition) {
      String hashBase64Url = value.getFingerprintBase64Url();
      setBytes(Base64.getUrlDecoder().decode(hashBase64Url));
    } else {
      throw new IllegalArgumentException("Only PreimageSha256Condition instances can be encoded");
    }
  }
}
