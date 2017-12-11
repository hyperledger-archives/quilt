package org.interledger.ipr;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.interledger.InterledgerAddress;
import org.interledger.codecs.CodecContext;
import org.interledger.codecs.CodecContextFactory;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.ilp.InterledgerPayment;
import org.interledger.psk.PskContext;
import org.interledger.psk.PskEncryptionType;
import org.interledger.psk.PskMessage;

import org.junit.Test;

import java.math.BigInteger;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.UUID;

/**
 * A test case that simulates the full payment flow using IPR and PSK.
 */
public class InterledgerPaymentRequestEndToEndTest {

  private static final byte[] SECRET = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5,
      6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1};

  @Test
  public final void test() {

    /* Step 1 - Build IPR at receiver. */

    InterledgerAddress destinationAddress = InterledgerAddress.of("private.bob");

    BigInteger destinationAmount = BigInteger.valueOf(100L);
    TemporalAmount expiry = Duration.ofSeconds(60);
    UUID paymentId = UUID.randomUUID();
    String secretStuff = "SECRET";
    byte[] data = new byte[]{0x01, 0x02, 0x03};

    // Load codecs
    CodecContext receiverCodecContextBuildingIpr = CodecContextFactory.interledger();

    // Seed PSK context
    PskContext receiverContextBuildingIpr = PskContext.seed(SECRET);

    //Get PSK (if we were not using IPR we would share this with the sender who could then generate
    //the fulfillment themselves (and derive the encryption key)
    final byte[] psk = receiverContextBuildingIpr.getSharedKey();

    // Build PSK Message
    PskMessage pskMessage = PskMessage.builder()
        .paymentId(paymentId)
        .expiry(expiry)
        .addPrivateHeader("Secret", secretStuff)
        .data(data)
        .build();

    // Encrypt message
    PskMessage encryptedPskMessage = receiverContextBuildingIpr.encryptMessage(pskMessage);
    byte[] encryptedPskMessageData =
        receiverCodecContextBuildingIpr.write(PskMessage.class, encryptedPskMessage);

    // Build ILP Payment Packet
    InterledgerPayment payment = InterledgerPayment.builder()
        .destinationAccount(receiverContextBuildingIpr.generateReceiverAddress(destinationAddress))
        .destinationAmount(destinationAmount)
        .data(encryptedPskMessageData)
        .build();

    // Generate condition
    Condition condition = receiverContextBuildingIpr.generateFulfillment(payment)
        .getCondition();

    // Build IPR
    InterledgerPaymentRequest ipr =
        InterledgerPaymentRequest.builder()
            .interledgerPayment(payment)
            .condition(condition)
            .build();

    // Encode to send...
    byte[] encodedIpr = receiverCodecContextBuildingIpr.write(InterledgerPaymentRequest.class, ipr);

    // Copy encrypted data of PSK message for testing against later
    byte[] encryptedData = encryptedPskMessage.getData();

    /* Step 2 - Parse IPR at Sender. */

    // Load codecs
    CodecContext senderCodecContext = CodecContextFactory.interledger();

    // Decode IPR
    InterledgerPaymentRequest decodedIpr =
        senderCodecContext.read(InterledgerPaymentRequest.class, encodedIpr);

    InterledgerPayment paymentToSend = decodedIpr.getInterledgerPayment();

    // Decode PSK message
    PskMessage message = senderCodecContext.read(PskMessage.class, paymentToSend.getData());

    assertArrayEquals("PSK Message data has changed", encryptedData, message.getData());
    assertEquals("Not encrypted", message.getEncryptionHeader()
            .getEncryptionType(),
        PskEncryptionType.AES_256_GCM);
    assertEquals("Payment ID wrong",
        message.getPublicHeaders(PskMessage.Header.WellKnown.PAYMENT_ID)
            .get(0)
            .getValue(),
        paymentId.toString());

    //Normally the sender wouldn't have the PSK as they were sent an IPR but we can test the
    //encryption here because we do.

    PskContext senderPskContext = PskContext.fromPreSharedKey(psk);
    PskMessage decryptedPskMessageAtSender = senderPskContext.decryptMessage(message);
    assertArrayEquals("Decrypted PSK Message data has changed", data,
        decryptedPskMessageAtSender.getData());
    assertEquals("Not decrypted", PskEncryptionType.NONE,
        decryptedPskMessageAtSender.getEncryptionHeader()
            .getEncryptionType());
    assertEquals("No private headers",
        2,
        decryptedPskMessageAtSender.getPrivateHeaders()
            .size());

    Condition conditionToSend = decodedIpr.getCondition();

    // Encode Payment and Condition to send
    byte[] encodedPayment = senderCodecContext.write(InterledgerPayment.class, paymentToSend);
    byte[] encodedCondition = senderCodecContext.write(Condition.class, conditionToSend);

    /* Step 3 - Parse Payment at receiver. */

    // Load codecs
    CodecContext receiverCodecContext = CodecContextFactory.interledger();

    // Decode Payment, Condition and PSK Message
    InterledgerPayment decodedPayment =
        receiverCodecContext.read(InterledgerPayment.class, encodedPayment);
    Condition decodedCondition = receiverCodecContext.read(Condition.class, encodedCondition);
    PskMessage encryptedMessage =
        receiverCodecContext.read(PskMessage.class, decodedPayment.getData());

    assertArrayEquals("Encrypted data has changed.", encryptedData, encryptedMessage.getData());

    // Load PSK Context based on token extracted of address in payment packet
    PskContext receiverContext =
        PskContext.fromReceiverAddress(SECRET, decodedPayment.getDestinationAccount());

    // Decrypt PSK Message
    PskMessage decryptedPskMessage = receiverContext.decryptMessage(encryptedMessage);

    assertTrue("Secret header missing", decryptedPskMessage.getPrivateHeaders("Secret")
        .size() > 0);

    Fulfillment fulfillment = receiverContext.generateFulfillment(decodedPayment);
    assertTrue("Fulfillment is not valid", fulfillment.verify(decodedCondition, new byte[]{}));
  }

}
