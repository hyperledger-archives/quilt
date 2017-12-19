package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Ed25519Sha256Condition;
import org.interledger.cryptoconditions.PrefixSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Condition;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.cryptoconditions.RsaSha256Condition;
import org.interledger.cryptoconditions.ThresholdSha256Condition;
import org.interledger.quilt.jackson.cryptoconditions.CryptoConditionsModule.Encoding;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Validates the functionality of {@link CryptoConditionsModule}.
 */
@RunWith(Parameterized.class)
public class CryptoConditionsModuleConditionTest extends AbstractCryptoConditionsModuleTest {

  public static final BigInteger PUBLIC_EXPONENT = BigInteger.valueOf(65537);

  // Preimage
  private static final String PREIMAGE_CONDITION_DER_BYTES_HEX =
      "A0258020BF165845FFDB85F44A32052EC6279D2DBF151DE8E3A7D3727C94FC7AB531ACD581012B";
  private static final String PREIMAGE_CONDITION_DER_BYTES_BASE64
      = "oCWAIL8WWEX/24X0SjIFLsYnnS2/FR3o46fTcnyU/Hq1MazVgQEr";
  private static final String PREIMAGE_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oCWAIL8WWEX/24X0SjIFLsYnnS2/FR3o46fTcnyU/Hq1MazVgQEr";
  private static final String PREIMAGE_CONDITION_DER_BYTES_BASE64_URL
      = "oCWAIL8WWEX_24X0SjIFLsYnnS2_FR3o46fTcnyU_Hq1MazVgQEr";
  private static final String PREIMAGE_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oCWAIL8WWEX_24X0SjIFLsYnnS2_FR3o46fTcnyU_Hq1MazVgQEr";
  // Prefix
  private static final String PREFIX_CONDITION_DER_BYTES_HEX =
      "A12A8020E9241D95AD4FC17A373E609BB69FF0263C83597DD6BC758F162BF0D9D15F09EA8102046682020780";
  private static final String PREFIX_CONDITION_DER_BYTES_BASE64
      = "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A=";
  private static final String PREFIX_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A";
  private static final String PREFIX_CONDITION_DER_BYTES_BASE64_URL
      = "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A=";
  private static final String PREFIX_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oSqAIOkkHZWtT8F6Nz5gm7af8CY8g1l91rx1jxYr8NnRXwnqgQIEZoICB4A";
  // RSA
  private static final String RSA_CONDITION_DER_BYTES_HEX =
      "A3268020C92FD89F3EBEEE47E69E20EE14521E9E5605ADC734F858EBD1C35F2025DA5D1D81024000";
  private static final String RSA_CONDITION_DER_BYTES_BASE64
      = "oyaAIMkv2J8+vu5H5p4g7hRSHp5WBa3HNPhY69HDXyAl2l0dgQJAAA==";
  private static final String RSA_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oyaAIMkv2J8+vu5H5p4g7hRSHp5WBa3HNPhY69HDXyAl2l0dgQJAAA";
  private static final String RSA_CONDITION_DER_BYTES_BASE64_URL
      = "oyaAIMkv2J8-vu5H5p4g7hRSHp5WBa3HNPhY69HDXyAl2l0dgQJAAA==";
  private static final String RSA_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oyaAIMkv2J8-vu5H5p4g7hRSHp5WBa3HNPhY69HDXyAl2l0dgQJAAA";
  // Ed25519
  private static final String ED25519_CONDITION_DER_BYTES_HEX =
      "A4278020689E64935CE7DAAAD040EE50858657A068A0BF4639AD269F895DC150CD45F6138103020000";
  private static final String ED25519_CONDITION_DER_BYTES_BASE64
      = "pCeAIGieZJNc59qq0EDuUIWGV6BooL9GOa0mn4ldwVDNRfYTgQMCAAA=";
  private static final String ED25519_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "pCeAIGieZJNc59qq0EDuUIWGV6BooL9GOa0mn4ldwVDNRfYTgQMCAAA";
  private static final String ED25519_CONDITION_DER_BYTES_BASE64_URL
      = "pCeAIGieZJNc59qq0EDuUIWGV6BooL9GOa0mn4ldwVDNRfYTgQMCAAA=";
  private static final String ED25519_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "pCeAIGieZJNc59qq0EDuUIWGV6BooL9GOa0mn4ldwVDNRfYTgQMCAAA";
  // Threshhold
  private static final String THRESHOLD_CONDITION_DER_BYTES_HEX =
      "A22A8020ECF2CD7971471204029D36833A1D548D3AB476B8957876B7494D8058A0AE4E6C81025066820204D0";
  private static final String THRESHOLD_CONDITION_DER_BYTES_BASE64
      = "oiqAIOzyzXlxRxIEAp02gzodVI06tHa4lXh2t0lNgFigrk5sgQJQZoICBNA=";
  private static final String THRESHOLD_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING
      = "oiqAIOzyzXlxRxIEAp02gzodVI06tHa4lXh2t0lNgFigrk5sgQJQZoICBNA";
  private static final String THRESHOLD_CONDITION_DER_BYTES_BASE64_URL
      = "oiqAIOzyzXlxRxIEAp02gzodVI06tHa4lXh2t0lNgFigrk5sgQJQZoICBNA=";
  private static final String THRESHOLD_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oiqAIOzyzXlxRxIEAp02gzodVI06tHa4lXh2t0lNgFigrk5sgQJQZoICBNA";

