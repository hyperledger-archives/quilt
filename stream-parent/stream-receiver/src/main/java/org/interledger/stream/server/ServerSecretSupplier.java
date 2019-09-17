package org.interledger.stream.server;

import java.util.function.Supplier;

/**
 * Supplies the Server secret, which is currently used as a master seed that can be used to derive sub-secrets and
 * actual HMAC'd values.
 */
public interface ServerSecretSupplier extends Supplier<byte[]> {

}
