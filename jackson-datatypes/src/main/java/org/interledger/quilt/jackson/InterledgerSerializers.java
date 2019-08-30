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
import org.interledger.quilt.jackson.address.InterledgerAddressSerializer;
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
   * The type of encoding that should be used to serialize and deserialize crypto-conditions.
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
    if (InterledgerCondition.class.isAssignableFrom(raw)) {
      return new ConditionSerializer(cryptoConditionEncoding);
    }
    if (InterledgerFulfillment.class.isAssignableFrom(raw)) {
      return new FulfillmentSerializer(cryptoConditionEncoding);
    }
    return null;
  }

}
