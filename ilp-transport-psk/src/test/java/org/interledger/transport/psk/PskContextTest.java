package org.interledger.transport.psk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.interledger.InterledgerAddress;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.mocks.DeterministicSecureRandomProvider;

import com.google.common.io.BaseEncoding;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Base64;
import java.util.UUID;

public class PskContextTest {

  private static final byte[] TEST_SECRET = BaseEncoding.base16()
      .decode("33798C3E0ACCA99F310041C4DEF1021FBC058C3ABCF3AEF04BAC00F12D3DD102");
  private static final byte[] TEST_TOKEN = BaseEncoding.base16()
      .decode("15671C99FE1450EA5A2265D2AE20923C");
  private static final byte[] TEST_RECEIVER_ID = BaseEncoding.base16()
      .decode("390082B4F82F0CB6");
  private static final byte[] TEST_SHARED_KEY = BaseEncoding.base16()
      .decode("C1FEA42E22F46D6C7D8F78A3649748DCB7567B9847425AD43D31C561DA3B9C19");
  private static final byte[] TEST_ENCRYPTION_KEY_DATA = BaseEncoding.base16()
      .decode("3AAB595DF4F2BCD83B4041F8202582717EC6528DC21CC1BB932D049964C5E143");
  private static final byte[] TEST_FULFILLMENT_MAC_KEY = BaseEncoding.base16()
      .decode("3C1CB375BB0C4F036FC5B3E632789C882F82F65BCD62375572AEDA2A0EC6C8D6");
  private static final byte[] TEST_PREIMAGE = BaseEncoding.base16()
      .decode("E622510588B453C593E68A2C0AFA944D2E9561D9E0F9A5533B9A9454BD6057A1");
  private static final byte[] TEST_MESSAGE = BaseEncoding.base16()
      .decode("50534B2F312E300A5061796D656E742D49643A36393333383137312D35323430"
          + "2D343339322D386337372D6434313762643937373936320A4E6F6E63653A6F5A"
          + "735F46697132464C6441684537757178367067670A456E6372797074696F6E3A"
          + "6E6F6E650A0A5345435245543A53545546460A0AA249C34D9B1050D0CF7A2141"
          + "577BDE1E");
  private static final byte[] TEST_AUTHENTICATION_TAG = BaseEncoding.base16()
      .decode("D8238DD449E4A11224206857988A8A49");
  private static final byte[] TEST_NONCE = BaseEncoding.base16()
      .decode("A19B3F162AB614B740844EEEAB1EA982");
  private static final String TEST_PAYMENT_ID = "69338171-5240-4392-8c77-d417bd977962";
  private static final String TEST_PRIVATE_HEADER_N = "SECRET";
  private static final String TEST_PRIVATE_HEADER_V = "STUFF";
  private static final byte[] TEST_DATA = BaseEncoding.base16()
      .decode("A249C34D9B1050D0CF7A2141577BDE1E");
  private static final byte[] ENCRYPTED_TEST_DATA = BaseEncoding.base16()
      .decode("3FEC201E1CB2A95F7E44068E5A5B12F6682DBC168234209EDEA7406516C3");
  private static final InterledgerAddress TEST_ADDRESS_PREFIX = InterledgerAddress.of(
      "test1.bob");
  private static final InterledgerAddress TEST_ADDRESS = InterledgerAddress.of(
      "test1.bob.OQCCtPgvDLYFWccmf4UUOpaImXSriCSPA");

  private void assertContextIsValid(PskContext context) {

    assertArrayEquals("Invalid token", TEST_TOKEN, context.getToken());
    assertArrayEquals("Invalid Receiver ID.", TEST_RECEIVER_ID, context.getReceiverId());
    assertArrayEquals("Invalid Shared key", TEST_SHARED_KEY, context.getSharedKey());
    assertArrayEquals("Invalid Encryption key content", TEST_ENCRYPTION_KEY_DATA,
        context.getEncryptionKey()
            .getEncoded());
    assertEquals("Invalid Encryption key algorithm", "AES",
        context.getEncryptionKey()
            .getAlgorithm());
    assertArrayEquals("Invalid Fulfillment HMAC key", TEST_FULFILLMENT_MAC_KEY,
        context.getFulfillmentHmacKey());

  }


