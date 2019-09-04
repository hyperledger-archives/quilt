package org.interledger.stream;

public class StreamServer {

  /**
   * <p>Setup a stream with another participant.</p>
   *
   * <p>A server MUST communicate the following values to a client using an authenticated, encrypted communication
   * channel (such as HTTPS). Key exchange is NOT provided by STREAM.</p>
   *
   * <p>
   * <ol>
   *  <li>STREAM Version: (optional -- assumed to be version 1 unless specified)</li>
   *  <li>Server ILP Address</li>
   *  <li>Cryptographically secure random or pseudorandom shared secret (it is RECOMMENDED to use 32 bytes). To avoid
   * storing a 32 byte secret for each connection, a server MAY deterministically generate the shared secret for each
   * connection from a single server secret and a nonce appended to the ILP Address given to a particular client, for
   * example by using an HMAC.</li>
   * </ol>
   * </p>
   */
//  public void setupStream() {
//
//  }

}
