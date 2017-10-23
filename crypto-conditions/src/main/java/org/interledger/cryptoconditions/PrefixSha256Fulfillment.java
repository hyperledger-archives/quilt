package org.interledger.cryptoconditions;

import static org.interledger.cryptoconditions.CryptoConditionType.PREFIX_SHA256;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Implementation of a fulfillment based on a prefix, a sub fulfillment, and the SHA-256 function.
 */
public class PrefixSha256Fulfillment extends FulfillmentBase<PrefixSha256Condition>
    implements Fulfillment<PrefixSha256Condition> {

  private final byte[] prefix;
  private final String prefixBase64Url;
  private final long maxMessageLength;
  private final Fulfillment subfulfillment;
  private final PrefixSha256Condition condition;

  /**
   * Constructs an instance of the fulfillment.
   *
   * @param prefix           The prefix associated with the condition and fulfillment
   * @param maxMessageLength The maximum length of a message.
   * @param subfulfillment   The subfulfillments that this fulfillment depends on.
   */
  public PrefixSha256Fulfillment(
      final byte[] prefix, final long maxMessageLength, final Fulfillment subfulfillment
  ) {
    super(PREFIX_SHA256);
    Objects.requireNonNull(prefix, "Prefix must not be null!");
    Objects.requireNonNull(subfulfillment, "Subfulfillment must not be null!");

    this.prefix = Arrays.copyOf(prefix, prefix.length);
    this.prefixBase64Url = Base64.getUrlEncoder().encodeToString(prefix);
    this.maxMessageLength = maxMessageLength;
    // Fulfillments are immutable, so no need to perform any type of deep-copy here.
    this.subfulfillment = subfulfillment;

    this.condition = new PrefixSha256Condition(prefix, maxMessageLength,
        subfulfillment.getCondition());
  }

  @Override
  public final PrefixSha256Condition getCondition() {
    return this.condition;
  }

  /**
   * Accessor for the prefix as an array of bytes.
   *
   * @return A byte array containing the prefix for this fulfillment.
   *
   * @deprecated Java 8 does not have the concept of an immutable byte array, so this method allows
   *     external callers to accidentally or intentionally mute the prefix. As such, this method may
   *     be removed in a future version. Prefer {@link #getPrefixBase64Url()} instead.
   */
  @Deprecated
  public byte[] getPrefix() {
    return prefix;
  }

  /**
   * Accessor for the prefix as a Base64Url-encoded String.
   *
   * @return A {@link String} containing Base64Url characters.
   */
  public String getPrefixBase64Url() {
    return this.prefixBase64Url;
  }

  public long getMaxMessageLength() {
    return maxMessageLength;
  }

  public Fulfillment getSubfulfillment() {
    return subfulfillment;
  }

  @Override
  public boolean verify(final PrefixSha256Condition condition, final byte[] message) {
    Objects.requireNonNull(condition,
        "Can't verify a PrefixSha256Fulfillment against a null condition!");
    Objects.requireNonNull(message, "Message must not be null!");

    if (message.length > maxMessageLength) {
      throw new IllegalArgumentException(
          String
              .format("Message length (%s) exceeds maximum message length of (%s).", message.length,
                  maxMessageLength));
    }

    if (!getCondition().equals(condition)) {
      return false;
    }

    final Condition subcondition = subfulfillment.getCondition();
    final byte[] prefixedMessage = Arrays.copyOf(prefix, prefix.length + message.length);
    System.arraycopy(message, 0, prefixedMessage, prefix.length, message.length);

    return subfulfillment.verify(subcondition, prefixedMessage);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }

    PrefixSha256Fulfillment that = (PrefixSha256Fulfillment) object;

    if (maxMessageLength != that.maxMessageLength) {
      return false;
    }
    if (!Arrays.equals(prefix, that.prefix)) {
      return false;
    }
    if (!subfulfillment.equals(that.subfulfillment)) {
      return false;
    }
    return condition.equals(that.condition);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + Arrays.hashCode(prefix);
    result = 31 * result + (int) (maxMessageLength ^ (maxMessageLength >>> 32));
    result = 31 * result + subfulfillment.hashCode();
    result = 31 * result + condition.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("\nPrefixSha256Fulfillment{");
    sb.append("\nprefix=").append(prefixBase64Url);
    sb.append(", \n\tmaxMessageLength=").append(maxMessageLength);
    sb.append(", \n\tsubfulfillment=").append(subfulfillment);
    sb.append(", \n\tcondition=").append(condition);
    sb.append(", \n\ttype=").append(getType());
    sb.append("\n}");
    return sb.toString();
  }
}
