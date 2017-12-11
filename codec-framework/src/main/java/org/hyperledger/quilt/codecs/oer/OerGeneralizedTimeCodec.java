package org.hyperledger.quilt.codecs.oer;

import org.hyperledger.quilt.codecs.framework.Codec;
import org.hyperledger.quilt.codecs.framework.CodecContext;
import org.hyperledger.quilt.codecs.oer.OerGeneralizedTimeCodec.OerGeneralizedTime;
import org.hyperledger.quilt.codecs.oer.OerIA5StringCodec.OerIA5String;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Objects;

/**
 * <p>An extension of {@link Codec} for reading and writing an ASN.1 OER GeneralizedTime. An ASN.1
 * GeneralizedTime object is used to represent time values with a higher precision than done by the
 * UTCTime ASN.1 type (allows a precision down to seconds). The ASN.1 GeneralizedTime syntax can
 * include fraction-of-second details, and can be expressed in several formats. However, the
 * interledger specs <b>mandate</b> that time be represented as a string in the format
 * 'YYYYMMDDHHmmSS.fffZ', where the following hold true:</p>
 *
 *  <ul>
 *    <li>yyyy - is the four digit year, e.g. 2017</li>
 *    <li>mm - is the two digit month, e.g. 07</li>
 *    <li>dd - is the two digit day, e.g. 04</li>
 *    <li>hh - is the two digit hour of the day, e.g. 21</li>
 *    <li>mm - is the two digit minute, e.g. 09</li>
 *    <li>ss - is the two digit second, e.g. 21</li>
 *    <li>. - is the literal '.' character</li>
 *    <li>fff - is the three digit millisecond, e.g. 000</li>
 *    <li>z - is the literal 'z' character indicating that the time is represented in the utc + 0
 *        timezone</li>
 *  </ul>
 */
public class OerGeneralizedTimeCodec implements Codec<OerGeneralizedTime> {

  protected DateTimeFormatter generalizedTimeFormatter;

  /**
   * Constructs a new instance of {@link OerGeneralizedTimeCodec}.
   */
  public OerGeneralizedTimeCodec() {
    this.generalizedTimeFormatter = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .parseStrict()
        .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, true)
        .appendZoneId()
        .toFormatter()
        .withZone(ZoneId.of("Z"));
  }

  @Override
  public OerGeneralizedTime read(CodecContext context, InputStream inputStream) throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(inputStream);

    final String timeString = context.read(OerIA5String.class, inputStream).getValue();

    if (timeString.length() != 19 || !timeString.endsWith("Z")) {
      throw new IllegalArgumentException(
          "Interledger GeneralizedTime only supports values in the format 'YYYYMMDDTHHMMSS.fffZ',"
              + " value " + timeString + " is invalid.");
    }

    try {
      final Instant value = Instant.from(generalizedTimeFormatter.parse(timeString));
      return new OerGeneralizedTime(value);
    } catch (DateTimeParseException dtp) {
      throw new IllegalArgumentException(
          "Interledger GeneralizedTime only supports values in the format 'YYYYMMDDTHHMMSS.fffZ', "
              + "value " + timeString + " is invalid.",
          dtp);
    }
  }

  @Override
  public void write(CodecContext context, OerGeneralizedTime instance, OutputStream outputStream)
      throws IOException {
    Objects.requireNonNull(context);
    Objects.requireNonNull(instance);
    Objects.requireNonNull(outputStream);

    final String formattedTime = generalizedTimeFormatter.format(instance.getValue());
    context.write(new OerIA5String(formattedTime), outputStream);
  }


  /**
   * A typing mechanism for registering multiple codecs that operate on the same type, in this case
   * {@link ZonedDateTime}.
   */
  public static class OerGeneralizedTime {

    private final Instant value;

    public OerGeneralizedTime(final Instant value) {
      this.value = Objects.requireNonNull(value);
    }

    public Instant getValue() {
      return this.value;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      OerGeneralizedTime other = (OerGeneralizedTime) obj;

      return value.equals(other.value);
    }

    @Override
    public int hashCode() {
      return this.value.hashCode();
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("OerGeneralizedTime{");
      sb.append("value=").append(value);
      sb.append('}');
      return sb.toString();
    }
  }

}
