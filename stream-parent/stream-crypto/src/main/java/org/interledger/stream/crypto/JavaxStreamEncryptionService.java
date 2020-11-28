package org.interledger.stream.crypto;

/**
 * An {@link StreamEncryptionService} that uses a JavaKeystore for underlying key storage.
 *
 * @deprecated This implementation will be removed in a future version. Prefer {@link AesGcmStreamEncryptionService}
 *     instead as a valid replacement.
 */
@Deprecated
public class JavaxStreamEncryptionService extends AesGcmStreamEncryptionService implements StreamEncryptionService {

}
