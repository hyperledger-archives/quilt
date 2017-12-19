package org.interledger.quilt.jackson.cryptoconditions;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.CryptoConditionReader;
import org.interledger.cryptoconditions.CryptoConditionWriter;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.der.DerEncodingException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;

/**
 * A Jackson {@link SimpleModule} for serializing and deserializing Crypto Condition objects like
 * {@link Condition} and {@link Fulfillment}.
 */
public class CryptoConditionsModule extends SimpleModule {

  private static final String NAME = "CryptoConditionsModule";

  /**
   * Default Constructor. Specifies an encoding of {@link Encoding#BASE64_WITHOUT_PADDING} by
   * default.
   */
  public CryptoConditionsModule() {
    this(Encoding.BASE64_WITHOUT_PADDING);
  }

  /**
   * Required-args Constructor.
   *
   * @param encoding The {@link Encoding} to use for serialization and deserialization of conditions
   *                 and fulfillments.
   */
  public CryptoConditionsModule(final Encoding encoding) {
    super(
        NAME,
        new Version(1, 0, 0, null, "org.interledger", "jackson-datatype-cryptoconditions")
    );

    Objects.requireNonNull(encoding, "Encoding must not be null!");
    addSerializer(Condition.class, new ConditionSerializer(encoding));
    addDeserializer(Condition.class, new ConditionDeserializer(encoding));
    addSerializer(Fulfillment.class, new FulfillmentSerializer(encoding));
    addDeserializer(Fulfillment.class, new FulfillmentDeserializer(encoding));
  }

  /**
   * Indicates the type of encoding and decoding to use when serializing and deserializing a {@link
   * Condition} or {@link Fulfillment}.
   */
  public enum Encoding {
    HEX,
    BASE64,
    BASE64_WITHOUT_PADDING,
    BASE64URL,
    BASE64URL_WITHOUT_PADDING
  }

  /**
   * Jackson serializer {@link Condition} using configurable encodings.
   */
  public static class ConditionSerializer extends StdScalarSerializer<Condition> {

    private final Encoding encoding;

    /**
     * Required-args Constructor.
     *
     * @param encoding The {@link Encoding} to use for serialization and deserialization of
     *                 conditions and fulfillments.
     */
    public ConditionSerializer(final Encoding encoding) {
      super(Condition.class, false);
      this.encoding = Objects.requireNonNull(encoding, "Encoding must not be null!");
    }