  private static Condition CONDITION = constructCondition();
  private static PreimageSha256Condition PREIMAGE_CONDITION = constructPreimageCondition();
  private static PrefixSha256Condition PREFIX_CONDITION = constructPrefixCondition();
  private static RsaSha256Condition RSA_CONDITION = constructRsaCondition();
  private static Ed25519Sha256Condition ED25519_CONDITION = constructEd25519Condition();
  private static ThresholdSha256Condition THRESHOLD_CONDITION = constructThresholdCondition();

  private Condition condition;

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   * @param condition            A {@link Condition} to encode and decode for each test run.
   */
  public CryptoConditionsModuleConditionTest(
      final Encoding encodingToUse, final String expectedEncodedValue, final Condition condition
  ) {
    super(encodingToUse, expectedEncodedValue);
    this.condition = Objects.requireNonNull(condition);
  }

  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoConditionsModuleConditionTest constructor.
    return Arrays.asList(new Object[][]{

        // Condition
        {Encoding.HEX, PREIMAGE_CONDITION_DER_BYTES_HEX, CONDITION},
        {Encoding.BASE64, PREIMAGE_CONDITION_DER_BYTES_BASE64, CONDITION},
        {Encoding.BASE64_WITHOUT_PADDING,
            PREIMAGE_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING,
            CONDITION
        },
        {Encoding.BASE64URL, PREIMAGE_CONDITION_DER_BYTES_BASE64_URL, CONDITION},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            PREIMAGE_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            CONDITION
        },

        // Preimage
        {Encoding.HEX, PREIMAGE_CONDITION_DER_BYTES_HEX, PREIMAGE_CONDITION},
        {Encoding.BASE64, PREIMAGE_CONDITION_DER_BYTES_BASE64, PREIMAGE_CONDITION},
        {Encoding.BASE64_WITHOUT_PADDING,
            PREIMAGE_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING,
            PREIMAGE_CONDITION
        },
        {Encoding.BASE64URL, PREIMAGE_CONDITION_DER_BYTES_BASE64_URL, PREIMAGE_CONDITION},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            PREIMAGE_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            PREIMAGE_CONDITION
        },

