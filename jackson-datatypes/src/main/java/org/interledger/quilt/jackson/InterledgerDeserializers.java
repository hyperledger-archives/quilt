package org.interledger.quilt.jackson;

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

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillment;
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
   * The type of encoding that should be used to serialize and deserialize crypto-conditions.
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

    if (type.hasRawClass(InterledgerCondition.class)) {
      return new ConditionDeserializer(cryptoConditionEncoding);
    }

    if (type.hasRawClass(InterledgerFulfillment.class)) {
      return new FulfillmentDeserializer(cryptoConditionEncoding);
    }

    return null;
  }

}
