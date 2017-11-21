package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.der.DerEncodingException;
import org.interledger.cryptoconditions.der.DerInputStream;
import org.interledger.cryptoconditions.der.DerTag;
import org.interledger.cryptoconditions.utils.UnsignedBigInteger;

import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides utility methods to read a crypto-condition from DER encoding.
 */
public class CryptoConditionReader {

  public static final String ED_25519 = "Ed25519";

  /**
   * Reads a DER encoded condition from the buffer.
   *
   * @param buffer contains the raw DER encoded condition.
   * @return The condition read from the buffer.
   * @throws DerEncodingException when DER encoding fails for any reason.
   */
  public static Condition readCondition(byte[] buffer) throws DerEncodingException {
    return readCondition(buffer, 0, buffer.length);
  }

  /**
   * Reads a DER encoded condition from the buffer.
   *
   * @param buffer contains the raw DER encoded condition.
   * @param offset the position within the buffer to begin reading the condition.
   * @param length the number of bytes to read.
   * @return The condition read from the buffer.
   * @throws DerEncodingException when DER encoding fails for any reason.
   */
  public static Condition readCondition(byte[] buffer, int offset, int length)
      throws DerEncodingException {

    ByteArrayInputStream bais = new ByteArrayInputStream(buffer, offset, length);
    DerInputStream in = new DerInputStream(bais);

    try {
      return readCondition(in);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    } finally {
      try {
        in.close();
      } catch (IOException ioe) {
        throw new UncheckedIOException(ioe);
      }
    }
  }

  /**
   * Reads a DER encoded condition from the input stream.
   *
   * @param in The input stream containing the DER encoded condition.
   * @return The condition read from the stream.
   * @throws DerEncodingException when DER encoding fails for any reason.
   * @throws IOException          if any I/O operation fails for any reason.
   */
  public static Condition readCondition(DerInputStream in)
      throws DerEncodingException, IOException {
    return readCondition(in, new AtomicInteger());
  }

  /**
   * Reads a DER encoded condition from the input stream.
   *
   * @param in        The input stream containing the DER encoded condition.
   * @param bytesRead will be updated with the number of bytes read from the stream.
   * @return The condition read from the stream.
   * @throws DerEncodingException when DER encoding fails for any reason.
   * @throws IOException          if any I/O operation fails for any reason.
   */
  public static Condition readCondition(DerInputStream in, AtomicInteger bytesRead)
      throws DerEncodingException, IOException {

    int tag = in.readTag(bytesRead, DerTag.CONSTRUCTED, DerTag.TAGGED);
    CryptoConditionType type = CryptoConditionType.valueOf(tag);
    int length = in.readLength(bytesRead);

    AtomicInteger innerBytesRead = new AtomicInteger();
    byte[] fingerprint =
        in.readTaggedObject(0, length - innerBytesRead.get(), innerBytesRead).getValue();
    long cost = new BigInteger(
        in.readTaggedObject(1, length - innerBytesRead.get(), innerBytesRead).getValue())
        .longValue();
    EnumSet<CryptoConditionType> subtypes = null;
    if (type == CryptoConditionType.PREFIX_SHA256 || type == CryptoConditionType.THRESHOLD_SHA256) {
      subtypes = CryptoConditionType.getEnumOfTypesFromBitString(
          in.readTaggedObject(2, length - innerBytesRead.get(), innerBytesRead).getValue());
    }
    bytesRead.addAndGet(innerBytesRead.get());

    switch (type) {
      case PREIMAGE_SHA256:
        return new PreimageSha256Condition(cost, fingerprint);
      case PREFIX_SHA256:
        return new PrefixSha256Condition(cost, fingerprint, subtypes);
      case THRESHOLD_SHA256:
        return new ThresholdSha256Condition(cost, fingerprint, subtypes);
      case RSA_SHA256:
        return new RsaSha256Condition(cost, fingerprint);
      case ED25519_SHA256:
        return new Ed25519Sha256Condition(fingerprint);
      default:
        throw new DerEncodingException("Unknown condition type: " + type);
    }
  }

  /**
   * Reads a DER encoded fulfillment from the buffer.
   *
   * @param buffer The buffer holding the DER encoded fulfillment
   * @return The fulfillment read from the buffer.
   * @throws DerEncodingException when DER encoding fails for any reason.
   */
  public static Fulfillment readFulfillment(byte[] buffer) throws DerEncodingException {
    return readFulfillment(buffer, 0, buffer.length);
  }

