package org.interledger.quilt.jackson.conditions;

import org.interledger.core.Fulfillment;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

/**
 * Jackson serializer {@link Fulfillment} using configurable encodings.
 */
public class FulfillmentSerializer extends StdScalarSerializer<Fulfillment> {

  private final Encoding encoding;

  /**
   * Required-args Constructor.
   *
   * @param encoding The {@link Encoding} to use for serialization and deserialization of conditions
   *                 and fulfillments.
   */
  public FulfillmentSerializer(final Encoding encoding) {
    super(Fulfillment.class, false);
    this.encoding = Objects.requireNonNull(encoding, "Encoding must not be null!");
  }

  @Override
  public void serialize(Fulfillment fulfillment, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    switch (encoding) {
      case HEX: {
        gen.writeString(
            BaseEncoding.base16().encode(fulfillment.getPreimage())
        );
        break;
      }
      case BASE64: {
        gen.writeString(
            SerializerUtils.encodeBase64(Base64.getEncoder(), fulfillment)
        );
        break;
      }
      case BASE64_WITHOUT_PADDING: {
        gen.writeString(
            SerializerUtils.encodeBase64(Base64.getEncoder().withoutPadding(), fulfillment)
        );
        break;
      }
      case BASE64URL: {
        gen.writeString(
            SerializerUtils.encodeBase64(Base64.getUrlEncoder(), fulfillment)
        );
        break;
      }
      case BASE64URL_WITHOUT_PADDING: {
        gen.writeString(
            SerializerUtils.encodeBase64(Base64.getUrlEncoder().withoutPadding(), fulfillment)
        );
        break;
      }
      default: {
        throw new RuntimeException("Unhandled Encoding!");
      }
    }
  }
}
