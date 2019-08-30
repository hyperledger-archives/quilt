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

import org.interledger.quilt.jackson.conditions.Encoding;

import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.Objects;

/**
 * A Jackson {@link SimpleModule} for serializing and deserializing Interledger objects.
 */
public class InterledgerModule extends SimpleModule {

  private static final String NAME = InterledgerModule.class.getName();
  private static final VersionUtil VERSION_UTIL = new VersionUtil() {
  };

  /**
   * The type of encoding that should be used to serialize and deserialize crypto-conditions.
   */
  private final Encoding cryptoConditionEncoding;

  public InterledgerModule(final Encoding cryptoConditionEncoding) {
    super(NAME, VERSION_UTIL.version());
    this.cryptoConditionEncoding = Objects.requireNonNull(cryptoConditionEncoding);
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addSerializers(new InterledgerSerializers(cryptoConditionEncoding));
    context.addDeserializers(new InterledgerDeserializers(cryptoConditionEncoding));
  }

}
