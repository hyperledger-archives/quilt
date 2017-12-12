package org.interledger.transport.ipr.codecs;

import org.interledger.codecs.InterledgerCodecContext;
import org.interledger.transport.ipr.InterledgerPaymentRequest;
import org.interledger.transport.ipr.codecs.oer.InterledgerPaymentRequestOerCodec;
import org.interledger.transport.psk.PskMessage;
import org.interledger.transport.psk.codecs.PskMessageBinaryCodec;

/**
 * Returns an {@link InterledgerCodecContext} with the IPR codec also registered.
 */
public class IprOerCodecContextDecorator {

  /**
   * Register the IPR codec into the context.
   *
   * @return An {@link InterledgerCodecContext} with IPR codecs also registered
   */
  public static InterledgerCodecContext registerIprOerCodecs(InterledgerCodecContext context) {
    context.register(InterledgerPaymentRequest.class, new InterledgerPaymentRequestOerCodec());
    return context;
  }
}
