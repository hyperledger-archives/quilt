package org.interledger.quilt.jackson.cryptoconditions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.interledger.cryptoconditions.Ed25519Sha256Fulfillment;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.PrefixSha256Fulfillment;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.cryptoconditions.RsaSha256Fulfillment;
import org.interledger.cryptoconditions.ThresholdSha256Fulfillment;
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
public class CryptoConditionsModuleFulfillmentTest extends AbstractCryptoConditionsModuleTest {

  // Preimage
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_HEX =
      "A02D802B796F75206275696C7420612074696D65206D616368696E65206F7574206F6620612044654C6F7265616E3F";
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_BASE64
      = "oC2AK3lvdSBidWlsdCBhIHRpbWUgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8=";
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "oC2AK3lvdSBidWlsdCBhIHRpbWUgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8";
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL
      = "oC2AK3lvdSBidWlsdCBhIHRpbWUgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8=";
  private static final String PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oC2AK3lvdSBidWlsdCBhIHRpbWUgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8";
  // Prefix
  private static final String PREFIX_FULFILLMENT_DER_BYTES_HEX =
      "A15D802749276D20796F75722064656E736974792E2049206D65616E2C20796F75722064657374696E792E810114"
          + "A22FA02D802B796F75206275696C7420612074696D65206D616368696E65206F7574206F6620612044654C"
          + "6F7265616E3F";
  private static final String PREFIX_FULFILLMENT_DER_BYTES_BASE64
      = "oV2AJ0knbSB5b3VyIGRlbnNpdHkuIEkgbWVhbiwgeW91ciBkZXN0aW55LoEBFKIvoC2AK3lvdSBidWlsdCBhIHRpbW"
      + "UgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8=";
  private static final String PREFIX_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "oV2AJ0knbSB5b3VyIGRlbnNpdHkuIEkgbWVhbiwgeW91ciBkZXN0aW55LoEBFKIvoC2AK3lvdSBidWlsdCBhIHRpbW"
      + "UgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8";
  private static final String PREFIX_FULFILLMENT_DER_BYTES_BASE64_URL
      = "oV2AJ0knbSB5b3VyIGRlbnNpdHkuIEkgbWVhbiwgeW91ciBkZXN0aW55LoEBFKIvoC2AK3lvdSBidWlsdCBhIHRpbW"
      + "UgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8=";
  private static final String PREFIX_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "oV2AJ0knbSB5b3VyIGRlbnNpdHkuIEkgbWVhbiwgeW91ciBkZXN0aW55LoEBFKIvoC2AK3lvdSBidWlsdCBhIHRpbW"
      + "UgbWFjaGluZSBvdXQgb2YgYSBEZUxvcmVhbj8";
  // RSA
  private static final String RSA_FULFILLMENT_DER_BYTES_HEX =
      "A3818E808180A56E4A0E701017589A5187DC7EA841D156F2EC0E36AD52A44DFEB1E61F7AD991D8C51056FFEDB162"
          + "B4C0F283A12A88A394DFF526AB7291CBB307CEABFCE0B1DFD5CD9508096D5B2B8B6DF5D671EF6377C0921C"
          + "B23C270A70E2598E6FF89D19F105ACC2D3F0CB35F29280E1386B6F64C4EF22E1E1F20D0CE8CFFB2249BD9A"
          + "213781097369676E6174757265";
  private static final String RSA_FULFILLMENT_DER_BYTES_BASE64
      = "o4GOgIGApW5KDnAQF1iaUYfcfqhB0Vby7A42rVKkTf6x5h962ZHYxRBW/+2xYrTA8oOhKoijlN/1JqtykcuzB86r/O"
      + "Cx39XNlQgJbVsri2311nHvY3fAkhyyPCcKcOJZjm/4nRnxBazC0/DLNfKSgOE4a29kxO8i4eHyDQzoz/siSb2aITeB"
      + "CXNpZ25hdHVyZQ==";
  private static final String RSA_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "o4GOgIGApW5KDnAQF1iaUYfcfqhB0Vby7A42rVKkTf6x5h962ZHYxRBW/+2xYrTA8oOhKoijlN/1JqtykcuzB86r/O"
      + "Cx39XNlQgJbVsri2311nHvY3fAkhyyPCcKcOJZjm/4nRnxBazC0/DLNfKSgOE4a29kxO8i4eHyDQzoz/siSb2aITeB"
      + "CXNpZ25hdHVyZQ";
  private static final String RSA_FULFILLMENT_DER_BYTES_BASE64_URL
      = "o4GOgIGApW5KDnAQF1iaUYfcfqhB0Vby7A42rVKkTf6x5h962ZHYxRBW_-2xYrTA8oOhKoijlN_1JqtykcuzB86r_O"
      + "Cx39XNlQgJbVsri2311nHvY3fAkhyyPCcKcOJZjm_4nRnxBazC0_DLNfKSgOE4a29kxO8i4eHyDQzoz_siSb2aITeB"
      + "CXNpZ25hdHVyZQ==";
  private static final String RSA_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "o4GOgIGApW5KDnAQF1iaUYfcfqhB0Vby7A42rVKkTf6x5h962ZHYxRBW_-2xYrTA8oOhKoijlN_1JqtykcuzB86r_O"
      + "Cx39XNlQgJbVsri2311nHvY3fAkhyyPCcKcOJZjm_4nRnxBazC0_DLNfKSgOE4a29kxO8i4eHyDQzoz_siSb2aITeB"
      + "CXNpZ25hdHVyZQ";
  // Ed25519
  private static final String ED25519_FULFILLMENT_DER_BYTES_HEX =
      "A47A802036AE1B97C577AE6AFB0294E91839FA7B1F9332791B9F2C5D586819025F4A2F1D815635565A44414D4E67"
          + "72484B5168754C4D67473643696F53486678363435646C30324850675A534A4A41565666754949566B4B4D"
          + "37724D59654F5841632D625272306C763138466C627669526C555546446A6E6F514377";
  private static final String ED25519_FULFILLMENT_DER_BYTES_BASE64
      = "pHqAIDauG5fFd65q+wKU6Rg5+nsfkzJ5G58sXVhoGQJfSi8dgVY1VlpEQU1OZ3JIS1FodUxNZ0c2Q2lvU0hmeDY0NW"
      + "RsMDJIUGdaU0pKQVZWZnVJSVZrS003ck1ZZU9YQWMtYlJyMGx2MThGbGJ2aVJsVVVGRGpub1FDdw==";
  private static final String ED25519_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "pHqAIDauG5fFd65q+wKU6Rg5+nsfkzJ5G58sXVhoGQJfSi8dgVY1VlpEQU1OZ3JIS1FodUxNZ0c2Q2lvU0hmeDY0NW"
      + "RsMDJIUGdaU0pKQVZWZnVJSVZrS003ck1ZZU9YQWMtYlJyMGx2MThGbGJ2aVJsVVVGRGpub1FDdw";
  private static final String ED25519_FULFILLMENT_DER_BYTES_BASE64_URL
      = "pHqAIDauG5fFd65q-wKU6Rg5-nsfkzJ5G58sXVhoGQJfSi8dgVY1VlpEQU1OZ3JIS1FodUxNZ0c2Q2lvU0hmeDY0NW"
      + "RsMDJIUGdaU0pKQVZWZnVJSVZrS003ck1ZZU9YQWMtYlJyMGx2MThGbGJ2aVJsVVVGRGpub1FDdw==";
  private static final String ED25519_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "pHqAIDauG5fFd65q-wKU6Rg5-nsfkzJ5G58sXVhoGQJfSi8dgVY1VlpEQU1OZ3JIS1FodUxNZ0c2Q2lvU0hmeDY0NW"
      + "RsMDJIUGdaU0pKQVZWZnVJSVZrS003ck1ZZU9YQWMtYlJyMGx2MThGbGJ2aVJsVVVGRGpub1FDdw";
  // Threshhold
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_HEX =
      "A2820125A082011FA02D802B796F75206275696C7420612074696D65206D616368696E65206F7574206F66206120"
          + "44654C6F7265616E3FA3818E808180A56E4A0E701017589A5187DC7EA841D156F2EC0E36AD52A44DFEB1E6"
          + "1F7AD991D8C51056FFEDB162B4C0F283A12A88A394DFF526AB7291CBB307CEABFCE0B1DFD5CD9508096D5B"
          + "2B8B6DF5D671EF6377C0921CB23C270A70E2598E6FF89D19F105ACC2D3F0CB35F29280E1386B6F64C4EF22"
          + "E1E1F20D0CE8CFFB2249BD9A213781097369676E6174757265A15D802749276D20796F75722064656E7369"
          + "74792E2049206D65616E2C20796F75722064657374696E792E810114A22FA02D802B796F75206275696C74"
          + "20612074696D65206D616368696E65206F7574206F6620612044654C6F7265616E3FA100";
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_BASE64
      = "ooIBJaCCAR+gLYAreW91IGJ1aWx0IGEgdGltZSBtYWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6OBjoCBgKVuSg5wEB"
      + "dYmlGH3H6oQdFW8uwONq1SpE3+seYfetmR2MUQVv/tsWK0wPKDoSqIo5Tf9SarcpHLswfOq/zgsd/VzZUICW1bK4tt"
      + "9dZx72N3wJIcsjwnCnDiWY5v+J0Z8QWswtPwyzXykoDhOGtvZMTvIuHh8g0M6M/7Ikm9miE3gQlzaWduYXR1cmWhXY"
      + "AnSSdtIHlvdXIgZGVuc2l0eS4gSSBtZWFuLCB5b3VyIGRlc3RpbnkugQEUoi+gLYAreW91IGJ1aWx0IGEgdGltZSBt"
      + "YWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6EA";
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING
      = "ooIBJaCCAR+gLYAreW91IGJ1aWx0IGEgdGltZSBtYWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6OBjoCBgKVuSg5wEB"
      + "dYmlGH3H6oQdFW8uwONq1SpE3+seYfetmR2MUQVv/tsWK0wPKDoSqIo5Tf9SarcpHLswfOq/zgsd/VzZUICW1bK4tt"
      + "9dZx72N3wJIcsjwnCnDiWY5v+J0Z8QWswtPwyzXykoDhOGtvZMTvIuHh8g0M6M/7Ikm9miE3gQlzaWduYXR1cmWhXY"
      + "AnSSdtIHlvdXIgZGVuc2l0eS4gSSBtZWFuLCB5b3VyIGRlc3RpbnkugQEUoi+gLYAreW91IGJ1aWx0IGEgdGltZSBt"
      + "YWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6EA";
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_URL
      = "ooIBJaCCAR-gLYAreW91IGJ1aWx0IGEgdGltZSBtYWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6OBjoCBgKVuSg5wEB"
      + "dYmlGH3H6oQdFW8uwONq1SpE3-seYfetmR2MUQVv_tsWK0wPKDoSqIo5Tf9SarcpHLswfOq_zgsd_VzZUICW1bK4tt"
      + "9dZx72N3wJIcsjwnCnDiWY5v-J0Z8QWswtPwyzXykoDhOGtvZMTvIuHh8g0M6M_7Ikm9miE3gQlzaWduYXR1cmWhXY"
      + "AnSSdtIHlvdXIgZGVuc2l0eS4gSSBtZWFuLCB5b3VyIGRlc3RpbnkugQEUoi-gLYAreW91IGJ1aWx0IGEgdGltZSBt"
      + "YWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6EA";
  private static final String THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING
      = "ooIBJaCCAR-gLYAreW91IGJ1aWx0IGEgdGltZSBtYWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6OBjoCBgKVuSg5wEB"
      + "dYmlGH3H6oQdFW8uwONq1SpE3-seYfetmR2MUQVv_tsWK0wPKDoSqIo5Tf9SarcpHLswfOq_zgsd_VzZUICW1bK4tt"
      + "9dZx72N3wJIcsjwnCnDiWY5v-J0Z8QWswtPwyzXykoDhOGtvZMTvIuHh8g0M6M_7Ikm9miE3gQlzaWduYXR1cmWhXY"
      + "AnSSdtIHlvdXIgZGVuc2l0eS4gSSBtZWFuLCB5b3VyIGRlc3RpbnkugQEUoi-gLYAreW91IGJ1aWx0IGEgdGltZSBt"
      + "YWNoaW5lIG91dCBvZiBhIERlTG9yZWFuP6EA";