    @Override
    public void serialize(Condition condition, JsonGenerator gen, SerializerProvider provider)
        throws IOException {

      try {
        switch (encoding) {
          case HEX: {
            gen.writeString(
                BaseEncoding.base16().encode(CryptoConditionWriter.writeCondition(condition))
            );
            break;
          }
          case BASE64: {
            gen.writeString(
                this.encodeBase64(Base64.getEncoder(), condition)
            );
            break;
          }
          case BASE64_WITHOUT_PADDING: {
            gen.writeString(
                this.encodeBase64(Base64.getEncoder().withoutPadding(), condition)
            );
            break;
          }
          case BASE64URL: {
            gen.writeString(
                this.encodeBase64(Base64.getUrlEncoder(), condition)
            );
            break;
          }
          case BASE64URL_WITHOUT_PADDING: {
            gen.writeString(
                this.encodeBase64(Base64.getUrlEncoder().withoutPadding(), condition)
            );
            break;
          }
          default: {
            throw new RuntimeException("Unhandled Encoding!");
          }
        }
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Helper method to encode a {@link Condition} using the supplied encoder, which migh be Base64
     * or Base64Url, with or without padding.
     *
     * @param encoder   A {@link Base64.Encoder} to encode with.
     * @param condition A {@link Condition} to encode into Base64 using the supplied encoder.
     *
     * @throws RuntimeException if a {@link DerEncodingException} is encountered.
     */
    private String encodeBase64(final Base64.Encoder encoder, final Condition condition) {
      Objects.requireNonNull(encoder);
      Objects.requireNonNull(condition);

      try {
        return encoder.encodeToString(CryptoConditionWriter.writeCondition(condition));
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Jackson deserializer for {@link Condition} using configurable encodings.
   */
  public static class ConditionDeserializer extends StdScalarDeserializer<Condition> {

    private final Encoding encoding;

    /**
     * Required-args Constructor.
     *
     * @param encoding The {@link Encoding} to use for serialization and deserialization of
     *                 conditions and fulfillments.
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

  /**
   * Jackson serializer {@link Fulfillment} using configurable encodings.
   */
  public static class FulfillmentSerializer extends StdScalarSerializer<Fulfillment> {

    private final Encoding encoding;

    /**
     * Required-args Constructor.
     *
     * @param encoding The {@link Encoding} to use for serialization and deserialization of
     *                 conditions and fulfillments.
     */
    public FulfillmentSerializer(final Encoding encoding) {
      super(Fulfillment.class, false);
      this.encoding = Objects.requireNonNull(encoding, "Encoding must not be null!");
    }

    @Override
    public void serialize(Fulfillment fulfillment, JsonGenerator gen, SerializerProvider provider)
        throws IOException {

      try {
        switch (encoding) {
          case HEX: {
            gen.writeString(
                BaseEncoding.base16().encode(CryptoConditionWriter.writeFulfillment(fulfillment))
            );
            break;
          }
          case BASE64: {
            gen.writeString(
                this.encodeBase64(Base64.getEncoder(), fulfillment)
            );
            break;
          }
          case BASE64_WITHOUT_PADDING: {
            gen.writeString(
                this.encodeBase64(Base64.getEncoder().withoutPadding(), fulfillment)
            );
            break;
          }
          case BASE64URL: {
            gen.writeString(
                this.encodeBase64(Base64.getUrlEncoder(), fulfillment)
            );
            break;
          }
          case BASE64URL_WITHOUT_PADDING: {
            gen.writeString(
                this.encodeBase64(Base64.getUrlEncoder().withoutPadding(), fulfillment)
            );
            break;
          }
          default: {
            throw new RuntimeException("Unhandled Encoding!");
          }
        }
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    /**
     * Helper method to encode a {@link Fulfillment} using the supplied encoder, which migh be
     * Base64 or Base64Url, with or without padding.
     *
     * @param encoder     A {@link Base64.Encoder} to encode with.
     * @param fulfillment A {@link Fulfillment} to encode into Base64 using the supplied encoder.
     *
     * @throws RuntimeException if a {@link DerEncodingException} is encountered.
     */
    private String encodeBase64(final Base64.Encoder encoder, final Fulfillment fulfillment) {
      Objects.requireNonNull(encoder);
      Objects.requireNonNull(fulfillment);

      try {
        return encoder.encodeToString(CryptoConditionWriter.writeFulfillment(fulfillment));
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Jackson deserializer for {@link Fulfillment} using configurable encodings.
   */
  public static class FulfillmentDeserializer extends StdScalarDeserializer<Fulfillment> {

    private final Encoding encoding;

    /**
     * Required-args Constructor.
     *
     * @param encoding The {@link Encoding} to use for serialization and deserialization of
     *                 conditions and fulfillments.
     */
    public FulfillmentDeserializer(final Encoding encoding) {
      super(String.class);
      this.encoding = Objects.requireNonNull(encoding, "Encoding must not be null!");
    }

    @Override
    public Fulfillment deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        throws IOException {
      try {
        switch (encoding) {
          case HEX: {
            return CryptoConditionReader.readFulfillment(
                BaseEncoding.base16().decode(jsonParser.getText().toUpperCase(Locale.US))
            );
          }
          case BASE64:
          case BASE64_WITHOUT_PADDING: {
            return CryptoConditionReader
                .readFulfillment(Base64.getDecoder().decode(jsonParser.getText()));
          }
          case BASE64URL:
          case BASE64URL_WITHOUT_PADDING: {
            return CryptoConditionReader
                .readFulfillment(Base64.getUrlDecoder().decode(jsonParser.getText()));
          }
          default: {
            throw new RuntimeException("Unhandled Fulfillment Encoding!");
          }
        }
      } catch (DerEncodingException e) {
        throw new RuntimeException(e);
      }
    }
  }

}