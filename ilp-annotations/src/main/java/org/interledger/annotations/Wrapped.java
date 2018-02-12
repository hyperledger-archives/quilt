package org.interledger.annotations;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Style(
    // Detect names starting with "Wrapped"
    typeAbstract = "Wrapped*",
    // Generate without any suffix, just raw detected name
    typeImmutable = "*",
    // Make generated public, leave "Wrapped" as package private
    visibility = ImplementationVisibility.PUBLIC,
    // Seems unnecessary to have builder or superfluous copy method
    defaults = @Value.Immutable(builder = false, copy = false))
public @interface Wrapped {

}
