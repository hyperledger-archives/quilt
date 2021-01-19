package org.interledger.stream.pay;

import org.interledger.link.Link;
import org.interledger.stream.StreamPacketUtils;
import org.interledger.stream.connection.StreamConnection;
import org.interledger.stream.crypto.StreamPacketEncryptionService;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ErrorCode;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.pay.model.SendState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * An abstract class that contains helper-logic for anything that wraps a Stream Pay operation, such as a run-loop,
 * prober, or other type of class that might need to send value or data on a Stream as part of a payment.
 */
public abstract class AbstractPayWrapper {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final StreamPacketEncryptionService streamPacketEncryptionService;

  /**
   * Required-args Constructor.
   *
   * @param streamPacketEncryptionService A {@link StreamPacketEncryptionService}.
   */
  protected AbstractPayWrapper(final StreamPacketEncryptionService streamPacketEncryptionService) {
    this.streamPacketEncryptionService = Objects.requireNonNull(streamPacketEncryptionService);
  }

  /**
   * Close the Stream Connection by sending a final packet with both a {@link ConnectionCloseFrame} and a {@link
   * StreamCloseFrame}.
   *
   * @param streamConnection A {@link StreamConnection} to close.
   */
  protected void closeConnection(final StreamConnection streamConnection, final ErrorCode errorCode) {
    Objects.requireNonNull(streamConnection);
    try {
      getLink().sendPacket(StreamPacketUtils.constructPacketToCloseStream(
        streamConnection, this.getStreamEncryptionService(), errorCode
      ));
    } catch (Exception e) {
      logger.error("Unable to close STREAM Connection: " + e.getMessage(), e);
      // swallow this error because the sender can still complete even though it couldn't get something to the receiver.
    }
  }

  /**
   * Based upon the supplied {@code sendState}, determines if a Connection Close frame should be sent to the
   * destination.
   *
   * @return {@code true} if the connection should be closed; {@code false} otherwise.
   */
  protected boolean shouldCloseConnection(final SendState sendState) {
    Objects.requireNonNull(sendState);

    if (sendState.isPaymentError()) {
      return sendState != SendState.ClosedByRecipient; // <-- Connection already closed, so don't try to close it again.
    } else {
      return sendState == SendState.End;
    }
  }

  /**
   * Helper method to sleep the current thread.
   *
   * @param sleepTimeMs The number of milliseconds to sleep.
   */
  protected void sleep(final int sleepTimeMs) throws InterruptedException {
    Thread.sleep(sleepTimeMs);
  }

  /**
   * Accessor for the {@link Link} that will be used to send ILPv4 packets.
   *
   * @return A {@link Link}.
   */
  protected abstract Link<?> getLink();

  /**
   * Accessor for this class's encryption service.
   *
   * @return A {@link StreamPacketEncryptionService}.
   */
  protected StreamPacketEncryptionService getStreamEncryptionService() {
    return this.streamPacketEncryptionService;
  }
}
