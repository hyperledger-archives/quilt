package org.interledger.quilt.jackson.conditions;

import org.interledger.core.Fulfillment;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/**
 * Jackson deserializer for {@link Fulfillment} using configurable encodings.
 */
public class FulfillmentDeserializer extends StdScalarDeserializer<Fulfillment> {

  private final Encoding encoding;

  /**
   * Required-args Constructor.
   *
   * @param encoding The {@link Encoding} to use for serialization and deserialization of conditions
   *                 and fulfillments.
   */
  public FulfillmentDeserializer(final Encoding encoding) {
    super(String.class);
    this.encoding = Objects.requireNonNull(encoding, "Encoding must not be null!");
  }

  @Override
  public Fulfillment deserialize(JsonParser jsonParser, DeserializationContext ctxt)
      throws IOException {
    switch (encoding) {
      case HEX: {
        return Fulfillment.of(
            BaseEncoding.base16().decode(jsonParser.getText().toUpperCase(Locale.US))
        );
      }
      case BASE64:
      case BASE64_WITHOUT_PADDING: {
        return Fulfillment.of(Base64.getDecoder().decode(jsonParser.getText()));
      }
      case BASE64URL:
      case BASE64URL_WITHOUT_PADDING: {
        return Fulfillment.of(Base64.getUrlDecoder().decode(jsonParser.getText()));
      }
      default: {
        throw new RuntimeException("Unhandled Fulfillment Encoding!");
      }
    }
  }
}
