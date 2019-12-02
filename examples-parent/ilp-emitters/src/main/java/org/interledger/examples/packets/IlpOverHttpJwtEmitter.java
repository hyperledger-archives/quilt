package org.interledger.examples.packets;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.interfaces.Verification;
import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * <p>Helper class to assemble a JWT (using HS256 and a 256-bit shared secret) that can be used by ILP-over-HTTP
 * endpoints. In order to work properly, the expiration of this token needs to be long-lived so we set it to be 1 year.
 * In general, we want to discourage the use of this mechanism (JWTs should instead be short-lived), so we don't let the
 * token live forever.</p>
 *
 * <p>For this type of JWT, the only type of claim required is the  </p>
 */
public class IlpOverHttpJwtEmitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(IlpOverHttpJwtEmitter.class);
  private static final String SHARED_SECRET_B64 = "HEiMCp0FoAC903QHueY89gAWJHo/izaBnJU8/58rlSI=";

  private static final Algorithm ALGORITHM_HS256 =
    Algorithm.HMAC256(BaseEncoding.base64().decode(SHARED_SECRET_B64));
  private static final String SUBJECT = "alice-xrp";

  public static void main(String[] args) {
    emitHs256Jwt();
    emitHs256JwtWithExpiry();
  }

  /**
   * Emit a token that claims only a subject (`alice`). Because this is a SIMPLE token, it essentially needs to do only
   * two things: identify the account that the token is good for, and prove that whoever generated the token has the
   * shared-secret. Note that while simple, this does not provide very good security since a compromised token can be
   * reused forever, potentially without being easy to detect.
   */
  private static void emitHs256Jwt() {
    final String jwtString = JWT.create()
      .withSubject(SUBJECT)
      .sign(ALGORITHM_HS256);

    LOGGER.info("JWT: {}", jwtString);
    LOGGER.info("JWT Length (bytes): {}", jwtString.length());

    // Log the JWT claims...
    JWT.decode(jwtString).getClaims().forEach((key, value) ->
      LOGGER.info("Claim -> \"{}\":\"{}\"", key, value.asString()
      ));

    // Valid token...
    final Verification verification = JWT.require(ALGORITHM_HS256).withSubject(SUBJECT);

    // Valid token...
    verification.build().verify(jwtString);

    // Invalid token...
    try {
      verification.withSubject("bob").build().verify(jwtString);
      throw new RuntimeException("Verify should have failed");
    } catch (InvalidClaimException e) {
      LOGGER.info("Invalid JWT for `bob` did not verify, as expected.");
    }
  }

  /**
   * Emit a JWT that has enhanced security.
   */
  private static void emitHs256JwtWithExpiry() {

    final String jwtString = JWT.create()
      .withSubject(SUBJECT)
      .withExpiresAt(Date.from(Instant.now().plus(730, ChronoUnit.DAYS)))
      .sign(ALGORITHM_HS256);

    LOGGER.info("JWT: {}", jwtString);
    LOGGER.info("JWT Length (bytes): {}", jwtString.length());

    // Log the JWT claims...
    JWT.decode(jwtString).getClaims().forEach((key, value) ->
      LOGGER.info("Claim -> \"{}\":\"{}\"", key, value.asString()
      ));

    // Valid token...
    final Verification verification = JWT.require(ALGORITHM_HS256).withSubject(SUBJECT);

    // Valid token...
    verification.build().verify(jwtString);

    // Invalid token...
    try {
      verification.withSubject("bob").build().verify(jwtString);
      throw new RuntimeException("Verify should have failed");
    } catch (InvalidClaimException e) {
      LOGGER.info("Invalid JWT for `bob` did not verify, as expected.");
    }
  }

}
