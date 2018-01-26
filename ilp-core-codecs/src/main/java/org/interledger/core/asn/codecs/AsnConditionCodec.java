package org.interledger.core.asn.codecs;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.encoding.asn.codecs.AsnOctetStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

import java.util.Base64;

public class AsnConditionCodec extends AsnOctetStringBasedObjectCodec<PreimageSha256Condition> {

  public AsnConditionCodec() {
    super(new AsnSizeConstraint(32));
  }

  @Override
  public PreimageSha256Condition decode() {
    return PreimageSha256Condition.fromCostAndFingerprint(32, getBytes());
  }

  @Override
  public void encode(PreimageSha256Condition value) {
    setBytes(Base64.getUrlDecoder().decode(value.getFingerprintBase64Url()));
  }
}