  /**
   * Need to add BouncyCastle so we have a provider that supports SHA256withRSA/PSS signatures
   */
  static {
    Provider bc = new BouncyCastleProvider();
    Security.addProvider(bc);
  }

  private static final Fulfillment FULFILLMENT = constructFulfillment();
  private static final PreimageSha256Fulfillment PREIMAGE_FULFILLMENT = constructPreimageFulfillment();
  private static final PrefixSha256Fulfillment PREFIX_FULFILLMENT = constructPrefixFulfillment();
  private static final RsaSha256Fulfillment RSA_FULFILLMENT = constructRsaFulfillment();
  private static final Ed25519Sha256Fulfillment ED25519_FULFILLMENT = constructEd25519Fulfillment();
  private static final ThresholdSha256Fulfillment THRESHOLD_FULFILLMENT = constructThresholdFulfillment();

  private Fulfillment fulfillment;

  /**
   * Required-args Constructor (used by JUnit's parameterized test annotation).
   *
   * @param encodingToUse        A {@link Encoding} to use for each test run.
   * @param expectedEncodedValue A {@link String} encoded in the above encoding to assert against.
   * @param fulfillment          A {@link Fulfillment} to encode and decode for each test run.
   */
  public CryptoConditionsModuleFulfillmentTest(
      final Encoding encodingToUse, final String expectedEncodedValue,
      final Fulfillment fulfillment
  ) {
    super(encodingToUse, expectedEncodedValue);
    this.fulfillment = Objects.requireNonNull(fulfillment);
  }