  /**
   * Reads a DER encoded fulfillment from the buffer.
   *
   * @param buffer The buffer holding the DER encoded fulfillment
   * @param offset the position within the buffer to begin reading the fulfilment.
   * @param length the number of bytes to read.
   * @return The fulfillment read from the buffer.
   * @throws DerEncodingException when DER encoding fails for any reason.
   */
  public static Fulfillment readFulfillment(byte[] buffer, int offset, int length)
      throws DerEncodingException {

    ByteArrayInputStream bais = new ByteArrayInputStream(buffer, offset, length);
    DerInputStream in = new DerInputStream(bais);

    try {
      return readFulfillment(in);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      try {
        in.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  /**
   * Reads a DER encoded fulfillment from the input stream.
   *
   * @param in The input stream containing the DER encoded fulfillment.
   * @return The fulfillment read from the stream.
   * @throws DerEncodingException when DER encoding fails for any reason.
   * @throws IOException          if any I/O operation fails for any reason.
   */
  public static Fulfillment readFulfillment(DerInputStream in)
      throws DerEncodingException, IOException {
    return readFulfillment(in, new AtomicInteger());
  }

  /**
   * Reads a DER encoded fulfillment from the input stream.
   *
   * @param in        The input stream containing the DER encoded fulfillment.
   * @param bytesRead will be updated with the number of bytes read from the stream.
   * @return The fulfillment read from the stream.
   * @throws DerEncodingException when DER encoding fails for any reason.
   * @throws IOException          if any I/O operation fails for any reason.
   */
  public static Fulfillment readFulfillment(DerInputStream in, AtomicInteger bytesRead)
      throws DerEncodingException, IOException {

    int tag = in.readTag(bytesRead, DerTag.CONSTRUCTED, DerTag.TAGGED);
    CryptoConditionType type = CryptoConditionType.valueOf(tag);
    int length = in.readLength(bytesRead);

    if (length == 0) {
      throw new DerEncodingException("Encountered an empty fulfillment.");
    }

    AtomicInteger innerBytesRead = new AtomicInteger();
    switch (type) {
      case PREIMAGE_SHA256:

        byte[] preimage =
            in.readTaggedObject(0, length - innerBytesRead.get(), innerBytesRead).getValue();

        bytesRead.addAndGet(innerBytesRead.get());

        return new PreimageSha256Fulfillment(preimage);

      case PREFIX_SHA256:

        final byte[] prefix =
            in.readTaggedObject(0, length - innerBytesRead.get(), innerBytesRead).getValue();
        final long maxMessageLength = new BigInteger(
            in.readTaggedObject(1, length - innerBytesRead.get(), innerBytesRead).getValue())
            .longValue();

        in.readTag(2, innerBytesRead, DerTag.CONSTRUCTED, DerTag.TAGGED);
        in.readLength(innerBytesRead);

        Fulfillment subfulfillment = readFulfillment(in, innerBytesRead);

        bytesRead.addAndGet(innerBytesRead.get());

        return new PrefixSha256Fulfillment(prefix, maxMessageLength, subfulfillment);

      case THRESHOLD_SHA256:

        List<Fulfillment> subfulfillments = new ArrayList<>();

        tag = in.readTag(innerBytesRead, DerTag.CONSTRUCTED, DerTag.TAGGED);
        length = in.readLength(innerBytesRead);

        // It is legal (per the encoding rules) for a THRESHOLD fulfillment to have only
        // sub-conditions even though it will never verify so we need to check if we've
        // skipped tag number 0
        if (tag == 0) {

          AtomicInteger subfulfillmentsBytesRead = new AtomicInteger();
          while (subfulfillmentsBytesRead.get() < length) {
            subfulfillments.add(readFulfillment(in, subfulfillmentsBytesRead));
          }
          innerBytesRead.addAndGet(subfulfillmentsBytesRead.get());

          in.readTag(1, innerBytesRead, DerTag.CONSTRUCTED, DerTag.TAGGED);
          length = in.readLength(innerBytesRead);

        } else if (tag != 1) {
          throw new DerEncodingException("Expected tag: 1, got: " + tag);
        }

        List<Condition> subconditions = new ArrayList<>();

        AtomicInteger subconditionsBytesRead = new AtomicInteger();
        while (subconditionsBytesRead.get() < length) {
          subconditions.add(readCondition(in, subconditionsBytesRead));
        }
        innerBytesRead.addAndGet(subconditionsBytesRead.get());

        bytesRead.addAndGet(innerBytesRead.get());

        return new ThresholdSha256Fulfillment(subconditions, subfulfillments);

      case RSA_SHA256:

        final BigInteger modulus = UnsignedBigInteger.fromUnsignedByteArray(
            in.readTaggedObject(0, length - innerBytesRead.get(), innerBytesRead).getValue()
        );

        final byte[] rsaSignature = in.readTaggedObject(
            1, length - innerBytesRead.get(), innerBytesRead
        ).getValue();

        bytesRead.addAndGet(innerBytesRead.get());

        final RSAPublicKeySpec rsaSpec = new RSAPublicKeySpec(
            modulus, RsaSha256Fulfillment.PUBLIC_EXPONENT
        );

        try {
          final KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
          final PublicKey publicKey = rsaKeyFactory.generatePublic(rsaSpec);

          return new RsaSha256Fulfillment((RSAPublicKey) publicKey, rsaSignature);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
          throw new RuntimeException("Error creating RSA key.", e);
        }

      case ED25519_SHA256:
        byte[] ed25519key =
            in.readTaggedObject(0, length - innerBytesRead.get(), innerBytesRead).getValue();
        byte[] ed25519Signature =
            in.readTaggedObject(1, length - innerBytesRead.get(), innerBytesRead).getValue();

        bytesRead.addAndGet(innerBytesRead.get());

        EdDSAPublicKeySpec ed25519spec = new EdDSAPublicKeySpec(ed25519key,
            EdDSANamedCurveTable.getByName(ED_25519));
        EdDSAPublicKey ed25519PublicKey = new EdDSAPublicKey(ed25519spec);

        return new Ed25519Sha256Fulfillment(ed25519PublicKey, ed25519Signature);

      default:
        throw new DerEncodingException("Unrecogized condition type: " + type);
    }
  }
}
