package org.interledger.quilt.jackson;

import org.interledger.InterledgerAddress;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.quilt.jackson.cryptoconditions.ConditionSerializer;
import org.interledger.quilt.jackson.cryptoconditions.Encoding;
import org.interledger.quilt.jackson.cryptoconditions.FulfillmentSerializer;

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
