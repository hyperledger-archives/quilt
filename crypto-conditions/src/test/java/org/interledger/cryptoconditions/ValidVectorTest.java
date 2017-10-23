package org.interledger.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.io.BaseEncoding;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.i2p.crypto.eddsa.EdDSAPublicKey;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import org.interledger.cryptoconditions.der.DerEncodingException;
import org.interledger.cryptoconditions.helpers.TestKeyFactory;
import org.interledger.cryptoconditions.helpers.TestVector;
import org.interledger.cryptoconditions.helpers.TestVectorFactory;
import org.interledger.cryptoconditions.helpers.TestVectorJson;
import org.interledger.cryptoconditions.utils.UnsignedBigInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Provider;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This class tests the Java implementation of crypto-conditions based on a set of pre-computed and
 * validated test vectors found in the crypto-conditions spec repository.
 * <p/>
 * Specifically, this harness performs the following validations according to the source of the
 * test 'vectors' file in the crypto-conditions rfc project:
 *
 * <pre>
 * <ul>
 *   <li>Parse condition Binary, serialize as a URI, should match conditionUri.</li>
 *   <li>Parse condition Uri, serialize as binary, should match conditionBinary.</li>
 *   <li>Parse fulfillment binary, serialize fulfillment, should match fulfillment.</li>
 *   <li>Parse fulfillment binary, and verify should return true.</li>
 *   <li>Parse fulfillment binary, and generate the fingerprint contents</li>
 *   <li>Parse fulfillment binary, generate the condition, serialize the condition as a URI, should
 *       match conditionUri.</li>
 *   <li>Create fulfillment from json, serialize fulfillment, should match fulfillment.</li>
 * </ul>
 * </pre>
 * <p/>
 * If a message field is provided, the condition should be evaluated against the message.
 * Otherwise, an empty message should be passed to the verification function.
 *
 * @see "https://github.com/rfcs/crypto-conditions/tree/master/test-vectors/valid"
 */
@RunWith(Parameterized.class)
public class ValidVectorTest {

  /**
   * Need to add BouncyCastle so we have a provider that supports SHA256withRSA/PSS signatures
   */
  static {
    Provider bc = new BouncyCastleProvider();
    Security.addProvider(bc);
  }

  private TestVector testVector;

  public ValidVectorTest(TestVector testVector) throws Exception {
    this.testVector = testVector;
  }

