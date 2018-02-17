package org.interledger.cryptoconditions.helpers;

import static org.interledger.cryptoconditions.helpers.TestKeyFactory.RSA_MODULUS;

import org.interledger.cryptoconditions.Ed25519Sha256Fulfillment;
import org.interledger.cryptoconditions.PrefixSha256Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.cryptoconditions.RsaSha256Fulfillment;
import org.interledger.cryptoconditions.ThresholdSha256Fulfillment;

import com.google.common.collect.Lists;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;

public class TestFulfillmentFactory {

  public static final String PREIMAGE1 = "Roads? Where we're going, we don't need roads.";
  public static final String PREIMAGE2 = "Great Scott!";
  public static final String PREFIX1 = "Order-12345";
  public static final String PREFIX2 = "Order-98765";
  public static final String MESSAGE = "Doc Brown";
  public static final String MESSAGE2 = "Marty McFly";

  /**
   * Helper to construct a {@link PreimageSha256Fulfillment}.
   */
  public static PreimageSha256Fulfillment constructPreimageFulfillment(final String preimage) {
    return PreimageSha256Fulfillment.from(preimage.getBytes());
  }

  /**
   * Helper to construct a {@link PrefixSha256Fulfillment}.
   */
  public static PrefixSha256Fulfillment constructPrefixSha256Fulfillment(final String prefix) {
    return PrefixSha256Fulfillment
        .from(prefix.getBytes(), 1000, constructPreimageFulfillment(PREIMAGE1));
  }

  /**
   * Helper to construct a {@link RsaSha256Fulfillment} with a known public key.
   */
  public static RsaSha256Fulfillment constructRsaSha256Fulfillment(
      final KeyPair rsaKeyPair
  ) {
    try {
      final Signature rsaSigner = Signature.getInstance("SHA256withRSA/PSS");
      rsaSigner.initSign(rsaKeyPair.getPrivate());
      rsaSigner.update(MESSAGE.getBytes());
      byte[] rsaSignature = rsaSigner.sign();
      return constructRsaSha256Fulfillment((RSAPublicKey) rsaKeyPair.getPublic(), rsaSignature);
    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper to construct a {@link RsaSha256Fulfillment} with a known public key  and signature
   * signed by the corresponding private key.
   */
  public static RsaSha256Fulfillment constructRsaSha256Fulfillment(
      final RSAPublicKey rsaPublicKey,
      final byte[] signature
  ) {
    return RsaSha256Fulfillment.from(rsaPublicKey, signature);
  }

  /**
   * Helper to construct a {@link Ed25519Sha256Fulfillment} with a known public key.
   */
  public static Ed25519Sha256Fulfillment constructEd25519Sha256Fulfillment(
      final KeyPair ed25519KeyPair
  ) {
    try {
      final MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");
      final Signature edDsaSigner = new EdDSAEngine(sha512Digest);
      edDsaSigner.initSign(ed25519KeyPair.getPrivate());
      edDsaSigner.update(MESSAGE.getBytes());
      byte[] edDsaSignature = edDsaSigner.sign();

      return constructEd25519Sha256Fulfillment(
          (EdDSAPublicKey) ed25519KeyPair.getPublic(),
          edDsaSignature
      );
    } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper to construct a {@link Ed25519Sha256Fulfillment} with a known Ed25519 public key and
   * signature signed by the corresponding private key.
   */
  public static Ed25519Sha256Fulfillment constructEd25519Sha256Fulfillment(
      final EdDSAPublicKey edDsaPublicKey,
      final byte[] signature
  ) {
    return Ed25519Sha256Fulfillment.from(edDsaPublicKey, signature);
  }

  /**
   * Helper to construct a {@link ThresholdSha256Fulfillment}.
   */
  public static ThresholdSha256Fulfillment constructThresholdFulfillment() {
    return ThresholdSha256Fulfillment.from(
        Lists.newArrayList(TestConditionFactory
            .constructRsaSha256Condition(TestKeyFactory.constructRsaPublicKey(RSA_MODULUS))),
        Lists.newArrayList(
            TestFulfillmentFactory.constructPreimageFulfillment(PREIMAGE1),
            TestFulfillmentFactory.constructPrefixSha256Fulfillment(PREFIX1)
        )
    );
  }
}
