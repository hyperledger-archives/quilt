package org.interledger.quilt.jackson.conditions;

/*-
 * ========================LICENSE_START=================================
 * Interledger Jackson Datatypes
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import static org.interledger.quilt.jackson.conditions.Encoding.BASE64;

import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;

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
 * {@link InterledgerCondition} and {@link InterledgerFulfillment}.
 */
public class ConditionModule extends SimpleModule {

  private static final String NAME = ConditionModule.class.getName();

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

    addSerializer(InterledgerCondition.class, new ConditionSerializer(encoding));
    addDeserializer(InterledgerCondition.class, new ConditionDeserializer(encoding));
    addSerializer(InterledgerFulfillment.class, new FulfillmentSerializer(encoding));
    addDeserializer(InterledgerFulfillment.class, new FulfillmentDeserializer(encoding));
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
        if (InterledgerCondition.class.isAssignableFrom(beanDesc.getType().getRawClass())) {
          return new ConditionSerializer(encoding);
        } else if (InterledgerFulfillment.class
            .isAssignableFrom(beanDesc.getType().getRawClass())) {
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
        if (InterledgerCondition.class.isAssignableFrom(beanDesc.getType().getRawClass())) {
          return new ConditionDeserializer(encoding);
        } else if (InterledgerFulfillment.class
            .isAssignableFrom(beanDesc.getType().getRawClass())) {
          return new FulfillmentDeserializer(encoding);
        } else {
          return super.modifyDeserializer(config, beanDesc, deserializer);
        }
      }
    });
  }
}
