package org.interledger.quilt.jackson.address;

import org.interledger.core.InterledgerAddress;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson Serializer for {@link InterledgerAddress}.
 */
public class InterledgerAddressSerializer extends StdScalarSerializer<InterledgerAddress> {

  public static final InterledgerAddressSerializer INSTANCE = new InterledgerAddressSerializer();

  /**
   * No-args Constructor.
   */
  public InterledgerAddressSerializer() {
    super(InterledgerAddress.class, false);
  }

  @Override
  public void serialize(InterledgerAddress value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    gen.writeString(value.getValue());
  }
}