package org.interledger.quilt.jackson.cryptoconditions;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.CryptoConditionReader;
import org.interledger.cryptoconditions.der.DerEncodingException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/**
 * Jackson deserializer for {@link Condition} using configurable encodings.
 */
public class ConditionDeserializer extends StdScalarDeserializer<Condition> {

  private final Encoding encoding;

  /**
   * Required-args Constructor.
   *
   * @param encoding The {@link Encoding} to use for serialization and deserialization of conditions
   *                 and fulfillments.
   */
  public ConditionDeserializer(final Encoding encoding) {
    super(String.class);
    this.encoding = Objects.requireNonNull(encoding, "Encoding must not be null!");
  }

  @Override
  public Condition deserialize(JsonParser jsonParser, DeserializationContext ctxt)
      throws IOException {
    try {
      switch (encoding) {
        case HEX: {
          return CryptoConditionReader.readCondition(
              BaseEncoding.base16().decode(jsonParser.getText().toUpperCase(Locale.US))
          );
        }
        case BASE64:
        case BASE64_WITHOUT_PADDING: {
          return CryptoConditionReader
              .readCondition(Base64.getDecoder().decode(jsonParser.getText()));
        }
        case BASE64URL:
        case BASE64URL_WITHOUT_PADDING: {
          return CryptoConditionReader
              .readCondition(Base64.getUrlDecoder().decode(jsonParser.getText()));
        }
        default: {
          throw new RuntimeException("Unhandled Condition Encoding!");
        }
      }
    } catch (DerEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}