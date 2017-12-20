package org.interledger.quilt.jackson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.interledger.InterledgerAddress;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.quilt.jackson.cryptoconditions.Encoding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Validates the functionality of {@link InterledgerModule}.
 */
@RunWith(Parameterized.class)
public class InterledgerModuleTest {

  private static final String CONDITION_DER_BYTES_HEX =
      "A0258020BF165845FFDB85F44A32052EC6279D2DBF151DE8E3A7D3727C94FC7AB531ACD581012B";
  private static final String CONDITION_DER_BYTES_BASE64
      = "oCWAIL8WWEX/24X0SjIFLsYnnS2/FR3o46fTcnyU/Hq1MazVgQEr";
  private static final String CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oCWAIL8WWEX/24X0SjIFLsYnnS2/FR3o46fTcnyU/Hq1MazVgQEr";
  private static final String CONDITION_DER_BYTES_BASE64_URL
      = "oCWAIL8WWEX_24X0SjIFLsYnnS2_FR3o46fTcnyU_Hq1MazVgQEr";
  private static final String CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oCWAIL8WWEX_24X0SjIFLsYnnS2_FR3o46fTcnyU_Hq1MazVgQEr";

  private static Condition CONDITION = constructCondition();

  private ObjectMapper objectMapper;
  private Condition condition;
  private Encoding encodingToUse;
  private String expectedEncodedValue;

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   * @param condition            A {@link Condition} to encode and decode for each test run.
   */
  public InterledgerModuleTest(
      final Encoding encodingToUse, final String expectedEncodedValue, final Condition condition
  ) {
    this.encodingToUse = Objects.requireNonNull(encodingToUse);
    this.expectedEncodedValue = Objects.requireNonNull(expectedEncodedValue);
    this.condition = Objects.requireNonNull(condition);
  }

  private static PreimageSha256Condition constructCondition() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return new PreimageSha256Fulfillment(preimage).getCondition();
  }

  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleConditionTest constructor.
    return Arrays.asList(new Object[][]{
        {Encoding.HEX, CONDITION_DER_BYTES_HEX, CONDITION},
        {Encoding.BASE64, CONDITION_DER_BYTES_BASE64, CONDITION},
        {Encoding.BASE64_WITHOUT_PADDING, CONDITION_DER_BYTES_BASE64_WITHOUTPADDING, CONDITION},
        {Encoding.BASE64URL, CONDITION_DER_BYTES_BASE64_URL, CONDITION},
        {
            Encoding.BASE64URL_WITHOUT_PADDING,
            CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            CONDITION
        }
    });
  }

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new InterledgerModule(encodingToUse));
  }

  @Test
  public void testSerializeDeserialize() throws IOException {

    final InterledgerAddress expectedAddress = InterledgerAddress.of("test1.ledger.foo.");

    final InterledgerContainer expectedContainer = new InterledgerContainer(expectedAddress,
        condition);

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"ledger_prefix\":\"%s\",\"execution_condition\":\"%s\"}",
            expectedContainer.getInterledgerAddress().getValue(),
            expectedEncodedValue)
    ));

    final InterledgerContainer actualAddressContainer = objectMapper
        .readValue(json, InterledgerContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getCondition(), is(condition));
  }

  private Condition constructTestCondition() {
    final byte[] preimage = "secret".getBytes();
    return new PreimageSha256Fulfillment(preimage).getCondition();
  }


  private static class InterledgerContainer {

    @JsonProperty("ledger_prefix")
    private final InterledgerAddress interledgerAddress;

    @JsonProperty("execution_condition")
    private final Condition condition;

    @JsonCreator
    public InterledgerContainer(
        @JsonProperty("ledger_prefix") final InterledgerAddress interledgerAddress,
        @JsonProperty("execution_condition") final Condition condition
    ) {
      this.interledgerAddress = Objects.requireNonNull(interledgerAddress);
      this.condition = Objects.requireNonNull(condition);
    }

    public InterledgerAddress getInterledgerAddress() {
      return interledgerAddress;
    }

    public Condition getCondition() {
      return condition;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      InterledgerContainer that = (InterledgerContainer) o;

      if (!interledgerAddress.equals(that.interledgerAddress)) {
        return false;
      }
      return condition.equals(that.condition);
    }

    @Override
    public int hashCode() {
      int result = interledgerAddress.hashCode();
      result = 31 * result + condition.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "InterledgerContainer{" +
          "interledgerAddress=" + interledgerAddress +
          ", condition=" + condition +
          '}';
    }
  }
}