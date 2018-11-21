package org.interledger.btp;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Represents a BTP session.</p>
 *
 * <p>This class cd is required because the BTP Auth protocol does not rely on standard HTTP Basic-Auth parameters in
 * a typical websocket URL, so the normal process of doing Auth during a websocket handshake is not workable, forcing
 * BTP to track its own sessions (at least when operating over a Websocket transport).
 */
public class BtpSession {

  // A given BTP session has only a single counterparty and Websocket Session.
  private final String websocketSessionId;
  private final AtomicReference<Optional<BtpSessionCredentials>> btpSessionCredentials;

  /**
   * No-Args Constructor.
   *
   * @param websocketSessionId The unique identifier of the websocket session this BtpSession is operating inside of.
   */
  public BtpSession(final String websocketSessionId) {
    this.websocketSessionId = Objects.requireNonNull(websocketSessionId);
    this.btpSessionCredentials = new AtomicReference<>(Optional.empty());
  }

  /**
   * Sets a valid set of {@link BtpSessionCredentials} into this session. Note that callers should not attempt this
   * method before properly validating the credentials being set.
   *
   * @param btpSessionCredentials A {@link BtpSessionCredentials} containing all credentials for this session.
   */
  public void setValidAuthentication(final BtpSessionCredentials btpSessionCredentials) {
    Objects.requireNonNull(btpSessionCredentials);

    // Either this session has never been authenticated, or it has, and we only want to honor the same authentication
    // (i.e., a repeat of the same credentials).
    if (this.btpSessionCredentials.get() != null) {
      final boolean success = this.btpSessionCredentials
          .compareAndSet(Optional.empty(), Optional.of(btpSessionCredentials));
      if (!success) {
        new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, String.format("BTP Session already authenticated!"));
      }
    } else {
      // The BTP Session is already authenticated, so only allow authenticating with the same credentials.
      final boolean success = this.btpSessionCredentials
          .compareAndSet(Optional.of(btpSessionCredentials), Optional.of(btpSessionCredentials));
      if (!success) {
        new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, String.format("BTP Session already authenticated!"));
      }
    }
  }

  /**
   * A BTP Session is considered to be authenticated if it has a non-empty instance of BtpCredentials. This can only
   * occur if the BTP Auth sub-protocol is followed properly.
   *
   * @return {@code true} if this BtpSession is properly authenticated; {@code false} otherwise.
   */
  public boolean isAuthenticated() {
    return this.btpSessionCredentials.get().isPresent();
  }

  public AtomicReference<Optional<BtpSessionCredentials>> getBtpSessionCredentials() {
    return btpSessionCredentials;
  }

  public String getWebsocketSessionId() {
    return this.websocketSessionId;
  }
}
