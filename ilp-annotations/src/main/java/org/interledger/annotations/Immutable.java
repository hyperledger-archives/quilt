package org.interledger.annotations;

/*-
 * ========================LICENSE_START=================================
 * Interledger Annotations
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

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Immutable annotation will generate builder which produces private implementations
 * of abstract value type.
 */
@Target(ElementType.TYPE)
@Value.Style(
    typeBuilder = "*Builder",
    visibility = Value.Style.ImplementationVisibility.PRIVATE,
    builderVisibility = Value.Style.BuilderVisibility.PUBLIC,
    defaults = @Value.Immutable())
public @interface Immutable {

}