  @Parameters
  public static Collection<Object[]> data() {
    // Create and return a Collection of Object arrays. Each element in each array is a parameter
    // to the CryptoFulfillmentsModuleFulfillmentTest constructor.
    return Arrays.asList(new Object[][]{

        // Fulfillment
        {Encoding.HEX, PREIMAGE_FULFILLMENT_DER_BYTES_HEX, FULFILLMENT},
        {Encoding.BASE64, PREIMAGE_FULFILLMENT_DER_BYTES_BASE64, FULFILLMENT},
        {Encoding.BASE64_WITHOUT_PADDING,
            PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING,
            FULFILLMENT
        },
        {Encoding.BASE64URL, PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL, FULFILLMENT},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            FULFILLMENT
        },

        // Preimage
        {Encoding.HEX, PREIMAGE_FULFILLMENT_DER_BYTES_HEX, PREIMAGE_FULFILLMENT},
        {Encoding.BASE64, PREIMAGE_FULFILLMENT_DER_BYTES_BASE64, PREIMAGE_FULFILLMENT},
        {Encoding.BASE64_WITHOUT_PADDING,
            PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING,
            PREIMAGE_FULFILLMENT
        },
        {Encoding.BASE64URL, PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL, PREIMAGE_FULFILLMENT},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            PREIMAGE_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            PREIMAGE_FULFILLMENT
        },

        // Prefix
        {Encoding.HEX, PREFIX_FULFILLMENT_DER_BYTES_HEX, PREFIX_FULFILLMENT},
        {Encoding.BASE64, PREFIX_FULFILLMENT_DER_BYTES_BASE64, PREFIX_FULFILLMENT},
        {Encoding.BASE64_WITHOUT_PADDING,
            PREFIX_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING,
            PREFIX_FULFILLMENT
        },
        {Encoding.BASE64URL, PREFIX_FULFILLMENT_DER_BYTES_BASE64_URL, PREFIX_FULFILLMENT},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            PREFIX_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            PREFIX_FULFILLMENT
        },

