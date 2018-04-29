package org.interledger.quilt.jackson;

import org.interledger.quilt.jackson.conditions.Encoding;

import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.util.Objects;

/**
 * A Jackson {@link SimpleModule} for serializing and deserializing Interledger objects.
 */
public class InterledgerModule extends SimpleModule {

  private static final String NAME = "org.interledger.quilt.jackson.address.InterledgerAddressModule";
  private static final VersionUtil VERSION_UTIL = new VersionUtil() {
  };

  /**
   * The type of encoding that should be used to serialize and deserialize crypto-condtions.
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