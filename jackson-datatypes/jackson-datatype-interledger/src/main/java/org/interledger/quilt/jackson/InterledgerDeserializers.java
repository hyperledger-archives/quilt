package org.interledger.quilt.jackson;

import org.interledger.core.InterledgerAddress;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.Deserializers;

import java.util.Objects;

public class InterledgerDeserializers extends Deserializers.Base {

  /**
   * The type of encoding that should be used to serialize and deserialize crypto-condtions.
   */
  private final Encoding cryptoConditionEncoding;

  public InterledgerDeserializers(final Encoding cryptoConditionEncoding) {
    this.cryptoConditionEncoding = Objects.requireNonNull(cryptoConditionEncoding);
  }

  @Override
  public JsonDeserializer<?> findBeanDeserializer(JavaType type,
                                                  DeserializationConfig config, BeanDescription beanDesc)
      throws JsonMappingException {
    final Class<?> raw = type.getRawClass();

    if (type.hasRawClass(InterledgerAddress.class)) {
      return new InterledgerAddressDeserializer();
    }

    if (type.hasRawClass(Condition.class)) {
      return new ConditionDeserializer(cryptoConditionEncoding);
    }

    if (type.hasRawClass(Fulfillment.class)) {
      return new FulfillmentDeserializer(cryptoConditionEncoding);
    }

    return null;
  }

}
