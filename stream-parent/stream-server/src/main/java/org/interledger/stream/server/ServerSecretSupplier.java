package org.interledger.stream.server;

import java.util.function.Supplier;

/**
 * Supplies the Server secret.
 */
public interface ServerSecretSupplier extends Supplier<byte[]> {

}
