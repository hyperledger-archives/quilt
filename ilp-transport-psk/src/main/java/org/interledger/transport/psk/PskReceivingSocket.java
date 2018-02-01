package org.interledger.transport.psk;

import org.interledger.core.InterledgerAddress;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PskReceivingSocket extends AsynchronousServerSocketChannel {

  private InterledgerAddress localAddressPrefix;
  private byte[] sharedkey;

  /**
   * Initializes a new instance of this class.
   *
   * @param provider The provider that created this channel
   */
  protected PskReceivingSocket(AsynchronousChannelProvider provider) {
    super(provider);
  }


  @Override
  public AsynchronousServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
    Objects.requireNonNull(local);

    if(!(local instanceof InterledgerSocketAddress)) {
      throw new IllegalArgumentException("Invalid address type.");
    }

    //TODO Get localAddressPrefix from channels identified by address

    return null;
  }

  @Override
  public <T> AsynchronousServerSocketChannel setOption(SocketOption<T> name, T value) throws
      IOException {
    return null;
  }

  @Override
  public <T> T getOption(SocketOption<T> name) throws IOException {
    return null;
  }

  @Override
  public Set<SocketOption<?>> supportedOptions() {
    return null;
  }

  @Override
  public <A> void accept(A attachment, CompletionHandler<AsynchronousSocketChannel, ? super A>
      handler) {

  }

  @Override
  public Future<AsynchronousSocketChannel> accept() {
    return null;
  }

  @Override
  public SocketAddress getLocalAddress() throws IOException {
    return null;
  }

  @Override
  public boolean isOpen() {
    return false;
  }

  @Override
  public void close() throws IOException {

  }
}
