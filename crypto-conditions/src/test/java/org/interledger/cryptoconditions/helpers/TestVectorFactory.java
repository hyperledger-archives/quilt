package org.interledger.cryptoconditions.helpers;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.CryptoConditionType;
import org.interledger.cryptoconditions.Ed25519Sha256Condition;
import org.interledger.cryptoconditions.Ed25519Sha256Fulfillment;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PrefixSha256Condition;
import org.interledger.cryptoconditions.PrefixSha256Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.cryptoconditions.RsaSha256Condition;
import org.interledger.cryptoconditions.RsaSha256Fulfillment;
import org.interledger.cryptoconditions.ThresholdSha256Condition;
import org.interledger.cryptoconditions.ThresholdSha256Fulfillment;

import net.i2p.crypto.eddsa.EdDSAPublicKey;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds instances from {@link Condition} for testing based on the test vectors loaded.
 */
public class TestVectorFactory {

  /**
   * Assembles an instance of {@link Condition} from the information provided in {@code
   * testVectorJson}, which is generally assembled from a JSON file in this project's test harness.
   *
   * @param testVectorJson A {@link TestVectorJson} to retrieve a condition from.
   *
   * @return A {@link Condition} assembled from the supplied test vector data.
   */
  public static Condition getConditionFromTestVectorJson(final TestVectorJson testVectorJson) {
    Objects.requireNonNull(testVectorJson);

    final CryptoConditionType type = CryptoConditionType.fromString(testVectorJson.getType());

    switch (type) {

      case PREIMAGE_SHA256: {
        return PreimageSha256Fulfillment.from(
            Base64.getUrlDecoder().decode(testVectorJson.getPreimage())
        ).getCondition();
      }

      case PREFIX_SHA256: {
        return PrefixSha256Condition.from(
            Base64.getUrlDecoder().decode(testVectorJson.getPrefix()),
            testVectorJson.getMaxMessageLength(),
            getConditionFromTestVectorJson(testVectorJson.getSubfulfillment())
        );
      }

      case RSA_SHA256: {
        final RSAPublicKey publicKey = TestKeyFactory.constructRsaPublicKey(
            testVectorJson.getModulus()
        );
        return RsaSha256Condition.from(publicKey);
      }

      case ED25519_SHA256: {
        final EdDSAPublicKey publicKey = TestKeyFactory.constructEdDsaPublicKey(
            testVectorJson.getPublicKey()
        );
        return Ed25519Sha256Condition.from(publicKey);
      }

      case THRESHOLD_SHA256: {
        //final List<Fulfillment> subFulfillments = Arrays
        //    .stream(testVectorJson.getSubfulfillments())
        //    .map(TestVectorFactory::getFulfillmentFromTestVectorJson)
        //    .collect(Collectors.toList());

        final List<Condition> subConditions = Arrays
            // This is somewhat wrong - the test vectors occasionally treat the data in
            // "testVectorJson.getSubfulfillments() as a fulfillment, and other times treat it as
            // Condition data. Thus, we get subfulfillment data but pass it into the condition test
            // factory
            .stream(testVectorJson.getSubfulfillments())
            // For example, here, we want to create a condition with a threshold
            // number that is potentially less than the number of fulfillments in the JSON, so we
            // utilize testVectorJson.getThreshold to create the condition...
            .map(TestVectorFactory::getConditionFromTestVectorJson)
            .collect(Collectors.toList());

        return ThresholdSha256Condition.from(testVectorJson.getThreshold(), subConditions);
      }

      default:
        throw new RuntimeException(String.format("Unknown Condition type: %s", type));
    }

  }

  /**
   * Assembles an instance of {@link Fulfillment} from the information provided in {@code
   * testVectorJson}, which is generally assembled from a JSON file in this project's test harness.
   *
   * @param testVectorJson A {@link TestVectorJson} to retrieve a condition from.
   *
   * @return A {@link Fulfillment} assembled from the supplied test vector data.
   */
  public static Fulfillment getFulfillmentFromTestVectorJson(final TestVectorJson testVectorJson) {
    Objects.requireNonNull(testVectorJson);

    final CryptoConditionType cryptoConditionType = CryptoConditionType
        .fromString(testVectorJson.getType());

    switch (cryptoConditionType) {

      case PREIMAGE_SHA256: {
        return PreimageSha256Fulfillment.from(
            Base64.getUrlDecoder().decode(testVectorJson.getPreimage()));
      }

      case PREFIX_SHA256: {
        return PrefixSha256Fulfillment.from(
            Base64.getUrlDecoder().decode(testVectorJson.getPrefix()),
            testVectorJson.getMaxMessageLength(),
            getFulfillmentFromTestVectorJson(testVectorJson.getSubfulfillment())
        );
      }

      case RSA_SHA256: {
        final RSAPublicKey publicKey = TestKeyFactory
            .constructRsaPublicKey(testVectorJson.getModulus());
        final byte[] signature = Base64.getUrlDecoder().decode(testVectorJson.getSignature());
        return RsaSha256Fulfillment.from(publicKey, signature);
      }

      case ED25519_SHA256: {
        final EdDSAPublicKey publicKey = TestKeyFactory
            .constructEdDsaPublicKey(testVectorJson.getPublicKey());
        final byte[] signature = Base64.getUrlDecoder().decode(testVectorJson.getSignature());
        return Ed25519Sha256Fulfillment.from(publicKey, signature);
      }

      case THRESHOLD_SHA256: {
        final List<Fulfillment> subfulfillments = Arrays
            .stream(testVectorJson.getSubfulfillments())
            .map(TestVectorFactory::getFulfillmentFromTestVectorJson)
            .collect(Collectors.toList());
        return ThresholdSha256Fulfillment.from(new LinkedList<>(), subfulfillments);
      }

      default:
        throw new RuntimeException(
            String.format("Unknown Condition cryptoConditionType: %s", cryptoConditionType));
    }

  }
}
