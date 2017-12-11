package org.interledger.transport.ipr.codecs;

import org.interledger.transport.ipr.InterledgerPaymentRequest;

import org.hyperledger.quilt.codecs.framework.Codec;

/**
 * An implementation of {@link Codec} that reads and writes instances of
 * {@link InterledgerPaymentRequest}.
 */
public interface InterledgerPaymentRequestCodec extends Codec<InterledgerPaymentRequest> {

}
