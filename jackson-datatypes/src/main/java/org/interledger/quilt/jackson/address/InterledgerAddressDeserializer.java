package org.interledger.quilt.jackson.address;

import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;

/**
 * An extension of {@link FromStringDeserializer} that deserializes a JSON string into an instance
 * of {@link InterledgerAddress}.
 */
public class InterledgerAddressDeserializer extends FromStringDeserializer<InterledgerAddress> {

  public static final InterledgerAddressDeserializer INSTANCE
      = new InterledgerAddressDeserializer();

  /**
   * No-args Constructor.
   */
  public InterledgerAddressDeserializer() {
    super(InterledgerAddress.class);
  }

  @Override
  protected InterledgerAddress _deserialize(
      final String value, final DeserializationContext deserializationContext
  ) {
    return InterledgerAddress.of(value);
  }
}