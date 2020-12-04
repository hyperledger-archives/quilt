package org.interledger.link.http;

import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.PRAGMA;
import static com.google.common.net.MediaType.OCTET_STREAM;
import static org.interledger.link.http.IlpOverHttpConstants.APPLICATION_OCTET_STREAM;
import static org.interledger.link.http.IlpOverHttpConstants.BEARER;
import static org.interledger.link.http.IlpOverHttpConstants.ILP_OPERATOR_ADDRESS_VALUE;
import static org.interledger.link.http.IlpOverHttpConstants.OCTET_STREAM_STRING;

import org.interledger.core.DateUtils;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerConstants;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.AbstractLink;
import org.interledger.link.Link;
import org.interledger.link.LinkHandler;
import org.interledger.link.LinkType;
import org.interledger.link.exceptions.LinkException;
import org.interledger.link.http.auth.BearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * <p>An extension of {@link AbstractLink} that handles HTTP (aka, ILP over HTTP) connections, both incoming and
 * outgoing.</p>
 *
 * <p>To handle incoming HTTP requests, use {@link #registerLinkHandler(LinkHandler)}.</p>
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
 */
public class IlpOverHttpLink extends AbstractLink<IlpOverHttpLinkSettings> implements Link<IlpOverHttpLinkSettings> {

  private static final String BEARER_WITH_SPACE = BEARER + " ";
  public static final String LINK_TYPE_STRING = "ILP_OVER_HTTP";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  private static final InterledgerPreparePacket UNFULFILLABLE_PACKET = InterledgerPreparePacket.builder()
      .executionCondition(InterledgerConstants.ALL_ZEROS_CONDITION)
      .expiresAt(DateUtils.now().plusSeconds(30))
      .destination(InterledgerAddress.of("peer.ilp_over_http_connection_test_that_should_always_reject"))
      .build();

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  // Note: The Http client in this sender is shared between all HTTP links...
  private final OkHttpClient okHttpClient;
  private final ObjectMapper objectMapper;
  private final CodecContext ilpCodecContext;

  /**
   * These are fine to be typed as String for two reasons: 1) They will be put into headers as a String (so they'll hang
   * around in memory) and 2) they have when they're JWTs, they have short expiries (the signing key is not persisted as
   * a String).
   */
  private final Supplier<String> authTokenSupplier;

  private final HttpUrl outgoingUrl;


  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A supplier for the ILP address of this node operating this Link. This value may be
   *                                uninitialized, for example, in cases where the Link obtains its address from a
   *                                parent node using IL-DCP. If an ILP address has not been assigned, or it has not
   *                                been obtained via IL-DCP, then this value will by default be {@link Link#SELF}.
   * @param outgoingUrl             A {@link HttpUrl} to the connector.
   * @param okHttpClient            A {@link OkHttpClient} to use to communicate with the remote ILP-over-HTTP
   *                                endpoint.
   * @param objectMapper            A {@link ObjectMapper} for reading error responses from the remote ILP-over-HTTP
   *                                endpoint.
   * @param ilpCodecContext         A {@link CodecContext} for ILP.
   * @param bearerTokenSupplier     A {@link BearerTokenSupplier} that can be used to get a bearer token to make
   *                                authenticated calls to the remote HTTP endpoint.
   */
  public IlpOverHttpLink(
      final Supplier<InterledgerAddress> operatorAddressSupplier,
      final HttpUrl outgoingUrl,
      final OkHttpClient okHttpClient,
      final ObjectMapper objectMapper,
      final CodecContext ilpCodecContext,
      final BearerTokenSupplier bearerTokenSupplier
  ) {
    // IlpOverHttpLink does not require a full link settings, but the parent class requires one (even though it also
    // does not use it for anything). For backwards compatibility pass a bare bones setting but ideally the abstract
    // class could be refactored to not require it.
    super(operatorAddressSupplier, IlpOverHttpLinkSettings.builder().build());
    this.outgoingUrl = outgoingUrl;
    this.okHttpClient = Objects.requireNonNull(okHttpClient);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
    this.authTokenSupplier = Objects.requireNonNull(bearerTokenSupplier);
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    if (preparePacket.getExpiresAt() != null &&
        okHttpClient.readTimeoutMillis() <= Duration.between(Instant.now(), preparePacket.getExpiresAt()).toMillis()) {
      logger.warn("OkHttpClient read timeout is shorter than the Prepare Packet's timeout.  " +
          "This may result in an HTTP timeout while unexpired ILP packets are in flight.");
    }

    final Request okHttpRequest;
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ilpCodecContext.write(preparePacket, byteArrayOutputStream);
      okHttpRequest = this.constructSendPacketRequest(preparePacket);
    } catch (IOException e) {
      throw new LinkException(e.getMessage(), e, getLinkId());
    }

    try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
      if (response.isSuccessful()) {
        //////////
        // Success
        return ilpCodecContext.read(InterledgerResponsePacket.class, response.body().byteStream());
      } else {
        //////////
        // Reject!
        final String errorResponseBody = CharStreams.toString(response.body().charStream());
        Optional<ThrowableProblem> problem = parseThrowableProblem(preparePacket, errorResponseBody);
        final InterledgerRejectPacket rejectPacket;

        if (response.code() >= 400 && response.code() < 500) {
          String customErrorMessage = null;
          if (response.code() == 401 || response.code() == 403) {
            // If this code is returned, we know the Link is misconfigured
            customErrorMessage = String.format("Unable to connect to remote ILP-over-HTTP Link: Invalid Bearer " +
                "Token. response=%s", response);
            logger.error(customErrorMessage);
          }

          String message = Stream.of(
              customErrorMessage,
              problem.map(Problem::getTitle).orElse(null),
              errorResponseBody
          )
              .filter(Objects::nonNull)
              .findFirst()
              .get();
          // The request was bad for some reason, likely due to whatever is in the packet.
          rejectPacket = InterledgerRejectPacket.builder()
              .triggeredBy(getOperatorAddressSupplier().get())
              .code(InterledgerErrorCode.F00_BAD_REQUEST)
              .message(message)
              .build();
        } else {
          // Something else went wrong on the server...try again later.
          rejectPacket = InterledgerRejectPacket.builder()
              .triggeredBy(getOperatorAddressSupplier().get())
              .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
              .message(problem.map(Problem::getTitle).orElse(errorResponseBody))
              .build();
        }

        logger.error(
            "Unable to send ILP-over-HTTP packet. preparePacket={} httpResponseCode={} errorResponse={} "
                + "rejectPacket={}",
            preparePacket, response.code(), CharStreams.toString(response.body().charStream()), rejectPacket
        );
        return rejectPacket;
      }
    } catch (IOException e) {
      throw new LinkException(
          String.format("Unable to sendPacket. preparePacket=%s error=%s", preparePacket, e.getMessage()),
          e,
          getLinkId()
      );
    }
  }

  /**
   * <p>Check the `/ilp` endpoint for ping by making an HTTP Head request with a ping packet, and
   * asserting the values returned are one of the supported content-types required for ILP-over-HTTP.</p>
   *
   * <p>If the endpoint does not support producing ILP-over-HTTP responses, we expect a 406 NOT_ACCEPTABLE response. If
   * the endpoint does not support ILP-over-HTTP requests, then we expect a 415 UNSUPPORTED_MEDIA_TYPE.</p>
   */
  public void testConnection() {
    try {
      final Request okHttpRequest = this.constructSendPacketRequest(UNFULFILLABLE_PACKET);

      try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
        if (response.isSuccessful()) {
          //////////
          // Success
          // If there's one Accept header we can work with, then treat this as a successful test connection...
          boolean responseHasOctetStreamContentType = response.headers(CONTENT_TYPE).stream()
              .findFirst()
              .filter(OCTET_STREAM_STRING::equals)
              .isPresent();

          if (responseHasOctetStreamContentType) {
            logger.info("Remote peer-link supports ILP-over-HTTP. url={} responseHeaders={}",
                outgoingUrl, response
            );
          } else {
            logger.warn("Remote peer-link supports ILP-over-HTTP but uses wrong ContentType. url={} "
                    + "response={}",
                outgoingUrl, response
            );
          }
        } else {
          if (response.code() == 406 || response.code() == 415) { // NOT_ACCEPTABLE || UNSUPPORTED_MEDIA_TYPE
            throw new LinkException(
                String.format("Remote peer-link DOES NOT support ILP-over-HTTP. url=%s response=%s",
                    outgoingUrl, response
                ), getLinkId()
            );
          } else {
            throw new LinkException(
                String.format("Unable to connect to ILP-over-HTTP. url=%s response=%s",
                    outgoingUrl, response
                ), getLinkId()
            );
          }
        }
      }
    } catch (IOException e) {
      throw new LinkException(e.getMessage(), e, getLinkId());
    }
  }

  /**
   * Construct headers for an ILP-over-HTTP request.
   *
   * @return A newly constructed instance of {@link Headers}.
   */
  private Headers constructHttpRequestHeaders() {
    final Headers.Builder headers = new Headers.Builder()
        // Defaults to ILP_OCTET_STREAM, but is replaced by whatever testConnection returns if it's a valid media-type.
        .add(HttpHeaders.ACCEPT, OCTET_STREAM.toString())
        .add(CONTENT_TYPE, OCTET_STREAM.toString())
        // Disable HTTP Caching of packets...
        .add(CACHE_CONTROL, "private, max-age=0, no-cache")
        .add(PRAGMA, "no-cache");

    // Set the Operator Address header, if present.
    headers.set(ILP_OPERATOR_ADDRESS_VALUE, getOperatorAddressSupplier().get().getValue());

    headers.add(HttpHeaders.AUTHORIZATION, BEARER_WITH_SPACE + this.authTokenSupplier.get());

    return headers.build();
  }

  /**
   * Helper method that attempts to parse a JSON error response into a {@link ThrowableProblem}.
   *
   * @param preparePacket     An {@link InterledgerPreparePacket}, for logging purposes.
   * @param errorResponseBody An error {@link String} from the counter party ILP-over-HTTP endpoint.
   *
   * @return An optionally-present {@link ThrowableProblem} if one can be parsed from the {@link Response}.
   */
  private Optional<ThrowableProblem> parseThrowableProblem(
      final InterledgerPreparePacket preparePacket, final String errorResponseBody
  ) {
    Objects.requireNonNull(preparePacket);
    Objects.requireNonNull(errorResponseBody);

    try {
      return Optional.ofNullable(objectMapper.readValue(errorResponseBody, ThrowableProblem.class));
    } catch (IOException e) {
      // Something went wrong unmarshalling the error response. Return both the exception and the actual remote
      // error.
      logger.warn(
          "Unable to parse Ilp-over-Http response. preparePacket={} errorResponseBody={} exception={}",
          preparePacket, errorResponseBody, e
      );
      return Optional.empty();
    }
  }

  /**
   * Helper method to construct a {@link Request} containing the supplied {@code preparePacket}.
   *
   * @param preparePacket A {@link InterledgerPreparePacket} to send to the remote HTTP endpoint.
   *
   * @return A {@link Request} that can be used with an OkHttp client.
   */
  private Request constructSendPacketRequest(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ilpCodecContext.write(preparePacket, byteArrayOutputStream);

      return new Builder()
          .headers(constructHttpRequestHeaders())
          .url(outgoingUrl)
          .post(
              RequestBody.create(byteArrayOutputStream.toByteArray(), APPLICATION_OCTET_STREAM)
          )
          .build();

    } catch (Exception e) {
      throw new LinkException(e.getMessage(), e, getLinkId());
    }
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", IlpOverHttpLink.class.getSimpleName() + "[", "]")
        .add("linkId=" + getLinkId())
        .add("operatorAddressSupplier=" + getOperatorAddressSupplier().get())
        .add("outgoingUrl=" + outgoingUrl)
        .add("linkSettings=" + getLinkSettings())
        .toString();
  }

  /**
   * Accessor for the "outgoing" URL that this link uses to make outgoing HTTP requests to its peer.
   *
   * @return A {@link okhttp3.HttpUrl}.
   */
  public HttpUrl getOutgoingUrl() {
    return this.outgoingUrl;
  }
}
