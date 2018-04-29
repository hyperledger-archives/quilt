package org.interledger.quilt.jackson.conditions;

import org.interledger.core.Condition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.util.Base64;
import java.util.Objects;


/**
 * Jackson serializer {@link Condition} using configurable encodings.
 */
public class ConditionSerializer extends StdScalarSerializer<Condition> {

  private final Encoding encoding;

  /**
   * No-args Constructor.
   */
  public ConditionSerializer() {
    this(Encoding.BASE64);
  }

  /**
   * Required-args Constructor.
   *
   * @param encoding The {@link Encoding} to use for serialization and deserialization of conditions
   *                 and fulfillments.
   */
  public ConditionSerializer(final Encoding encoding) {
    super(Condition.class, false);
    this.encoding = Objects.requireNonNull(encoding, "Encoding must not be null!");
  }

  @Override
  public void serialize(Condition condition, JsonGenerator gen, SerializerProvider provider)
      throws IOException {

    switch (encoding) {
      case HEX: {
        gen.writeString(BaseEncoding.base16().encode(condition.getHash()));
        break;
      }
      case BASE64: {
        gen.writeString(
            SerializerUtils.encodeBase64(Base64.getEncoder(), condition)
        );
        break;
      }
      case BASE64_WITHOUT_PADDING: {
        gen.writeString(
            SerializerUtils.encodeBase64(Base64.getEncoder().withoutPadding(), condition)
        );
        break;
      }
      case BASE64URL: {
        gen.writeString(
            SerializerUtils.encodeBase64(Base64.getUrlEncoder(), condition)
        );
        break;
      }
      case BASE64URL_WITHOUT_PADDING: {
        gen.writeString(
            SerializerUtils.encodeBase64(Base64.getUrlEncoder().withoutPadding(), condition)
        );
        break;
      }
      default: {
        throw new RuntimeException("Unhandled Encoding!");
      }
    }
  }
}
