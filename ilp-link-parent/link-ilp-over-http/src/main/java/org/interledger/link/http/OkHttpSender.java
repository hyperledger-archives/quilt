package org.interledger.link.http;

import static com.google.common.net.HttpHeaders.CACHE_CONTROL;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.PRAGMA;
import static com.google.common.net.MediaType.OCTET_STREAM;
import static org.interledger.link.http.IlpHttpHeaders.ILP_OPERATOR_ADDRESS_VALUE;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.link.http.auth.BearerTokenSupplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import com.google.common.net.HttpHeaders;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An implementation of {@link HttpSender} for communicating with a remote ILP-over-HTTP endpoint using the OkHttp3
 * library.
 */
public class OkHttpSender implements HttpSender {

  private static final String BEARER = "Bearer ";
  private static final String OCTET_STREAM_STRING = OCTET_STREAM.toString();
  private static final MediaType APPLICATION_OCTET_STREAM = MediaType.parse(OCTET_STREAM_STRING);

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * The optionally-present {@link InterledgerAddress} of the operator of this sender. Sometimes the Operator Address is
   * not yet populated when this client is constructed (e.g, IL-DCP). Thus, the value is optional to accommodate these
   * cases.
   */
  private final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier;
  private final OkHttpClient okHttpClient;
  private final ObjectMapper objectMapper;
  private final CodecContext ilpCodecContext;
  private final OutgoingLinkSettings outgoingLinkSettings;

