package org.interledger.btp;

/*-
 * ========================LICENSE_START=================================
 * Bilateral Transfer Protocol Core Libs
 * %%
 * Copyright (C) 2017 - 2019 Hyperledger and its contributors
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

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Represents a BTP session.</p>
 *
 * <p>This class cd is required because the BTP Auth protocol does not rely on standard HTTP Basic-Auth parameters in
 * a typical websocket URL, so the normal process of doing Auth during a websocket handshake is not workable, forcing
 * BTP to track its own sessions (at least when operating over a Websocket transport).
 */
public class BtpSession {

  private static final boolean UNSUCCESSFUL_ASSIGNMENT = false;

  // A given BTP session has only a single counter-party using a single Websocket Session.
  private final String websocketSessionId;

  // Once btpSessionCredentials are set into this object, they cannot be unset.
  private final AtomicReference<Optional<BtpSessionCredentials>> btpSessionCredentials;

  // Once a BtpSession has become authenticated, it cannot be reversed. It's possible that a session will expire, in
  // which case this class MUST be destroyed and a new session created.
  private final AtomicBoolean authenticated;

  /**
   * No-Args Constructor.
   *
   * @param websocketSessionId The unique identifier of the websocket session this BtpSession is operating inside of.
   *                           Note that this is a {@link String} in order to support more than just UUIDs, which is
   *                           common is Java Websocket libraries, where string is used to identify a Websocket
   *                           session.
   */
  public BtpSession(final String websocketSessionId) {
    this.websocketSessionId = Objects.requireNonNull(websocketSessionId);
    this.btpSessionCredentials = new AtomicReference<>(Optional.empty());
    this.authenticated = new AtomicBoolean(); // initially is `false`
  }

  /**
   * Required-Args Constructor.
   *
   * @param websocketSessionId    The unique identifier of the websocket session this BtpSession is operating inside of.
   *                              Note that this is a {@link String} in order to support more than just UUIDs, which is
   *                              common is Java Websocket libraries, where string is used to identify a Websocket
   *                              session.
   * @param btpSessionCredentials A {@link BtpSessionCredentials} containing all necessary information required to
   *                              authenticate this BTP session with a remote peer.
   */
  public BtpSession(
      final String websocketSessionId, final BtpSessionCredentials btpSessionCredentials
  ) {
    this.websocketSessionId = Objects.requireNonNull(websocketSessionId);
    this.btpSessionCredentials = new AtomicReference<>(Optional.of(btpSessionCredentials));
    this.authenticated = new AtomicBoolean(); // initially is `false`
  }

  /**
   * Accessor for this session's credentials, if present. No need to return the AtomicReference because that is only
   * used as a setter-guard.
   *
   * @return An optionally present instance of {@link BtpSessionCredentials}.
   */
  public Optional<BtpSessionCredentials> getBtpSessionCredentials() {
    return btpSessionCredentials.get();
  }

  /**
   * Sets a {@link BtpSessionCredentials} into this session that will ultimately be used to perform authentication with
   * a remote peer/server using the BTP Auth protocol.
   *
   * @param btpSessionCredentials A {@link BtpSessionCredentials} containing all credentials for this session.
   */
  public void setBtpSessionCredentials(final BtpSessionCredentials btpSessionCredentials) {
    Objects.requireNonNull(btpSessionCredentials);

    // Either this session has never been authenticated, or it has, and we only want to honor the same authentication
    // (i.e., a repeat of the same credentials).
    if (this.btpSessionCredentials
        .compareAndSet(Optional.empty(), Optional.of(btpSessionCredentials)) == UNSUCCESSFUL_ASSIGNMENT
    ) {
      // If this happens, it indicates a bug.
      new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, "BtpSessionCredentials may only be set once!");
    }
  }

  /**
   * A BTP Session is considered to be authenticated if it has a non-empty instance of BtpCredentials. This can only
   * occur if the BTP Auth sub-protocol is followed properly.
   *
   * @return {@code true} if this BtpSession is properly authenticated; {@code false} otherwise.
   */
  public boolean isAuthenticated() {
    return this.authenticated.get();
  }

  /**
   * Update this session to indicate that it is authenticated with a remote peer.
   */
  public void setAuthenticated() {
    if (this.authenticated.compareAndSet(false, true) == UNSUCCESSFUL_ASSIGNMENT) {
      // If this happens, it indicates a bug.
      throw new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, "BtpSession may only be authenticated once!");
    }
  }

  /**
   * Accessor for the unique identifier of this session.
   */
  public String getWebsocketSessionId() {
    return this.websocketSessionId;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    BtpSession that = (BtpSession) object;

    if (!websocketSessionId.equals(that.websocketSessionId)) {
      return false;
    }
    if (!btpSessionCredentials.equals(that.btpSessionCredentials)) {
      return false;
    }
    return authenticated.equals(that.authenticated);
  }

  @Override
  public int hashCode() {
    int result = websocketSessionId.hashCode();
    result = 31 * result + btpSessionCredentials.hashCode();
    result = 31 * result + authenticated.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", BtpSession.class.getSimpleName() + "[", "]")
        .add("websocketSessionId='" + websocketSessionId + "'")
        .add("btpSessionCredentials=" + btpSessionCredentials)
        .add("authenticated=" + authenticated)
        .toString();
  }
}
