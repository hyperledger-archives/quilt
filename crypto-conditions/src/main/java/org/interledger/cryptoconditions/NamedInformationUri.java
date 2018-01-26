package org.interledger.cryptoconditions;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Provides utility methods related to named information (ni://) URI's.
 */
public class NamedInformationUri {

  public static final String SCHEME = "ni";
  public static final String SCHEME_PREFIX = SCHEME + "://";

  /**
   * Creates a URI for the hash function and values.
   *
   * @param hashFunction      The hash function used.
   * @param hash              The value from the hash.
   * @param queryStringParams Additional values to include in the query string portion from the URI
   *
   * @return A URI containing the hash function, the hashed value and any additional query
   *     parameters.
   */
  public static URI getUri(HashFunction hashFunction, byte[] hash,
      Map<String, String> queryStringParams) {

    Objects.requireNonNull(hashFunction);
    Objects.requireNonNull(hash);
    Objects.requireNonNull(queryStringParams);

    StringBuilder sb = new StringBuilder();
    sb.append(SCHEME_PREFIX) // Scheme prefix
        // No authority portion
        .append("/") // Path prefix
        .append(hashFunction.getName()).append(";") // Hash function name
        .append(Base64.getUrlEncoder().withoutPadding().encodeToString(hash)); // Hash

    if (!queryStringParams.isEmpty()) {
      sb.append("?");

      queryStringParams.forEach((key, value) -> {
        if (value != null && !"".equals(value)) {
          // Some params might contain multiple values, separate by a comma.  If so, we need to
          // break them up (by comma), encode each value, and then re-combine each encoded value
          // separated by a comma.
          final String encodedValue = Optional.ofNullable(value)
              .filter(val -> val.contains(","))
              // If there's a comma, then split the string into components by that comma...
              .map(valueWithComma -> Arrays.asList(value.split(",")).stream())
              // Encode each portion, and re-assemble with commas intact.
              .map(stringStream -> stringStream
                  .filter(v -> v != "")
                  .map(NamedInformationUri::encode)
                  .distinct()
                  .collect(Collectors.joining(",")))
              // If there's no comma in the value, then just encode and return.
              .orElseGet(() -> encode(value));

          sb.append(key).append("=").append(encodedValue);
          sb.append("&");
        }
      });

      // Delete the trailing "&" or "?"
      sb.deleteCharAt(sb.length() - 1);
    }

    return URI.create(sb.toString());
  }

  private static String encode(final String value) {
    try {
      return URLEncoder.encode(value, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a regex listing the names of the HashFunctions.
   */
  private static String getHashFunctionRegexGroup() {
    return "("
        + String.join("|",
        Arrays.stream(HashFunction.values()).map(s -> s.getName()).toArray(String[]::new))
        + ")";
  }

  /**
   * From https://www.iana.org/assignments/hash-function-text-names/hash-function-text-names.xhtml
   *
   * @author adrianhopebailie
   */
  public enum HashFunction {
    MD2("md2", "1.2.840.113549.2.2"),
    MD5("md5", "1.2.840.113549.2.5"),
    SHA_1("sha-1", "1.3.14.3.2.26"),
    SHA_224("sha-224", "2.16.840.1.101.3.4.2.4"),
    SHA_256("sha-256", "2.16.840.1.101.3.4.2.1"),
    SHA_384("sha-384", "2.16.840.1.101.3.4.2.2"),
    SHA_512("sha-512", "2.16.840.1.101.3.4.2.3");

    private String name;
    private String oid;

    HashFunction(String name, String oid) {
      this.name = name;
      this.oid = oid;
    }

    /**
     * <p>Returns the name of the hash function.</p>
     *
     * @return A {@link String} containing the <tt>name</tt> of this hash function.
     */
    public String getName() {
      return name;
    }

    /**
     * <p>Returns the OID for the hash function.</p>
     *
     * @return A {@link String} containing the <tt>oid</tt> of this hash function.
     */
    public String getOid() {
      return oid;
    }

  }

}