        // Prefix
        {Encoding.HEX, PREFIX_CONDITION_DER_BYTES_HEX, PREFIX_CONDITION},
        {Encoding.BASE64, PREFIX_CONDITION_DER_BYTES_BASE64, PREFIX_CONDITION},
        {Encoding.BASE64_WITHOUT_PADDING,
            PREFIX_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING,
            PREFIX_CONDITION
        },
        {Encoding.BASE64URL, PREFIX_CONDITION_DER_BYTES_BASE64_URL, PREFIX_CONDITION},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            PREFIX_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            PREFIX_CONDITION
        },

        // RSA
        {Encoding.HEX, RSA_CONDITION_DER_BYTES_HEX, RSA_CONDITION},
        {Encoding.BASE64, RSA_CONDITION_DER_BYTES_BASE64, RSA_CONDITION},
        {Encoding.BASE64_WITHOUT_PADDING,
            RSA_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING,
            RSA_CONDITION
        },
        {Encoding.BASE64URL, RSA_CONDITION_DER_BYTES_BASE64_URL, RSA_CONDITION},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            RSA_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            RSA_CONDITION
        },

        // Ed25519
        {Encoding.HEX, ED25519_CONDITION_DER_BYTES_HEX, ED25519_CONDITION},
        {Encoding.BASE64, ED25519_CONDITION_DER_BYTES_BASE64, ED25519_CONDITION},
        {Encoding.BASE64_WITHOUT_PADDING,
            ED25519_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING,
            ED25519_CONDITION
        },
        {Encoding.BASE64URL, ED25519_CONDITION_DER_BYTES_BASE64_URL, ED25519_CONDITION},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            ED25519_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            ED25519_CONDITION
        },

        // Threshold
        {Encoding.HEX, THRESHOLD_CONDITION_DER_BYTES_HEX, THRESHOLD_CONDITION},
        {Encoding.BASE64, THRESHOLD_CONDITION_DER_BYTES_BASE64, THRESHOLD_CONDITION},
        {Encoding.BASE64_WITHOUT_PADDING,
            THRESHOLD_CONDITION_DER_BYTES_BASE64_WITHOUTPADDING,
            THRESHOLD_CONDITION
        },
        {Encoding.BASE64URL, THRESHOLD_CONDITION_DER_BYTES_BASE64_URL, THRESHOLD_CONDITION},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            THRESHOLD_CONDITION_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            THRESHOLD_CONDITION
        }
    });

  }

  //////////////////
  // Private Helpers
  //////////////////

  private static Condition constructCondition() {
    return constructPreimageCondition();
  }

  private static PreimageSha256Condition constructPreimageCondition() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return new PreimageSha256Fulfillment(preimage).getCondition();
  }

  private static PrefixSha256Condition constructPrefixCondition() {
    final byte[] prefix = "I'm your density. I mean, your destiny.".getBytes();
    return new PrefixSha256Condition(prefix, 20, constructPreimageCondition());
  }

  private static RsaSha256Condition constructRsaCondition() {
    try {
      final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      final RSAPrivateKey privateKey = buildRsaPrivKey();
      final RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
          privateKey.getModulus(), PUBLIC_EXPONENT
      );
      final PublicKey myPublicKey = keyFactory.generatePublic(publicKeySpec);

      return new RsaSha256Condition((RSAPublicKey) myPublicKey);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Ed25519Sha256Condition constructEd25519Condition() {
    try {
      final MessageDigest sha512Digest = MessageDigest.getInstance("SHA-512");

      KeyPair edDsaKeyPair = constructEd25519KeyPair();
      Signature edDsaSigner = new EdDSAEngine(sha512Digest);
      edDsaSigner.initSign(edDsaKeyPair.getPrivate());

      final byte[] prefix = "Oh, honey, he's teasing you. Nobody has two television sets."
          .getBytes();
      edDsaSigner.update(prefix);

      final byte[] message = "Marty! You've got to come back with me!".getBytes();
      edDsaSigner.update(message);

      return new Ed25519Sha256Condition((EdDSAPublicKey) edDsaKeyPair.getPublic());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static ThresholdSha256Condition constructThresholdCondition() {
    return new ThresholdSha256Condition(
        2,
        Lists.newArrayList(
            constructPreimageCondition(), constructRsaCondition(), constructPrefixCondition()
        )
    );
  }

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new CryptoConditionsModule(encodingToUse));

    // Ensures the empty Optionals are not serialized to make the assertions easier.
    this.objectMapper = this.objectMapper.setSerializationInclusion(Include.NON_EMPTY);
  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final CryptoConditionsContainer expectedContainer = new CryptoConditionsContainer(
        Optional.of(condition),
        Optional.empty()
    );

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"condition\":\"%s\"}", expectedEncodedValue)
    ));

    final CryptoConditionsContainer actualAddressContainer = objectMapper
        .readValue(json, CryptoConditionsContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getCondition().get(), is(condition));
  }

}