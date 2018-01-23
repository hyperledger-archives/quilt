package org.interledger.transport.psk;

import org.interledger.InterledgerAddress;
import org.interledger.InterledgerRuntimeException;
import org.interledger.codecs.InterledgerCodecContext;
import org.interledger.codecs.InterledgerCodecContextFactory;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.transport.psk.PskCryptoUtils.AesGcmEncryptResult;
import org.interledger.transport.psk.PskMessage.Header.WellKnown;
import org.interledger.transport.psk.codecs.PskMessageBinaryCodec;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public interface PskContext {

  /* the expected length of the auth tag, in bytes */
  int AUTH_TAG_LEN_BYTES = 16;
  /* the expected length of the receiver secret, in bytes */
  int RECEIVER_SECRET_LEN_BYTES = 32;
  /* the expected length of the token, in bytes */
  int TOKEN_LEN_BYTES = 16;
  /* the expected length of the receiver id, in bytes */
  int RECEIVER_ID_LEN_BYTES = 8;
  /* the expected length of the shared key, in bytes */
  int SHARED_KEY_LEN_BYTES = 32;
  /* the expected length of the encryption key, in bytes */
  int ENCRYPTION_KEY_LEN_BYTES = 32;
  /* the expected length of the encryption key, in bytes */
  int FULFILLMENT_KEY_LEN_BYTES = 32;


  /* constant used to generate receiver id of secret */
  String IPR_RECEIVER_ID_STRING = "ilp_psk_receiver_id";
  /* constant used to generate receiver id of secret */
  String PSK_GENERATION_STRING = "ilp_psk_generation";
  /* constant used to generate receiver id of secret */
  String PSK_CONDITION_STRING = "ilp_psk_condition";
  /* constant used to generate receiver id of secret */
  String PSK_ENCRYPTION_STRING = "ilp_key_encryption";
  /*
   * the cipher spec used for encryption and decryption. Note that the RFC calls for PKCS-7, which
   * wikipedia claims is equivalent to PKCS-5
   */
  String CIPHER_SPEC = "AES/GCM/PKCS5Padding";
  /* HMAC SHA-256 spec */
  String HMAC_ALGORITHM = "HmacSHA256";

  /**
   * Create a new receiver context with a new random token.
   *
   * <p>By default the context will not use encryption
   *
   * @param receiverSecret The receiver's local secret
   *
   * @return A new PSK Context initialized with a random token
   */
  static PskContext seed(byte[] receiverSecret) {
    Objects.requireNonNull(receiverSecret, "receiverSecret must not be null");

    if (receiverSecret.length != RECEIVER_SECRET_LEN_BYTES) {
      throw new IllegalArgumentException("Invalid secret. Expected " + RECEIVER_SECRET_LEN_BYTES
          + " bytes " + " but got " + receiverSecret.length);
    }

    return new ReceiverPskContext(receiverSecret);
  }

  /**
   * Create a new receiver context based on the given Interledger Payment address.
   *
   * <p>This will attempt to parse the token of the payment address.
   *
   * @param receiverSecret The receiver's local secret
   * @param address        The destination address of the incoming payment
   *
   * @return A new PSK Context initialized with a token parsed of the provided address
   */
  static PskContext fromReceiverAddress(byte[] receiverSecret, InterledgerAddress address) {

    Objects.requireNonNull(receiverSecret, "receiverSecret must not be null");
    Objects.requireNonNull(address, "address must not be null");

    if (receiverSecret.length != RECEIVER_SECRET_LEN_BYTES) {
      throw new IllegalArgumentException("Invalid secret. Expected " + RECEIVER_SECRET_LEN_BYTES
          + " bytes " + " but got " + receiverSecret.length);
    }

    // Is this a PSK payment for this receiver?
    final byte[] receiverId = generateReceiverId(receiverSecret);
    final String receiverIdBase64Url =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(receiverId);
    final String receiverAddress = address.getValue();
    final String addressSuffix = receiverAddress.substring(receiverAddress.lastIndexOf(".") + 1);

    if (!addressSuffix.startsWith(receiverIdBase64Url)) {
      throw new RuntimeException(
          "Invalid destination address [" + receiverAddress + "] expecting to find receiverId ["
              + receiverIdBase64Url + "] at start of final segment.");
    }

    // Extract token of address
    String tokenBase64Url = addressSuffix.substring(receiverIdBase64Url.length());
    byte[] token = Base64.getUrlDecoder()
        .decode(tokenBase64Url);

    if (token.length != TOKEN_LEN_BYTES) {
      throw new RuntimeException("Invalid token [" + tokenBase64Url
          + "] found in destination address. Expected 16 bytes Base64Url encoded.");
    }

    return new ReceiverPskContext(receiverSecret, token);
  }

  /**
   * Create a new receiver context based on the given token.
   *
   * @param receiverSecret The receiver's local secret
   * @param token          The token to use to initialize the context (must be 16 bytes)
   *
   * @return A new PSK Context initialized with the given token
   */
  static PskContext fromToken(byte[] receiverSecret, byte[] token) {

    Objects.requireNonNull(receiverSecret, "receiverSecret must not be null");
    Objects.requireNonNull(token, "token must not be null");

    if (receiverSecret.length != RECEIVER_SECRET_LEN_BYTES) {
      throw new IllegalArgumentException("Invalid secret. Expected " + RECEIVER_SECRET_LEN_BYTES
          + " bytes " + " but got " + receiverSecret.length);
    }

    if (token.length != TOKEN_LEN_BYTES) {
      throw new IllegalArgumentException(
          "Invalid token. Expected " + TOKEN_LEN_BYTES + " bytes " + " but got " + token.length);
    }

    return new ReceiverPskContext(receiverSecret, token);
  }

  /**
   * Create a new sender context using the provided pre-shared key.
   *
   * @param preSharedKey The key shared with the sender
   *
   * @return a new context
   */
  static PskContext fromPreSharedKey(byte[] preSharedKey) {
    Objects.requireNonNull(preSharedKey, "preSharedKey must not be null");

    if (preSharedKey.length != SHARED_KEY_LEN_BYTES) {
      throw new IllegalArgumentException("Invalid key. Expected " + SHARED_KEY_LEN_BYTES + " bytes "
          + " but got " + preSharedKey.length);
    }

    return new SenderPskContext(preSharedKey);
  }

  /**
   * Deterministically derive an encryption key of a shared key.
   *
   * @param sharedKey The shared key
   *
   * @return a new AES encryption key derived of the shared key
   */
  static SecretKey generateEncryptionKey(byte[] sharedKey) {
    return new SecretKeySpec(
        hmacSha256(sharedKey, PSK_ENCRYPTION_STRING.getBytes(StandardCharsets.UTF_8)), "AES");
  }

  /**
   * Deterministically derive a fulfillment HMAC key of a shared key.
   *
   * @param sharedKey The shared key
   *
   * @return a byte array to use as the key when getting an HMAC of a payment packet as a preimage
   */
  static byte[] generateFulfillmentHmacKey(byte[] sharedKey) {
    return hmacSha256(sharedKey, PSK_CONDITION_STRING.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Perform a SHA 256 HMAC.
   *
   * @param key     The HMAC key to use (must be 32 bytes);
   * @param message The data to hash
   *
   * @return the SHA 256 HMAC of the provided data
   */
  static byte[] hmacSha256(byte[] key, byte[] message) {

    Objects.requireNonNull(key);
    Objects.requireNonNull(message);

    if (key.length != 32) {
      throw new RuntimeException("Invalid SHA-256 HMAC key. Must be 32 bytes.");
    }

    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
      return mac.doFinal(message);
    } catch (InvalidKeyException | NoSuchAlgorithmException | IllegalStateException e) {
      throw new InterledgerRuntimeException("Error getting HMAC", e);
    }
  }

  /**
   * Deterministically generate a shared key of a given receiver secret and a token.
   *
   * @param receiverSecret The local receiver secret
   * @param token          A random token
   *
   * @return The key that can be shared with the sender in the PSK protocol
   */
  static byte[] generatePreSharedKey(byte[] receiverSecret, byte[] token) {
    byte[] generator =
        hmacSha256(receiverSecret, PSK_GENERATION_STRING.getBytes(StandardCharsets.UTF_8));
    return Arrays.copyOf(hmacSha256(generator, token), SHARED_KEY_LEN_BYTES);
  }

  /**
   * Generate a strong random 16 byte token using the system provided {@link SecureRandom}.
   *
   * @return a random 16 byte token
   */
  static byte[] generateToken() {

    try {
      SecureRandom sr = SecureRandom.getInstanceStrong();
      byte[] token = new byte[16];
      sr.nextBytes(token);
      return token;
    } catch (NoSuchAlgorithmException nsa) {
      throw new InterledgerRuntimeException("Could not generate token", nsa);
    }
  }

  /**
   * Deterministically generate a receiver id of a given secret.
   *
   * @param receiverSecret The local receiver secret
   *
   * @return the 8 byte receiver id
   */
  static byte[] generateReceiverId(byte[] receiverSecret) {
    return Arrays.copyOf(
        hmacSha256(receiverSecret, IPR_RECEIVER_ID_STRING.getBytes(StandardCharsets.UTF_8)),
        RECEIVER_ID_LEN_BYTES);
  }

  /**
   * Decrypt a PSK Message.
   *
   * <p>Decrypts the data and private headers using AES 256 GCM and the key of this context.
   *
   * @param message An encrypted PSK message
   *
   * @return the decrypted PSK message (with private headers added)
   */
  PskMessage decryptMessage(PskMessage message);

  /**
   * Encrypts a PSK Message.
   *
   * <p>Encrypts the data and private headers using AES 256 GCM and the key of this context.
   *
   * @param message An unencrypted PSK Message
   *
   * @return A new message with AES-GCM encryption header and private headers and data encrypted.
   */
  PskMessage encryptMessage(PskMessage message);

  /**
   * Generate a fulfillment of a given Interledger Payment.
   *
   * <p>This will encode the payment to a byte array and then HMAC the data using a key derived of
   * the pre-shared key.
   *
   * @param payment The payment for which a fulfillment is required.
   *
   * @return The fulfillment for the given payment and this context.
   */
  Fulfillment generateFulfillment(InterledgerPayment payment);

  /**
   * Generate a new address based on the given address by appending a new segment with the receiver
   * ID and token of this context.
   *
   * @param address Destination address without PSK suffix
   *
   * @return new address with PSK suffix added
   */
  InterledgerAddress generateReceiverAddress(InterledgerAddress address);

  /**
   * Get the token for this context.
   *
   * @return the token.
   *
   * @throws RuntimeException if the current context is for a sender.
   */
  byte[] getToken();

  /**
   * Get the receiver id for this context.
   *
   * @return the context.
   *
   * @throws RuntimeException if the current context is for a sender.
   */
  byte[] getReceiverId();

  /**
   * Get the shared key for this context.
   *
   * @return the shared key.
   */
  byte[] getSharedKey();

  /**
   * Get the encryption key for this context.
   *
   * @return the the AES key derived of the shared key.
   */
  SecretKey getEncryptionKey();

  /**
   * Get the HMAC key for generating fulfillments for this context.
   *
   * @return the fulfillment HMAC key derived of the shared key.
   */
  byte[] getFulfillmentHmacKey();

  class SenderPskContext implements PskContext {

    private final byte[] sharedKey;
    private final byte[] fulfillmentHmacKey;
    private final SecretKey encryptionKey;

    protected SenderPskContext(byte[] preSharedKey) {

      Objects.requireNonNull(preSharedKey);
      if (preSharedKey.length != SHARED_KEY_LEN_BYTES) {
        throw new IllegalArgumentException("Illegal pre-shared key. " + "Key is the wrong size ["
            + preSharedKey.length + "]. " + "Expected [" + SHARED_KEY_LEN_BYTES + "].");
      }

      this.sharedKey = Arrays.copyOf(preSharedKey, preSharedKey.length);
      this.encryptionKey = generateEncryptionKey(this.sharedKey);
      this.fulfillmentHmacKey = generateFulfillmentHmacKey(this.sharedKey);
    }

    @Override
    public PskMessage decryptMessage(PskMessage message) {

      Objects.requireNonNull(message);

      PskEncryptionHeader encryptionHeader = message.getEncryptionHeader();
      PskNonceHeader nonceHeader = message.getNonceHeader();

      Objects.requireNonNull(encryptionHeader, "Message has no encryption header.");
      Objects.requireNonNull(nonceHeader, "Message has no nonce header.");

      if (encryptionHeader.getEncryptionType() == PskEncryptionType.NONE) {
        throw new IllegalArgumentException("Message is not encrypted.");
      }

      byte[] decryptedData = PskCryptoUtils.decryptPskData(encryptionKey,
          encryptionHeader.getAuthenticationTag(), nonceHeader.getNonce(), message.getData());

      PskMessage privateMessage = new PskMessageBinaryCodec().parsePrivateData(decryptedData);
      PskMessage.Builder builder = PskMessage.builder();

      message.getPublicHeaders()
          .stream()
          .filter(header -> (!header.getName()
              .equals(WellKnown.ENCRYPTION)
              && !header.getName()
              .equals(WellKnown.NONCE)))
          .forEach(builder::addPublicHeader);

      for (PskMessage.Header privateHeader : privateMessage.getPrivateHeaders()) {
        builder.addPrivateHeader(privateHeader);
      }

      builder.addPublicHeader(PskEncryptionHeader.none());
      builder.addPublicHeader(message.getNonceHeader());

      builder.data(privateMessage.getData());
      return builder.build();
    }

    @Override
    public PskMessage encryptMessage(PskMessage message) {

      Objects.requireNonNull(message);

      PskEncryptionHeader encryptionHeader = message.getEncryptionHeader();
      PskNonceHeader nonceHeader = message.getNonceHeader();

      Objects.requireNonNull(encryptionHeader,
          "Message has no encryption header. May already be encrypted.");

      if (nonceHeader == null) {
        nonceHeader = PskNonceHeader.seed();
      }

      if (encryptionHeader.getEncryptionType() != PskEncryptionType.NONE) {
        throw new IllegalArgumentException("Message is already encrypted.");
      }

      byte[] encryptedData = new PskMessageBinaryCodec().writePrivateData(message);

      AesGcmEncryptResult result =
          PskCryptoUtils.encryptPskData(encryptionKey, nonceHeader.getNonce(), encryptedData);

      PskMessage.Builder builder = PskMessage.builder();

      message.getPublicHeaders()
          .stream()
          .filter(header -> (!header.getName()
              .equals(WellKnown.ENCRYPTION)
              && !header.getName()
              .equals(WellKnown.NONCE)))
          .forEach(builder::addPublicHeader);

      builder.addPublicHeader(PskEncryptionHeader.aesGcm(result.getAuthenticationTag()));
      builder.addPublicHeader(nonceHeader);

      builder.data(result.getEncryptedData());
      return builder.build();

    }

    @Override
    public Fulfillment generateFulfillment(InterledgerPayment payment) {
      Objects.requireNonNull(payment);

      // TODO This should not depend on an OER encoded payment packet...
      // Get OER encoded payment packet
      byte[] packet = InterledgerCodecContextFactory.oer()
          .write(InterledgerPayment.class, payment);
      return new PreimageSha256Fulfillment(hmacSha256(fulfillmentHmacKey, packet));
    }

    @Override
    public InterledgerAddress generateReceiverAddress(InterledgerAddress address) {
      throw new RuntimeException("Unable to generate a receiver address of a sender context.");
    }


    @Override
    public byte[] getToken() {
      throw new RuntimeException("Token is not available in a sender context.");
    }


    @Override
    public byte[] getReceiverId() {
      throw new RuntimeException("Token is not available in a sender context.");
    }


    @Override
    public byte[] getSharedKey() {
      return Arrays.copyOf(sharedKey, SHARED_KEY_LEN_BYTES);
    }

    @Override
    public SecretKey getEncryptionKey() {
      return new SecretKeySpec(encryptionKey.getEncoded(), "AES");
    }

    @Override
    public byte[] getFulfillmentHmacKey() {
      return Arrays.copyOf(fulfillmentHmacKey, FULFILLMENT_KEY_LEN_BYTES);
    }

  }

  class ReceiverPskContext extends SenderPskContext {

    private final byte[] token;
    private final byte[] receiverId;


    private ReceiverPskContext(byte[] receiverSecret) {
      this(receiverSecret, generateToken());
    }

    private ReceiverPskContext(byte[] receiverSecret, byte[] token) {
      super(generatePreSharedKey(receiverSecret, token));
      this.token = Arrays.copyOf(token, token.length);
      this.receiverId = generateReceiverId(receiverSecret);
    }

    @Override
    public InterledgerAddress generateReceiverAddress(InterledgerAddress address) {

      final String receiverIdBase64Url =
          Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(receiverId);
      final String tokenBase64Url = Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(token);

      return address.with(receiverIdBase64Url + tokenBase64Url);
    }

    @Override
    public byte[] getToken() {
      return Arrays.copyOf(token, TOKEN_LEN_BYTES);
    }

    @Override
    public byte[] getReceiverId() {
      return Arrays.copyOf(receiverId, RECEIVER_ID_LEN_BYTES);
    }

  }

}
