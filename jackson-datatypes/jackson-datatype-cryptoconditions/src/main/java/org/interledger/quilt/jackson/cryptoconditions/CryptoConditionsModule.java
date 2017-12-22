package org.interledger.quilt.jackson.cryptoconditions;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.Objects;

/**
 * A Jackson {@link SimpleModule} for serializing and deserializing Crypto-Condition objects like
 * {@link Condition} and {@link Fulfillment}.
 */
public class CryptoConditionsModule extends SimpleModule {

  private static final String NAME = "CryptoConditionsModule";

  /**
   * Default Constructor. Specifies an encoding of {@link Encoding#BASE64} by default, since this is
   * the most compatible with various language libraries (e.g., openssl requires padding to work
   * properly).
   */
  public CryptoConditionsModule() {
    this(Encoding.BASE64);
  }

  /**
   * Required-args Constructor.
   *
   * @param encoding The {@link Encoding} to use for serialization and deserialization of conditions
   *                 and fulfillments.
   */
  public CryptoConditionsModule(final Encoding encoding) {
    super(
        NAME,
        new Version(1, 0, 0, null, "org.interledger", "jackson-datatype-cryptoconditions")
    );

    Objects.requireNonNull(encoding, "Encoding must not be null!");
    addSerializer(Condition.class, new ConditionSerializer(encoding));
    addDeserializer(Condition.class, new ConditionDeserializer(encoding));
    addSerializer(Fulfillment.class, new FulfillmentSerializer(encoding));
    addDeserializer(Fulfillment.class, new FulfillmentDeserializer(encoding));
  }

}