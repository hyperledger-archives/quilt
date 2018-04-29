package org.interledger.quilt.jackson;

import org.interledger.core.Condition;
import org.interledger.core.Fulfillment;
import org.interledger.core.InterledgerAddress;
import org.interledger.quilt.jackson.conditions.ConditionSerializer;
import org.interledger.quilt.jackson.conditions.Encoding;
import org.interledger.quilt.jackson.conditions.FulfillmentSerializer;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.Serializers;

import java.util.Objects;

public class InterledgerSerializers extends Serializers.Base {

  /**
   * The type of encoding that should be used to serialize and deserialize crypto-condtions.
   */
  private final Encoding cryptoConditionEncoding;

  public InterledgerSerializers(final Encoding cryptoConditionEncoding) {
    this.cryptoConditionEncoding = Objects.requireNonNull(cryptoConditionEncoding);
  }

  @Override
  public JsonSerializer<?> findSerializer(
      SerializationConfig config, JavaType type, BeanDescription beanDesc
  ) {
    final Class<?> raw = type.getRawClass();
    if (InterledgerAddress.class.isAssignableFrom(raw)) {
      return InterledgerAddressSerializer.INSTANCE;
    }
    if (Condition.class.isAssignableFrom(raw)) {
      return new ConditionSerializer(cryptoConditionEncoding);
    }
    if (Fulfillment.class.isAssignableFrom(raw)) {
      return new FulfillmentSerializer(cryptoConditionEncoding);
    }
    return null;
  }

}
