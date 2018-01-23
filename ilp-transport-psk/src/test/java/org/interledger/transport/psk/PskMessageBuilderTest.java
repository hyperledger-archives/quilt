package org.interledger.transport.psk;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.interledger.InterledgerRuntimeException;

import org.junit.Assert;
import org.junit.Test;

import java.util.Base64;

/**
 * JUnit to exercise the {@link PskMessage.Builder} implementation.
 */
public class PskMessageBuilderTest {

  @Test
  public void test_NonceValueProvided() {
    /* add our own nonce header, the builder should not try generate its own */
    String nonce = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(new byte[16]);

    PskMessage message =
        PskMessage.builder()
            .addPublicHeader(PskMessage.Header.WellKnown.NONCE, nonce)
            .build();

    Assert.assertNotNull(message.getNonceHeader());
    assertEquals(1, message.getPublicHeaders(PskMessage.Header.WellKnown.NONCE)
        .size());

    assertEquals(nonce, message.getNonceHeader()
        .getValue());
  }

  @Test
  public void test_NonceAndEncryptHeaderGenerated() {
    PskMessage message = PskMessage.builder()
        .build();

    Assert.assertNotNull(message);

    /* check that a nonce and encryption header was automatically added by the builder */
    assertEquals(2, message.getPublicHeaders()
        .size());
    Assert.assertNotNull(message.getEncryptionHeader());
    assertEquals(PskEncryptionType.NONE, message.getEncryptionHeader()
        .getEncryptionType());
  }


  @Test(expected = InterledgerRuntimeException.class)
  public void test_addPublicHeaderEncryption() {
    PskMessage.builder()
        .addPublicHeader(PskMessage.Header.WellKnown.ENCRYPTION, "3DES")
        .build();
  }

  @Test
  public void test_addPrivateHeaderEncryption() {
    /* we can add any header we want to the private portion */
    PskMessage message = PskMessage.builder()
        .addPrivateHeader(PskMessage.Header.WellKnown.ENCRYPTION, "3DES")
        .build();

    Assert.assertNotNull(message);
    assertEquals(1, message.getPrivateHeaders()
        .size());
    assertEquals("Encryption", message.getPrivateHeaders()
        .get(0)
        .getName());
    assertEquals("3DES", message.getPrivateHeaders()
        .get(0)
        .getValue());
  }

  @Test
  public void test() {
    PskMessage message =
        PskMessage.builder()
            .addPublicHeader("public_header", "public_header_value")
            .addPrivateHeader("private_encryption_header", "3DES")
            .data("Application Data".getBytes())
            .build();

    Assert.assertNotNull(message);
    assertEquals(3, message.getPublicHeaders()
        .size());
    assertEquals(1, message.getPublicHeaders("Nonce")
        .size());
    assertEquals("public_header_value",
        message.getPublicHeaders("public_header")
            .get(0)
            .getValue());

    assertEquals(1, message.getPrivateHeaders()
        .size());
    assertEquals("private_encryption_header", message.getPrivateHeaders()
        .get(0)
        .getName());
    assertEquals("3DES", message.getPrivateHeaders()
        .get(0)
        .getValue());

    assertArrayEquals("Application Data".getBytes(), message.getData());
  }

}
