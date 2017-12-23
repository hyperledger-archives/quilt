package org.interledger.quilt.jackson;

import org.interledger.InterledgerAddress;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * A Jackson {@link SimpleModule} for serializing and deserializing {@link InterledgerAddress}
 * objects.
 */
public class InterledgerAddressModule extends SimpleModule {

  private static final String NAME = "InterledgerAddressModule";

  /**
   * No-args Constructor.
   */
  public InterledgerAddressModule() {

    super(
        NAME,
        new Version(1, 0, 0, null, "org.interledger", "jackson-datatype-interledger-address")
    );

    addSerializer(InterledgerAddress.class, InterledgerAddressSerializer.INSTANCE);
    addDeserializer(InterledgerAddress.class, InterledgerAddressDeserializer.INSTANCE);
  }

}