        // RSA
        {Encoding.HEX, RSA_FULFILLMENT_DER_BYTES_HEX, RSA_FULFILLMENT},
        {Encoding.BASE64, RSA_FULFILLMENT_DER_BYTES_BASE64, RSA_FULFILLMENT},
        {Encoding.BASE64_WITHOUT_PADDING,
            RSA_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING,
            RSA_FULFILLMENT
        },
        {Encoding.BASE64URL, RSA_FULFILLMENT_DER_BYTES_BASE64_URL, RSA_FULFILLMENT},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            RSA_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            RSA_FULFILLMENT
        },

        // Ed25519
        {Encoding.HEX, ED25519_FULFILLMENT_DER_BYTES_HEX, ED25519_FULFILLMENT},
        {Encoding.BASE64, ED25519_FULFILLMENT_DER_BYTES_BASE64, ED25519_FULFILLMENT},
        {Encoding.BASE64_WITHOUT_PADDING,
            ED25519_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING,
            ED25519_FULFILLMENT
        },
        {Encoding.BASE64URL, ED25519_FULFILLMENT_DER_BYTES_BASE64_URL, ED25519_FULFILLMENT},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            ED25519_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            ED25519_FULFILLMENT
        },

        // Threshold
        {Encoding.HEX, THRESHOLD_FULFILLMENT_DER_BYTES_HEX, THRESHOLD_FULFILLMENT},
        {Encoding.BASE64, THRESHOLD_FULFILLMENT_DER_BYTES_BASE64, THRESHOLD_FULFILLMENT},
        {Encoding.BASE64_WITHOUT_PADDING,
            THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_WITHOUTPADDING,
            THRESHOLD_FULFILLMENT
        },
        {Encoding.BASE64URL, THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_URL, THRESHOLD_FULFILLMENT},
        {Encoding.BASE64URL_WITHOUT_PADDING,
            THRESHOLD_FULFILLMENT_DER_BYTES_BASE64_URL_WITHOUTPADDING,
            THRESHOLD_FULFILLMENT
        }
    });

  }

  //////////////////
  // Private Helpers
  //////////////////

  private static Fulfillment constructFulfillment() {
    return constructPreimageFulfillment();
  }

  private static PreimageSha256Fulfillment constructPreimageFulfillment() {
    final byte[] preimage = "you built a time machine out of a DeLorean?".getBytes();
    return new PreimageSha256Fulfillment(preimage);
  }

  private static PrefixSha256Fulfillment constructPrefixFulfillment() {
    final byte[] prefix = "I'm your density. I mean, your destiny.".getBytes();
    return new PrefixSha256Fulfillment(prefix, 20, constructPreimageFulfillment());
  }

  private static RsaSha256Fulfillment constructRsaFulfillment() {
    try {

      final KeyPair rsaKeyPair = constructRsaKeyPair();
      Signature rsaSigner = Signature.getInstance("SHA256withRSA/PSS");
      rsaSigner.initSign(rsaKeyPair.getPrivate());

      final byte[] message = "Marty, your acting like you haven't seen me in a week.".getBytes();
      rsaSigner.update(message);

      return new RsaSha256Fulfillment(
          (RSAPublicKey) rsaKeyPair.getPublic(),
          "signature".getBytes()
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Ed25519Sha256Fulfillment constructEd25519Fulfillment() {
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

      final byte[] signature = "5VZDAMNgrHKQhuLMgG6CioSHfx645dl02HPgZSJJAVVfuIIVkKM7rMYeOXAc-bRr0lv18FlbviRlUUFDjnoQCw"
          .getBytes();
      return new Ed25519Sha256Fulfillment((EdDSAPublicKey) edDsaKeyPair.getPublic(), signature);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static ThresholdSha256Fulfillment constructThresholdFulfillment() {
    return new ThresholdSha256Fulfillment(
        Lists.newArrayList(),
        Lists.newArrayList(
            constructPreimageFulfillment(), constructRsaFulfillment(), constructPrefixFulfillment()
        )
    );
  }

  @Before
  public void setup() {
    this.objectMapper = new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new CryptoConditionsModule(encodingToUse));

    this.objectMapper = this.objectMapper.setSerializationInclusion(Include.NON_EMPTY);
  }

  @Test
  public void testSerializeDeserialize() throws IOException {
    final CryptoConditionsContainer expectedContainer = new CryptoConditionsContainer(
        Optional.empty(),
        Optional.of(fulfillment)
    );

    final String json = objectMapper.writeValueAsString(expectedContainer);
    assertThat(json, is(
        String.format("{\"fulfillment\":\"%s\"}", expectedEncodedValue)
    ));

    final CryptoConditionsContainer actualAddressContainer = objectMapper
        .readValue(json, CryptoConditionsContainer.class);

    assertThat(actualAddressContainer, is(expectedContainer));
    assertThat(actualAddressContainer.getFulfillment().get(), is(fulfillment));
  }
}