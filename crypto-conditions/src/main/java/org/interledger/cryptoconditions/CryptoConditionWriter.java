package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.der.DerEncodingException;
import org.interledger.cryptoconditions.der.DerOutputStream;
import org.interledger.cryptoconditions.der.DerTag;
import org.interledger.cryptoconditions.utils.UnsignedBigInteger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Objects;

/**
 * Provides utility methods to read a crypto-condition from DER encoding.
 */
public class CryptoConditionWriter {

  /**
   * Encodes a Condition using ASN.1 DER encoding.
   *
   * @param condition A {@link Condition} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code condition}.
   *
   * @throws DerEncodingException if any of the DER encoded bytes are not encoded properly.
   */
  public static byte[] writeCondition(final Condition condition) throws DerEncodingException {
    Objects.requireNonNull(condition);

    if (condition instanceof PreimageSha256Condition) {
      return writeSingleCondition((PreimageSha256Condition) condition);
    } else if (condition instanceof PrefixSha256Condition) {
      return writeCompoundCondition((PrefixSha256Condition) condition);
    } else if (condition instanceof Ed25519Sha256Condition) {
      return writeSingleCondition((Ed25519Sha256Condition) condition);
    } else if (condition instanceof RsaSha256Condition) {
      return writeSingleCondition((RsaSha256Condition) condition);
    } else if (condition instanceof ThresholdSha256Condition) {
      return writeCompoundCondition((ThresholdSha256Condition) condition);
    } else {
      throw new IllegalArgumentException(
          String.format("Unhandled Condition type: %s", condition.getClass().getName())
      );
    }
  }

  /**
   * Encodes a Fulfillment using ASN.1 DER encoding.
   *
   * @param fulfillment A {@link Fulfillment} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code fulfillment}.
   *
   * @throws DerEncodingException if any of the DER encoded bytes are not encoded properly.
   */
  public static byte[] writeFulfillment(final Fulfillment fulfillment) throws DerEncodingException {
    Objects.requireNonNull(fulfillment);

    if (fulfillment instanceof PreimageSha256Fulfillment) {
      return writeTypedFulfillment((PreimageSha256Fulfillment) fulfillment);
    } else if (fulfillment instanceof PrefixSha256Fulfillment) {
      return writeTypedFulfillment((PrefixSha256Fulfillment) fulfillment);
    } else if (fulfillment instanceof Ed25519Sha256Fulfillment) {
      return writeTypedFulfillment((Ed25519Sha256Fulfillment) fulfillment);
    } else if (fulfillment instanceof RsaSha256Fulfillment) {
      return writeTypedFulfillment((RsaSha256Fulfillment) fulfillment);
    } else if (fulfillment instanceof ThresholdSha256Fulfillment) {
      return writeTypedFulfillment((ThresholdSha256Fulfillment) fulfillment);
    } else {
      throw new IllegalArgumentException(
          String.format("Unhandled Fulfillment type: %s", fulfillment.getClass().getName())
      );
    }
  }

  /**
   * Encodes a PreimageSha256Condition using ASN.1 DER encoding.
   *
   * @param condition A {@link PreimageSha256Condition} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code condition}.
   */
  private static byte[] writeSingleCondition(final SimpleCondition condition) {
    try {
      // Build Fingerprint and Cost SEQUENCE
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DerOutputStream out = new DerOutputStream(baos);
      out.writeTaggedObject(0, condition.getFingerprint());
      out.writeTaggedObject(1, BigInteger.valueOf(condition.getCost()).toByteArray());
      out.close();
      byte[] buffer = baos.toByteArray();

      // Wrap CHOICE
      baos = new ByteArrayOutputStream();
      out = new DerOutputStream(baos);
      out.writeEncoded(
          DerTag.CONSTRUCTED.getTag() + DerTag.TAGGED.getTag() + condition.getType().getTypeCode(),
          buffer);
      out.close();
      return baos.toByteArray();
    } catch (IOException ioe) {
      throw new UncheckedIOException("DER Encoding Error.", ioe);
    }
  }

