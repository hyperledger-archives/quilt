package org.interledger.annotations;

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