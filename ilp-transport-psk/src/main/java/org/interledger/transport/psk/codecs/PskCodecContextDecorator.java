package org.interledger.transport.psk.codecs;

import org.interledger.codecs.InterledgerCodecContext;
import org.interledger.codecs.InterledgerCodecContextFactory;
import org.interledger.transport.psk.PskMessage;

/**
 * Creates an {@link InterledgerCodecContext} with the PSK codec also registered.
 */
public class PskCodecContextDecorator {

  /**
   * Create a new {@link InterledgerCodecContext} and register the binary codec
   * for {@link PskMessage} objects.
   *
   * @return An {@link InterledgerCodecContext} with PSK codecs also registered
   */
  public static InterledgerCodecContext registerPskCodecs(InterledgerCodecContext context) {
    context.register(PskMessage.class, new PskMessageBinaryCodec());
    return context;
  }
}