  /**
   * Encodes a PreimageSha256Condition using ASN.1 DER encoding.
   *
   * @param condition A {@link PreimageSha256Condition} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code condition}.
   */
  private static byte[] writeCompoundCondition(final CompoundCondition condition)
      throws DerEncodingException {
    try {
      // Build Fingerprint and Cost SEQUENCE
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DerOutputStream out = new DerOutputStream(baos);
      out.writeTaggedObject(0, condition.getFingerprint());
      out.writeTaggedObject(1, BigInteger.valueOf(condition.getCost()).toByteArray());

      // Encode Subconditions...
      byte[] bitStringData = CryptoConditionType.getEnumOfTypesAsBitString(condition.getSubtypes());
      out.writeTaggedObject(2, bitStringData);

      out.close();
      byte[] buffer = baos.toByteArray();

      // Wrap CHOICE
      baos = new ByteArrayOutputStream();
      out = new DerOutputStream(baos);
      out.writeEncoded(
          DerTag.CONSTRUCTED.getTag() + DerTag.TAGGED.getTag() + condition.getType().getTypeCode(),
          buffer);
      out.close();
      return baos.toByteArray();
    } catch (IOException ioe) {
      throw new UncheckedIOException("DER Encoding Error.", ioe);
    }
  }


  /**
   * Encodes a PreimageSha256Fulfillment using ASN.1 DER encoding.
   *
   * @param fulfillment A {@link PreimageSha256Fulfillment} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code fulfillment}.
   */
  private static byte[] writeTypedFulfillment(final PreimageSha256Fulfillment fulfillment) {

    try {
      // Build preimage sequence
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DerOutputStream out = new DerOutputStream(baos);
      out.writeTaggedObject(0,
          Base64.getUrlDecoder().decode(fulfillment.getEncodedPreimage()));
      out.close();
      byte[] buffer = baos.toByteArray();

      // Wrap CHOICE
      baos = new ByteArrayOutputStream();
      out = new DerOutputStream(baos);
      out.writeTaggedConstructedObject(fulfillment.getType().getTypeCode(), buffer);
      out.close();

      return baos.toByteArray();

    } catch (IOException e) {
      throw new UncheckedIOException("DER Encoding Error", e);
    }

  }

  /**
   * Encodes a PrefixSha256Fulfillment using ASN.1 DER encoding.
   *
   * @param fulfillment A {@link PrefixSha256Fulfillment} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code fulfillment}.
   */
  private static byte[] writeTypedFulfillment(final PrefixSha256Fulfillment fulfillment)
      throws DerEncodingException {

    try {
      // Build prefix sequence
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DerOutputStream out = new DerOutputStream(baos);
      out.writeTaggedObject(0, fulfillment.getPrefix());
      out.writeTaggedObject(1, BigInteger.valueOf(fulfillment.getMaxMessageLength()).toByteArray());
      out.writeTaggedConstructedObject(2, writeFulfillment(fulfillment.getSubfulfillment()));
      out.close();
      byte[] buffer = baos.toByteArray();

      // Wrap CHOICE
      baos = new ByteArrayOutputStream();
      out = new DerOutputStream(baos);
      out.writeTaggedConstructedObject(fulfillment.getType().getTypeCode(), buffer);
      out.close();

      return baos.toByteArray();

    } catch (IOException ioe) {
      throw new UncheckedIOException("DER Encoding Error", ioe);
    }
  }

  /**
   * Encodes a RsaSha256Fulfillment using ASN.1 DER encoding.
   *
   * @param fulfillment A {@link RsaSha256Fulfillment} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code fulfillment}.
   */
  private static byte[] writeTypedFulfillment(final RsaSha256Fulfillment fulfillment)
      throws DerEncodingException {
    try {
      // Build preimage sequence
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DerOutputStream out = new DerOutputStream(baos);
      out.writeTaggedObject(0,
          UnsignedBigInteger.toUnsignedByteArray(fulfillment.getPublicKey().getModulus()));
      out.writeTaggedObject(1, fulfillment.getSignature());
      out.close();
      byte[] buffer = baos.toByteArray();

      // Wrap CHOICE
      baos = new ByteArrayOutputStream();
      out = new DerOutputStream(baos);
      out.writeTaggedConstructedObject(fulfillment.getType().getTypeCode(), buffer);
      out.close();

      return baos.toByteArray();

    } catch (IOException e) {
      throw new UncheckedIOException("DER Encoding Error", e);
    }
  }

