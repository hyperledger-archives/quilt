package org.interledger.cryptoconditions;

import org.interledger.cryptoconditions.NamedInformationUri.HashFunction;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for parsing a uri-formatted crypto-condition.
 */
public class CryptoConditionUri {

  // This is a stricter version based on limitations of the current
  // implementation. Specifically, we can't handle bitmasks greater than 32 bits.
  public static final String SCHEME_PREFIX = "ni://";
  public static final String HASH_FUNCTION_NAME = "sha-256";

  public static final String CONDITION_REGEX_STRICT = "^" + SCHEME_PREFIX + "([A-Za-z0-9_-]?)/"
      + HASH_FUNCTION_NAME + ";([a-zA-Z0-9_-]{0,86})\\?(.+)$";

  /**
   * Parses a URI formatted crypto-condition.
   *
   * @param uri The crypto-condition formatted as a URI.
   *
   * @return The equivalent crypto-condition.
   *
   * @throws URISyntaxException if the URI syntax is invalid.
   */
  public static Condition parse(final URI uri) throws URISyntaxException {
    Objects.requireNonNull(uri);

    // based strongly on the five bells implementation at
    // https://github.com/interledgerjs/five-bells-condition
    // (7b6a97990cd3a51ee41b276c290e4ae65feb7882)

    if (!"ni".equals(uri.getScheme())) {
      throw new URISyntaxException(uri.toString(), "Serialized condition must start with 'ni:'");
    }

    // the regex covers the entire uri format including the 'ni:' scheme
    Matcher matcher = Pattern.compile(CONDITION_REGEX_STRICT).matcher(uri.toString());

    if (!matcher.matches()) {
      throw new URISyntaxException(uri.toString(), "Invalid condition format");
    }

    Map<String, List<String>> queryParams;
    try {
      queryParams = splitQuery(uri.getQuery());
    } catch (UnsupportedEncodingException x) {
      throw new URISyntaxException(uri.toString(), "Invalid condition format");
    }

    if (!queryParams.containsKey(QueryParams.TYPE)) {
      throw new URISyntaxException(uri.toString(), "No fingerprint type provided");
    }

    CryptoConditionType type = CryptoConditionType
        .fromString(queryParams.get(QueryParams.TYPE).get(0));

    long cost = 0;
    try {
      cost = Long.parseLong(queryParams.get(QueryParams.COST).get(0));
    } catch (NumberFormatException | NullPointerException x) {
      throw new URISyntaxException(uri.toString(), "No or invalid cost provided");
    }

    byte[] fingerprint = Base64.getUrlDecoder().decode(matcher.group(2));

    EnumSet<CryptoConditionType> subtypes = null;
    if (type == CryptoConditionType.PREFIX_SHA256 || type == CryptoConditionType.THRESHOLD_SHA256) {

      if (!queryParams.containsKey(QueryParams.SUBTYPES)) {
        throw new URISyntaxException(uri.toString(), "No subtypes provided");
      }

      subtypes =
          CryptoConditionType
              .getEnumOfTypesFromString(queryParams.get(QueryParams.SUBTYPES).get(0));
    }

    switch (type) {
      case PREIMAGE_SHA256:
        return PreimageSha256Condition.fromCostAndFingerprint(cost, fingerprint);
      case PREFIX_SHA256:
        return PrefixSha256Condition.fromCostAndFingerprint(cost, fingerprint, subtypes);
      case THRESHOLD_SHA256:
        return ThresholdSha256Condition.fromCostAndFingerprint(cost, fingerprint, subtypes);
      case RSA_SHA256:
        return RsaSha256Condition.fromCostAndFingerprint(cost, fingerprint);
      case ED25519_SHA256:
        return Ed25519Sha256Condition.fromCostAndFingerprint(fingerprint);
      default:
        throw new URISyntaxException(uri.toString(), "No or invalid type provided");
    }
  }

  /**
   * Convert a crypto condition to its ni-schemed URI representation.
   *
   * @param condition A {@link Condition} to convert.
   *
   * @return A {@link URI} representing the ni-schemed version of the supplied {@code condition}.
   */
  public static URI toUri(final Condition condition) {
    if (condition instanceof SimpleCondition) {
      return writeSingleCondition((SimpleCondition) condition);
    } else if (condition instanceof CompoundCondition) {
      return writeCompoundCondition((CompoundCondition) condition);
    } else {
      throw new IllegalArgumentException(
          String.format("Unhandled Condition type: %s", condition.getClass().getName())
      );
    }
  }

  private static URI writeSingleCondition(final SimpleCondition condition) {
    Objects.requireNonNull(condition);
    final Map<String, String> params = new HashMap<>();
    params.put(CryptoConditionUri.QueryParams.TYPE, condition.getType().toString().toLowerCase());
    params.put(CryptoConditionUri.QueryParams.COST, Long.toString(condition.getCost()));

    return NamedInformationUri.getUri(HashFunction.SHA_256, condition.getFingerprint(), params);
  }

  private static URI writeCompoundCondition(final CompoundCondition condition) {
    Objects.requireNonNull(condition);

    Map<String, String> params = new HashMap<>();
    params.put(CryptoConditionUri.QueryParams.TYPE, condition.getType().toString().toLowerCase());
    params.put(CryptoConditionUri.QueryParams.COST, Long.toString(condition.getCost()));

    if (condition.getSubtypes() != null && !condition.getSubtypes().isEmpty()) {
      params.put(CryptoConditionUri.QueryParams.SUBTYPES,
          CryptoConditionType.getEnumOfTypesAsString(condition.getSubtypes()));
    }

    return NamedInformationUri.getUri(HashFunction.SHA_256, condition.getFingerprint(), params);
  }

  /**
   * Unpacks an URL encoded string of query parameters into a map of keys and values.
   *
   * @param queryParams The url-encoded query parameters.
   *
   * @return A map containing keyed on the query parameter names containing the associated values.
   */
  private static Map<String, List<String>> splitQuery(String queryParams)
      throws UnsupportedEncodingException {
    // Used to avoid dependencies on additional libraries. Lightly adapted from
    // http://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
    final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
    final String[] pairs = queryParams.split("&");
    for (String pair : pairs) {
      final int idx = pair.indexOf("=");
      final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
      if (!query_pairs.containsKey(key)) {
        query_pairs.put(key, new LinkedList<String>());
      }
      final String value = idx > 0 && pair.length() > idx + 1
          ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
      query_pairs.get(key).add(value);
    }
    return query_pairs;
  }

  public static class QueryParams {

    public static final String COST = "cost";
    public static final String TYPE = "fpt";
    public static final String SUBTYPES = "subtypes";
  }
}

