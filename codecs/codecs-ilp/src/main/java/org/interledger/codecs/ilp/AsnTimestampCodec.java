package org.interledger.codecs.ilp;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core Codecs
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import static org.interledger.encoding.asn.codecs.AsnSizeConstraint.UNCONSTRAINED;

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
 * <p>An Interledger Timestamp represented internally as an {@link Instant}.</p>
 *
 * <p>Interledger uses ISO-8601 and not POSIX time, because ISO-8601 increases monotonically and never "travels back in
 * time", which could cause issues with transfer expiry date-time values. ISO-8601 is also one of the most widely
 * supported and most well-defined date formats, as of 2017.</p>
 *
 * <p>The wire format for Interledger date/time values leaves out any fixed/redundant characters, such as hyphens,
 * colons, the "T" separator, the decimal period, and the "Z" timezone indicator.
 *
 * <p>The wire format is four digits for the year, two digits for the month, two digits for the day, two digits for the
 * hour, two digits for the minutes, two digits for the seconds and three digits for the milliseconds.</p>
 *
 * <p>I.e. the wire format is: <tt>YYYYMMDDHHmmSSfff</tt></p>
 *
 * <p>All date-time values MUST be expressed in UTC time.</p>
 */
public class AsnTimestampCodec extends AsnPrintableStringBasedObjectCodec<Instant> {

  private final DateTimeFormatter interledgerTimestampFormatter;

  /**
   * No-args constructor.
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
          String.format(
              "Interledger timestamps must conform to IL-RFC-27! Value %s is invalid.",
              getCharString()
          ),
          dtp
      );
    }
  }

  @Override
  public void encode(Instant value) {
    setCharString(interledgerTimestampFormatter.format(value));
  }

}
