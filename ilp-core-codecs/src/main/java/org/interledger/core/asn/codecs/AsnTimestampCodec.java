package org.interledger.core.asn.codecs;

import org.interledger.encoding.asn.codecs.AsnPrintableStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.regex.Pattern;

/**
 * An Interledger Timestamp represented internally as an {@link Instant}
 *
 * <p>Interledger uses ISO 8601 and not POSIX time, because ISO 8601 increases
 * monotonically and never "travels back in time" which could cause issues
 * with transfer expiries. It is also one of the most widely supported and most
 * well-defined date formats as of 2017.
 *
 * <p>The wire format leaves out any fixed/redundant characters, such as
 * hyphens, colons, the "T" separator, the decimal period and the "Z" timezone
 * indicator.
 *
 * <p>The wire format is four digits for the year, two digits for the month,
 * two digits for the day, two digits for the hour, two digits for the minutes,
 * two digits for the seconds and three digits for the milliseconds.
 *
 * <p>I.e. the wire format is: 'YYYYMMDDHHmmSSfff'
 *
 * <p>All times MUST be expressed in UTC time.
 */
public class AsnTimestampCodec extends AsnPrintableStringBasedObjectCodec<Instant> {

  private final DateTimeFormatter interledgerTimestampFormatter;

  /**
   * Default constructor.
   */
  public AsnTimestampCodec() {
    super(new AsnSizeConstraint(17));
    setValidator(Pattern.compile("[0-9]{17}").asPredicate());
    this.interledgerTimestampFormatter = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .appendFraction(ChronoField.MILLI_OF_SECOND, 3, 3, false)
        .toFormatter()
        .withZone(ZoneId.of("Z"));
  }

  @Override
  public Instant decode() {
    try {
      return Instant.from(interledgerTimestampFormatter.parse(getCharString()));
    } catch (DateTimeParseException dtp) {
      throw new IllegalArgumentException(
          "Interledger timestamps only support values in the format 'YYYYMMDDHHMMSSfff', "
              + "value " + getCharString() + " is invalid.",
          dtp);
    }
  }

  @Override
  public void encode(Instant value) {
    setCharString(interledgerTimestampFormatter.format(value));
  }


}
