package org.interledger.codecs.asn;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * An ASN1 GeneralizedTime object represented as an {@link Instant}.
 */
public class AsnGeneralizedTime extends AsnIA5StringBasedObject<Instant> {

  protected DateTimeFormatter generalizedTimeFormatter;

  /**
   * Constructs a new instance of {@link AsnGeneralizedTime}.
   */
  public AsnGeneralizedTime() {
    super(new AsnSizeConstraint(19));
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
  protected Instant decode() {

    try {
      return Instant.from(generalizedTimeFormatter.parse(getCharString()));
    } catch (DateTimeParseException dtp) {
      throw new IllegalArgumentException(
          "Interledger GeneralizedTime only supports values in the format 'YYYYMMDDHHMMSS.fffZ', "
              + "value " + getCharString() + " is invalid.",
          dtp);
    }
  }

  @Override
  protected void encode(Instant value) {
    setCharString(generalizedTimeFormatter.format(value));
  }

}