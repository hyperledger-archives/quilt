package org.interledger.codecs.btp;

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

import org.interledger.encoding.asn.codecs.AsnPrintableStringBasedObjectCodec;
import org.interledger.encoding.asn.codecs.AsnSizeConstraint;
import org.interledger.encoding.asn.framework.CodecException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.regex.Pattern;

/**
 * <p>An Interledger BTP 2.0 Timestamp represented internally as an {@link Instant}.</p>
 *
 * <p>BTP timestamps are encoded using ASN.1 GeneralizedTime. This is a shortened and slightly
 * restricted variant of ISO 8601 encoding. Once the date string is derived it is encoded as a variable-length octet
 * string.</p>
 *
 * <p>When encoding an <tt>ISO-8601</tt> timestamp, the hyphens, colons and T character MUST be
 * removed. The date MUST end in Z, denoting UTC time, local timezones are not allowed. Timestamps MAY use up to
 * millisecond precision. The period . MUST be used as the decimal separator. If the millisecond part is zero, it MUST
 * be left out. Trailing zeros in the millisecond part MUST be left out. Years MUST be given as four digits and MUST NOT
 * be left out. Months, day, hours, minutes and seconds MUST be given as two digits and MUST NOT be left out. Midnight
 * MUST be encoded as 000000 on the following day. Leap seconds MUST be encoded using 60 as the value for seconds.</p>
 *
 * <p>Note that BTP times are encoded slightly different from Interledger timestamps. See <tt>AsnTimestampCodec</tt> in
 * <tt>ilp-core</tt> for more details.</p>
 */
public class AsnBtpGeneralizedTimeCodec extends AsnPrintableStringBasedObjectCodec<Instant> {

  private static final String GENERALIZED_TIME_REGEX =
      "^([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2}(\\.\\d{1,3})?Z)$";

  private final DateTimeFormatter generalizedTimeTimestampFormatter;

  /**
   * No-args constructor.
   */
  public AsnBtpGeneralizedTimeCodec() {
    super(AsnSizeConstraint.UNCONSTRAINED);
    setValidator(Pattern.compile(GENERALIZED_TIME_REGEX).asPredicate());

    this.generalizedTimeTimestampFormatter = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4)
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
        .appendZoneId()
        .toFormatter()
        .withZone(ZoneId.of("Z"));
  }

  @Override
  public Instant decode() {
    try {
      return Instant.from(generalizedTimeTimestampFormatter.parse(getCharString()));
    } catch (DateTimeParseException dtp) {
      throw new CodecException(
          String.format(
              "Invalid format: BTP timestamps must conform to IL-RFC-23! Value %s is invalid.",
              getCharString()
          ),
          dtp
      );
    }
  }

  @Override
  public void encode(Instant value) {
    setCharString(generalizedTimeTimestampFormatter.format(value));
  }

}