  @Test
  public final void testFromSeed() {

    DeterministicSecureRandomProvider.setAsDefault(TEST_TOKEN);

    assertContextIsValid(PskContext.seed(TEST_SECRET));

    DeterministicSecureRandomProvider.remove();

  }

  @Test
  public final void testFromReceiverAddress() {

    assertContextIsValid(PskContext.fromReceiverAddress(TEST_SECRET, TEST_ADDRESS));

  }

  @Test
  public final void testDecryptMessage() {

    PskMessage encryptedMessage = PskMessage.builder()
        .addPublicHeader(PskEncryptionHeader.aesGcm(TEST_AUTHENTICATION_TAG))
        .addPublicHeader(PskNonceHeader.fromNonce(TEST_NONCE))
        .data(ENCRYPTED_TEST_DATA)
        .build();

    PskMessage decryptedMessage =
        PskContext.fromToken(TEST_SECRET, TEST_TOKEN)
            .decryptMessage(encryptedMessage);

    assertEquals("Invalid encryption type.", PskEncryptionType.NONE,
        decryptedMessage.getEncryptionHeader()
            .getEncryptionType());
    assertEquals("Incorrect number of private headers.", 1,
        decryptedMessage.getPrivateHeaders()
            .size());
    assertArrayEquals("Incorrect data.", decryptedMessage.getData(), TEST_DATA);

  }

  @Test
  public final void testEncryptMessage() {
    PskMessage decryptedMessage = PskMessage.builder()
        .addPublicHeader(PskEncryptionHeader.none())
        .addPublicHeader(PskNonceHeader.fromNonce(TEST_NONCE))
        .paymentId(UUID.fromString(TEST_PAYMENT_ID))
        .addPrivateHeader(TEST_PRIVATE_HEADER_N, TEST_PRIVATE_HEADER_V)
        .data(TEST_DATA)
        .build();

    PskMessage encryptedMessage =
        PskContext.fromToken(TEST_SECRET, TEST_TOKEN)
            .encryptMessage(decryptedMessage);

    assertEquals("Invalid encryption type.", PskEncryptionType.AES_256_GCM,
        encryptedMessage.getEncryptionHeader()
            .getEncryptionType());
    assertEquals("Incorrect number of private headers.", 0,
        encryptedMessage.getPrivateHeaders()
            .size());
    assertArrayEquals("Incorrect data.", encryptedMessage.getData(), ENCRYPTED_TEST_DATA);
  }

  @Test
  public final void testGenerateReceiverAddress() {

    PskContext context = PskContext.fromReceiverAddress(TEST_SECRET, TEST_ADDRESS);
    assertEquals("Incorrect address generated.",
        context.generateReceiverAddress(TEST_ADDRESS_PREFIX), TEST_ADDRESS);
  }

  @Test
  public final void testGenerateFulfillment() {

    PskContext context = PskContext.fromReceiverAddress(TEST_SECRET, TEST_ADDRESS);

    InterledgerPayment payment = InterledgerPayment.builder()
        .destinationAccount(TEST_ADDRESS)
        .destinationAmount(BigInteger.valueOf(100L))
        .data(TEST_MESSAGE)
        .build();

    Fulfillment fulfillment = context.generateFulfillment(payment);

    Assert.assertEquals("Incorrect fulfillment.",
            ((PreimageSha256Fulfillment) fulfillment).getPreimage(),
            //TODO Fix crypto-conditions to use Bas64Url without padding
            //Base64.getUrlEncoder().withoutPadding().encodeToString(TEST_PREIMAGE));
            Base64.getUrlEncoder().encodeToString(TEST_PREIMAGE));

  }


}