  /**
   * Loads a list of tests based on the json-encoded test vector files.
   */
  @Parameters(name = "Test Vector {index}: {0}")
  public static Collection<TestVector> testVectorData() throws Exception {
    final URI baseUri =
        ValidVectorTest.class.getResource(ValidVectorTest.class.getSimpleName() + ".class").toURI();
    final File baseDirectoryFile = new File(baseUri).getParentFile();
    final File validTestVectorDir = new File(baseDirectoryFile, "/vectors/valid");

    final Builder<TestVector> vectors = ImmutableList.builder();
    final ObjectMapper mapper = new ObjectMapper();

    Arrays.stream(validTestVectorDir.listFiles()).forEach(file -> {
      try {
        if (file.getName().endsWith(".json")) {
          TestVector vector = mapper.readValue(file, TestVector.class);
          vector.setName(file.getName().substring(0, file.getName().length() - 5));
          vectors.add(vector);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    return vectors.build();
  }

  /**
   * This test parses the conditionBinary, serializes it as a URI, and validates that the generated
   * URI matches "conditionUri" from the test vector.
   */
  @Test
  public void testParseConditionBinary() throws DerEncodingException {
    final Condition actualCondition = CryptoConditionReader
        .readCondition(BaseEncoding.base16().decode(testVector.getConditionBinary()));
    final Condition testVectorCondition =
        TestVectorFactory.getConditionFromTestVectorJson(testVector.getJson());

    assertThat(CryptoConditionUri.toUri(actualCondition).toString(),
        is(CryptoConditionUri.toUri(testVectorCondition).toString()));
    assertThat(actualCondition, is(testVectorCondition));
  }

  /**
   * This test parses a condition URI, serializes the condition to ASN.1 DER encoded bytes, and then
   * validates that the binary value matches the "condition" binary value from the test vector.
   */
  @Test
  public void testParseConditionFromUri() throws DerEncodingException, URISyntaxException {
    final Condition actualCondition = CryptoConditionUri
        .parse(URI.create(testVector.getConditionUri()));
    final byte[] actualEncodedCondition = CryptoConditionWriter.writeCondition(actualCondition);

    final byte[] testVectorBinary = BaseEncoding.base16().decode(testVector.getConditionBinary());

    assertThat(actualEncodedCondition, is(testVectorBinary));
  }

  /**
   * This test parses a fulfillment binary from the test vectors, serializes it back to binary, and
   * asserts that the generated bytes match the serialized bytes.
   */
  @Test
  public void testParseFulfillmentBytes() throws DerEncodingException {
    final byte[] testVectorFulfillmentBytes = BaseEncoding.base16()
        .decode(testVector.getFulfillment());
    final Fulfillment testVectorFulfillment = CryptoConditionReader.readFulfillment(
        testVectorFulfillmentBytes
    );

    assertThat(
        CryptoConditionWriter.writeFulfillment(testVectorFulfillment),
        is(testVectorFulfillmentBytes)
    );
  }

  /**
   * This test parses the fulfillment binary as a control fulfillment. It then loads a condition
   * from the test vector by its URI, and calls {@link Fulfillment#verify(Condition,
   * byte[])}, asserting that it returns true.
   */
  @Test
  public void testParseFulfillmentFromBinaryAndVerify() throws Exception {
    final Condition conditionFromTestVectorUri = CryptoConditionUri
        .parse(URI.create(testVector.getConditionUri()));
    final byte[] messageBytes = BaseEncoding.base16().decode(testVector.getMessage());

    final Fulfillment controlFulfillment = CryptoConditionReader
        .readFulfillment(BaseEncoding.base16().decode(testVector.getFulfillment()));

    assertThat(controlFulfillment.verify(conditionFromTestVectorUri, messageBytes), is(true));
  }

  /**
   * This test reads the binary condition and binary fulfillment data, and then asserts that the
   * condition verifies the fulfillment.
   */
  @Test
  public void testParseBinaryConditionAgainstBinaryFulfillment() throws DerEncodingException {
    final byte[] messageBinary = BaseEncoding.base16().decode(testVector.getMessage());
    final Condition conditionFromBinary = CryptoConditionReader
        .readCondition(BaseEncoding.base16().decode(testVector.getConditionBinary()));
    final Fulfillment fulfillmentFromBinary = CryptoConditionReader
        .readFulfillment(BaseEncoding.base16().decode(testVector.getFulfillment()));

    assertTrue(fulfillmentFromBinary.verify(conditionFromBinary, messageBinary));
  }

  /**
   * This test parses fulfillment binary from the test vector, and then validates that
   * this value matches the fingerprint from an actual Condition generated from the testVector
   * JSON. This is a slightly different test from the one that parses the binary fulfillment and
   * condition data and asserts that the fingerprints are the same
   * (i.e., {@link #testParseBinaryConditionAgainstBinaryFulfillment}).
   */
  @Test
  public void testParseFulfillmentFromBinaryAndValidateFingerprintContents() {
    final byte[] testVectorFingerprintContents = BaseEncoding.base16()
        .decode(testVector.getFingerprintContents());

    final Condition actualTestCondition = TestVectorFactory
        .getConditionFromTestVectorJson(testVector.getJson());

    // Depending on the type, we need to cast the condition to access constructFingerprintContents();
    final byte[] unhashedFingerprintContents;
    switch (actualTestCondition.getType()) {
      case PREIMAGE_SHA256: {
        unhashedFingerprintContents = ((PreimageSha256Condition) actualTestCondition)
            .constructFingerprintContents(
                BaseEncoding.base64Url().decode(testVector.getJson().getPreimage())
            );
        break;
      }
      case PREFIX_SHA256: {
        unhashedFingerprintContents =
            ((PrefixSha256Condition) actualTestCondition).constructFingerprintContents(
                BaseEncoding.base64Url().decode(testVector.getJson().getPrefix()),
                testVector.getJson().getMaxMessageLength(),
                TestVectorFactory
                    .getFulfillmentFromTestVectorJson(testVector.getJson().getSubfulfillment())
                    .getCondition()
            );
        break;
      }
      case ED25519_SHA256: {
        final EdDSAPublicKey publicKey = TestKeyFactory
            .constructEdDsaPublicKey(testVector.getJson().getPublicKey());
        unhashedFingerprintContents =
            ((Ed25519Sha256Condition) actualTestCondition).constructFingerprintContents(publicKey);
        break;
      }
      case RSA_SHA256: {
        final RSAPublicKey publicKey = TestKeyFactory
            .constructRsaPublicKey(testVector.getJson().getModulus());
        unhashedFingerprintContents
            = ((RsaSha256Condition) actualTestCondition).constructFingerprintContents(publicKey);
        break;
      }
      case THRESHOLD_SHA256: {
        final int threshold = testVector.getJson().getThreshold();
        final TestVectorJson[] jsonSubfulfillments = testVector.getJson().getSubfulfillments();
        final List<Condition> subconditions = new LinkedList<>();
        for (int i = 0; i < jsonSubfulfillments.length; i++) {
          subconditions
              .add(TestVectorFactory.getConditionFromTestVectorJson(jsonSubfulfillments[i]));
        }
        unhashedFingerprintContents = ((ThresholdSha256Condition) actualTestCondition)
            .constructFingerprintContents(threshold, subconditions);
        break;
      }
      default: {
        throw new RuntimeException("Unhandled Condition Type!");
      }
    }

    assertThat(unhashedFingerprintContents, is(testVectorFingerprintContents));
  }

  /**
   * This test parses the fulfillment from the test vector file, generates a corresponding
   * condition, serializes the condition as a URI, and validates that the generated URI matches the
   * conditionUri from the test vector.
   */
  @Test
  public void testParseFulfillmentFromBinaryAndGenerateCondition()
      throws DerEncodingException, URISyntaxException {

    final Fulfillment testVectorFulfillment = CryptoConditionReader
        .readFulfillment(BaseEncoding.base16().decode(testVector.getFulfillment()));

    final String derivedUri = CryptoConditionUri.toUri(testVectorFulfillment.getCondition())
        .toString();

    assertThat(testVector.getConditionUri(), is(derivedUri));
  }

  /**
   * This test creates a fulfillment from individual values found in the json test vector file,
   * then converts the fulfillment to a condition, and then asserts that the binary fulfillment in
   * JSON test vector matches what is generated by the Java code.
   */
  @Test
  public void testParseJsonFulfillment() throws Exception {
    // Reads binary from the JSON test vector, and converts to Java...
    final Fulfillment controlFulfillment = CryptoConditionReader
        .readFulfillment(BaseEncoding.base16().decode(testVector.getFulfillment()));

    // Reads individual components from JSON and assembles a condition...
    final Condition conditionToFulfill = TestVectorFactory
        .getConditionFromTestVectorJson(testVector.getJson());

    // Assert that the control fulfillment (which we assume is assembled properly since we're not
    // testing CryptoConditionReader here) can be verifed with the manually-assembled condition.
    final byte[] messageBytes = BaseEncoding.base16().decode(testVector.getMessage());
    assertTrue(controlFulfillment.verify(conditionToFulfill, messageBytes));
  }

  @Test
  public void testCost() throws URISyntaxException, DerEncodingException {
    final long actualCost = TestVectorFactory.getConditionFromTestVectorJson(testVector.getJson())
        .getCost();

    assertThat(actualCost, is(testVector.getCost()));
  }

  @Test
  public void testParseFulfillmentAndCheckProperties() throws Exception {
    byte[] fulfillmentBytes = BaseEncoding.base16().decode(testVector.getFulfillment());
    Fulfillment fulfillment = CryptoConditionReader.readFulfillment(fulfillmentBytes);

    switch (fulfillment.getType()) {
      case PREIMAGE_SHA256:
        PreimageSha256Fulfillment preimageFulfillment = (PreimageSha256Fulfillment) fulfillment;
        assertEquals(testVector.getName() + " [compare preimage]",
            testVector.getJson().getPreimage(),
            preimageFulfillment.getPreimage());
        break;

      case PREFIX_SHA256:
        PrefixSha256Fulfillment prefixFulfillment = (PrefixSha256Fulfillment) fulfillment;
        assertArrayEquals(testVector.getName() + " [compare prefix]",
            Base64.getUrlDecoder().decode(testVector.getJson().getPrefix()),
            prefixFulfillment.getPrefix());
        assertEquals(testVector.getName() + " [compare max message length]",
            testVector.getJson().getMaxMessageLength(), prefixFulfillment.getMaxMessageLength());
        CryptoConditionAssert.assertSetOfTypesIsEqual(testVector.getName() + " [compare subtypes]",
            testVector.getSubtypes(), prefixFulfillment.getCondition().getSubtypes());
        // TODO Should we test for equality of subfulfillments?
        break;

      case THRESHOLD_SHA256:
        ThresholdSha256Fulfillment thresholdFulfillment = (ThresholdSha256Fulfillment) fulfillment;
        CryptoConditionAssert.assertSetOfTypesIsEqual(testVector.getName() + " [compare subtypes]",
            testVector.getSubtypes(), thresholdFulfillment.getCondition().getSubtypes());
        // TODO Should we test for equality of subfulfillments and subconditions?
        break;

      case RSA_SHA256:
        RsaSha256Fulfillment rsaFulfillment = (RsaSha256Fulfillment) fulfillment;
        assertArrayEquals(testVector.getName() + " [compare rsa key modulus]",
            Base64.getUrlDecoder().decode(testVector.getJson().getModulus()),
            UnsignedBigInteger.toUnsignedByteArray(rsaFulfillment.getPublicKey().getModulus()));
        assertArrayEquals(testVector.getName() + " [compare rsa signature]",
            Base64.getUrlDecoder().decode(testVector.getJson().getSignature()),
            rsaFulfillment.getSignature());
        break;

      case ED25519_SHA256:
        Ed25519Sha256Fulfillment ed25519Fulfillment = (Ed25519Sha256Fulfillment) fulfillment;
        assertArrayEquals(testVector.getName() + " [compare ed25519 key]",
            Base64.getUrlDecoder().decode(testVector.getJson().getPublicKey()),
            ed25519Fulfillment.getPublicKey().getAbyte());
        assertArrayEquals(testVector.getName() + " [compare signature]",
            Base64.getUrlDecoder().decode(testVector.getJson().getSignature()),
            ed25519Fulfillment.getSignature());
        break;

      default:
        throw new Exception("Unknown fulfillment type: " + fulfillment.getType());
    }
  }
}