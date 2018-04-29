package org.interledger.quilt.jackson;

import org.interledger.core.Condition;
import org.interledger.core.Fulfillment;
import org.interledger.core.InterledgerAddress;
import org.interledger.quilt.jackson.address.InterledgerAddressDeserializer;
import org.interledger.quilt.jackson.conditions.ConditionDeserializer;
import org.interledger.quilt.jackson.conditions.Encoding;
import org.interledger.quilt.jackson.conditions.FulfillmentDeserializer;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
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
  public JsonDeserializer<?> findBeanDeserializer(
      JavaType type, DeserializationConfig config, BeanDescription beanDesc
  ) {
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
