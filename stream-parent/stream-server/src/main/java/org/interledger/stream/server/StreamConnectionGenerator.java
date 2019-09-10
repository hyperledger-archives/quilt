package org.interledger.stream.server;

/**
 * <p>A STREAM connection generator that creates `destination_account` and `shared_secret` values based on a single
 * root secret.</p>
 *
 * <p>This service can be reused across multiple STREAM connections so that a single receiver can accept incoming
 * packets for multiple connections.</p>
 */
public interface StreamConnectionGenerator {



}