  /**
   * Encodes a Ed25519Sha256Fulfillment using ASN.1 DER encoding.
   *
   * @param fulfillment A {@link Ed25519Sha256Fulfillment} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code fulfillment}.
   */
  private static byte[] writeTypedFulfillment(final Ed25519Sha256Fulfillment fulfillment)
      throws DerEncodingException {
    try {
      // Build preimage sequence
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DerOutputStream out = new DerOutputStream(baos);
      out.writeTaggedObject(0, fulfillment.getPublicKey().getA().toByteArray());
      out.writeTaggedObject(1, fulfillment.getSignature());
      out.close();
      byte[] buffer = baos.toByteArray();

      // Wrap CHOICE
      baos = new ByteArrayOutputStream();
      out = new DerOutputStream(baos);
      out.writeTaggedConstructedObject(fulfillment.getType().getTypeCode(), buffer);
      out.close();

      return baos.toByteArray();

    } catch (IOException e) {
      throw new UncheckedIOException("DER Encoding Error", e);
    }
  }

  /**
   * Encodes a ThresholdSha256Fulfillment using ASN.1 DER encoding.
   *
   * @param fulfillment A {@link ThresholdSha256Fulfillment} to encode.
   *
   * @return A byte array containing the ASN.1 DER encoding of the supplied {@code fulfillment}.
   */
  private static byte[] writeTypedFulfillment(final ThresholdSha256Fulfillment fulfillment)
      throws DerEncodingException {

    // Preemptively load all condition/subconditions so that encoding works properly.
    //fulfillment.getCondition();
    //Arrays.stream(fulfillment.getSubfulfillments()).forEach(Fulfillment::getCondition);

    try {
      // Build subfulfillment sequence
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      for (int i = 0; i < fulfillment.getSubfulfillments().size(); i++) {
        baos.write(writeFulfillment(fulfillment.getSubfulfillments().get(i)));
      }
      baos.close();
      byte[] fulfillmentsBuffer = baos.toByteArray();

      // Wrap SET OF
      baos = new ByteArrayOutputStream();
      DerOutputStream out = new DerOutputStream(baos);
      out.writeTaggedConstructedObject(0, fulfillmentsBuffer);
      out.close();
      fulfillmentsBuffer = baos.toByteArray();

      // Build subcondition sequence
      baos = new ByteArrayOutputStream();
      for (int i = 0; i < fulfillment.getSubconditions().size(); i++) {
        baos.write(writeCondition(fulfillment.getSubconditions().get(i)));
      }
      out.close();
      byte[] conditionsBuffer = baos.toByteArray();

      // Wrap SET OF
      baos = new ByteArrayOutputStream();
      out = new DerOutputStream(baos);
      out.writeTaggedConstructedObject(1, conditionsBuffer);
      out.close();
      conditionsBuffer = baos.toByteArray();

      byte[] buffer = new byte[fulfillmentsBuffer.length + conditionsBuffer.length];
      System.arraycopy(fulfillmentsBuffer, 0, buffer, 0, fulfillmentsBuffer.length);
      System.arraycopy(conditionsBuffer, 0, buffer, fulfillmentsBuffer.length,
          conditionsBuffer.length);

      // Wrap CHOICE
      baos = new ByteArrayOutputStream();
      out = new DerOutputStream(baos);
      out.writeTaggedConstructedObject(fulfillment.getType().getTypeCode(), buffer);
      out.close();

      return baos.toByteArray();

    } catch (IOException ioe) {
      throw new UncheckedIOException("DER Encoding Error", ioe);
    }

  }

}
