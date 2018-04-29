package org.interledger.quilt.jackson.conditions;

import org.interledger.core.Condition;
import org.interledger.core.Fulfillment;
import org.interledger.quilt.jackson.conditions.ConditionModule;
import org.interledger.quilt.jackson.conditions.Encoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;

import java.security.Provider;
import java.security.Security;
import java.util.Objects;

/**
 * Validates the functionality of {@link ConditionModule}.
 */
public abstract class AbstractConditionModuleTest {

  /**
   * Need to add BouncyCastle so we have a provider that supports SHA256withRSA/PSS signatures
   */
  static {
    Provider bc = new BouncyCastleProvider();
    Security.addProvider(bc);
  }

  protected ObjectMapper objectMapper;
  protected Encoding encodingToUse;
  protected String expectedEncodedValue;

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   */
  public AbstractConditionModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue
  ) {
    this.encodingToUse = Objects.requireNonNull(encodingToUse);
    this.expectedEncodedValue = Objects.requireNonNull(expectedEncodedValue);
  }

  //////////////////
  // Protected Helpers
  //////////////////


  protected static Condition constructPreimageCondition() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return Condition.of(preimage);
  }

  protected static Fulfillment constructPreimageFulfillment() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return Fulfillment.of(preimage);
  }

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new ConditionModule(encodingToUse));
  }

}
