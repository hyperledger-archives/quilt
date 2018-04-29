package org.interledger.quilt.jackson.conditions;

import static org.interledger.quilt.jackson.conditions.Encoding.BASE64;

import org.interledger.core.Condition;
import org.interledger.core.Fulfillment;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.util.Objects;

/**
 * A Jackson {@link SimpleModule} for serializing and deserializing Crypto-Condition objects like
 * {@link Condition} and {@link Fulfillment}.
 */
public class ConditionModule extends SimpleModule {

  private static final String NAME = "ConditionModule";

  private final Encoding encoding;

  /**
   * Default Constructor. Specifies an encoding of {@link Encoding#BASE64} by default, since this is
   * the most compatible with various language libraries (e.g., openssl requires padding to work
   * properly).
   */
  public ConditionModule() {
    this(BASE64);
  }

  /**
   * Required-args Constructor.
   *
   * @param encoding The {@link Encoding} to use for serialization and deserialization of conditions
   *                 and fulfillments.
   */
  public ConditionModule(final Encoding encoding) {
    super(
        NAME,
        new Version(1, 0, 0, null, "org.interledger", "jackson-datatype-cryptoconditions")
    );

    this.encoding = Objects.requireNonNull(encoding, "Encoding must not be null!");

    addSerializer(Condition.class, new ConditionSerializer(encoding));
    addDeserializer(Condition.class, new ConditionDeserializer(encoding));
    addSerializer(Fulfillment.class, new FulfillmentSerializer(encoding));
    addDeserializer(Fulfillment.class, new FulfillmentDeserializer(encoding));
  }

  @Override
  public void setupModule(final SetupContext context) {
    context.addBeanSerializerModifier(new BeanSerializerModifier() {
      @Override
      public JsonSerializer<?> modifySerializer(
          SerializationConfig config,
          BeanDescription beanDesc,
          JsonSerializer<?> serializer
      ) {
        if (Condition.class.isAssignableFrom(beanDesc.getType().getRawClass())) {
          return new ConditionSerializer(encoding);
        } else if (Fulfillment.class.isAssignableFrom(beanDesc.getType().getRawClass())) {
          return new FulfillmentSerializer(encoding);
        } else {
          return serializer;
        }
      }
    });

    context.addBeanDeserializerModifier(new BeanDeserializerModifier() {
      @Override
      public JsonDeserializer<?> modifyDeserializer(
          DeserializationConfig config,
          BeanDescription beanDesc,
          JsonDeserializer<?> deserializer
      ) {
        if (Condition.class.isAssignableFrom(beanDesc.getType().getRawClass())) {
          return new ConditionDeserializer(encoding);
        } else if (Fulfillment.class.isAssignableFrom(beanDesc.getType().getRawClass())) {
          return new FulfillmentDeserializer(encoding);
        } else {
          return super.modifyDeserializer(config, beanDesc, deserializer);
        }
      }
    });
  }
}