  // These are fine to be typed as String for two reasons: 1) They will be put into headers as a String (so they'll
  // hang around in memory) and 2) they have when they're JWTs, they have short expiries (the signing key is not
  // persisted as a String).
  private final Supplier<String> authTokenSupplier;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} for the ILP address of the node operating this ILP-over-HTTP
   *                                sender.
   * @param okHttpClient            A {@link OkHttpClient} to use to communicate with the remote ILP-over-HTTP
   *                                endpoint.
   * @param objectMapper            A {@link ObjectMapper} for reading error responses from the remote ILP-over-HTTP
   *                                endpoint.
   * @param outgoingLinkSettings    A {@link OutgoingLinkSettings} for communicating with the remote endpoint.
   * @param bearerTokenSupplier     A {@link BearerTokenSupplier} that can be used to get a bearer token to make
   *                                authenticated calls to the remote HTTP endpoint.
   */
  public OkHttpSender(
      final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
      final OkHttpClient okHttpClient,
      final ObjectMapper objectMapper,
      final CodecContext ilpCodecContext,
      final OutgoingLinkSettings outgoingLinkSettings,
      final BearerTokenSupplier bearerTokenSupplier
  ) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
    this.okHttpClient = Objects.requireNonNull(okHttpClient);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.ilpCodecContext = Objects.requireNonNull(ilpCodecContext);
    this.outgoingLinkSettings = Objects.requireNonNull(outgoingLinkSettings);
    this.authTokenSupplier = Objects.requireNonNull(bearerTokenSupplier);
  }

  /**
   * Send an ILP prepare packet to the remote peer.
   *
   * @param preparePacket An {@link InterledgerPreparePacket} to send to the remote ILP-over-HTTP endpoint.
   *
   * @return An optionally-present {@link InterledgerResponsePacket}. If the request to the remote peer times-out, then
   *     the ILP reject packet will contain a {@link InterledgerRejectPacket#getTriggeredBy()} address that   that
   *     matches this node's operator address.
   */
  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    final Request okHttpRequest;
    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ilpCodecContext.write(preparePacket, byteArrayOutputStream);
      okHttpRequest = this.constructSendPacketRequest(UNFULFILLABLE_PACKET);
    } catch (IOException e) {
      throw new IlpOverHttpSenderException(e.getMessage(), e);
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
          // The request was bad for some reason, likely due to whatever is in the packet.
          rejectPacket = InterledgerRejectPacket.builder()
              .triggeredBy(operatorAddressSupplier().get())
              .code(InterledgerErrorCode.F00_BAD_REQUEST)
              .message(problem.map(Problem::getTitle).orElse(errorResponseBody))
              .build();
        } else {
          // Something else went wrong on the server...try again later.
          rejectPacket = InterledgerRejectPacket.builder()
              .triggeredBy(operatorAddressSupplier().get())
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
      throw new IlpOverHttpSenderException(e.getMessage(), e);
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
    final String tokenSubject = outgoingLinkSettings.tokenSubject();
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
            logger.info("Remote peer-link supports ILP-over-HTTP. tokenSubject={} url={} responseHeaders={}",
                outgoingLinkSettings.tokenSubject(), outgoingLinkSettings.url(), response
            );
          } else {
            logger.warn("Remote peer-link supports ILP-over-HTTP but uses wrong ContentType. tokenSubject={} url={} "
                    + "response={}",
                outgoingLinkSettings.tokenSubject(), outgoingLinkSettings.url(), response
            );
          }
        } else {
          if (response.code() == 406 || response.code() == 415) { // NOT_ACCEPTABLE || UNSUPPORTED_MEDIA_TYPE
            throw new IlpOverHttpSenderException(
                String.format("Remote peer-link DOES NOT support ILP-over-HTTP. tokenSubject=%s url=%s response=%s",
                    tokenSubject, outgoingLinkSettings.url(), response
                )
            );
          } else {
            throw new IlpOverHttpSenderException(
                String.format("Unable to connect to ILP-over-HTTP. tokenSubject=%s url=%s response=%s",
                    tokenSubject, outgoingLinkSettings.url(), response
                )
            );
          }
        }
      }
    } catch (IOException e) {
      throw new IlpOverHttpSenderException(e.getMessage(), e);
    }
  }

  /**
   * The optionally-present ILP Address of operator running this sender.
   *
   * @return A {@link Supplier} that produces an optionally present ILP address.
   */
  public Supplier<Optional<InterledgerAddress>> operatorAddressSupplier() {
    return operatorAddressSupplier;
  }

  /**
   * The {@link OutgoingLinkSettings} held by this sender.
   *
   * @return The {@link OutgoingLinkSettings} for this sender.
   */
  public OutgoingLinkSettings outgoingLinkSettings() {
    return outgoingLinkSettings;
  }

  /**
   * Construct headers for an ILP-over-HTTP request.
   *
   * @return A newly constructed instance of {@link Headers}.
   */
  @NotNull
  private Headers constructHttpRequestHeaders() {
    final Headers.Builder headers = new Headers.Builder()
        // Defaults to ILP_OCTET_STREAM, but is replaced by whatever testConnection returns if it's a valid media-type.
        .add(HttpHeaders.ACCEPT, OCTET_STREAM.toString())
        .add(CONTENT_TYPE, OCTET_STREAM.toString())
        // Disable HTTP Caching of packets...
        .add(CACHE_CONTROL, "private, max-age=0, no-cache")
        .add(PRAGMA, "no-cache");

    // Set the Operator Address header, if present.
    operatorAddressSupplier.get()
        .ifPresent(operatorAddress -> headers.set(ILP_OPERATOR_ADDRESS_VALUE, operatorAddress.getValue()));

    headers.add(HttpHeaders.AUTHORIZATION, BEARER + this.authTokenSupplier.get());

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
  @NotNull
  private Request constructSendPacketRequest(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    try {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ilpCodecContext.write(preparePacket, byteArrayOutputStream);

      return new Builder()
          .headers(constructHttpRequestHeaders())
          .url(outgoingLinkSettings.url())
          .post(
              RequestBody.create(byteArrayOutputStream.toByteArray(), APPLICATION_OCTET_STREAM)
          )
          .build();

    } catch (Exception e) {
      throw new IlpOverHttpSenderException(e.getMessage(), e);
    }
  }
}
