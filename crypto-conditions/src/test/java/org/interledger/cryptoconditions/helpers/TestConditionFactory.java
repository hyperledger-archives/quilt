package org.interledger.cryptoconditions.helpers;

import org.interledger.cryptoconditions.Ed25519Sha256Condition;
import org.interledger.cryptoconditions.PrefixSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.RsaSha256Condition;
import org.interledger.cryptoconditions.ThresholdSha256Condition;

import com.google.common.collect.Lists;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

import java.security.interfaces.RSAPublicKey;

public class TestConditionFactory {
  
  /**
   * Helper to construct a {@link PreimageSha256Condition}.
   */
  public static PreimageSha256Condition constructPreimageCondition(final String preimage) {
    return TestFulfillmentFactory.constructPreimageFulfillment(preimage).getCondition();
  }

  /**
   * Helper to construct a {@link PrefixSha256Condition}.
   */
  public static PrefixSha256Condition constructPrefixSha256Condition(final String prefix) {
    return TestFulfillmentFactory.constructPrefixSha256Fulfillment(prefix).getCondition();
  }

  /**
   * Helper to construct a {@link RsaSha256Condition}.
   */
  public static RsaSha256Condition constructRsaSha256Condition(final RSAPublicKey rsaPublicKey) {
    return RsaSha256Condition.from(rsaPublicKey);
  }

  /**
   * Helper to construct a {@link RsaSha256Condition}.
   */
  public static Ed25519Sha256Condition constructEd25519Sha256Condition(
      final EdDSAPublicKey edDsaPublicKey) {
    return Ed25519Sha256Condition.from(edDsaPublicKey);
  }

  /**
   * Helper to construct a {@link ThresholdSha256Condition}.
   */
  public static ThresholdSha256Condition constructThresholdCondition(final String message) {
    return ThresholdSha256Condition.from(1, Lists.newArrayList(
        constructPreimageCondition(message)
    ));
  }
}
