package org.interledger.transport.psk;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public class PskSocketProvider extends AsynchronousChannelProvider {

  @Override
  public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory
      threadFactory) throws IOException {
    return null;
  }

  @Override
  public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int
      initialSize) throws IOException {
    return null;
  }

  @Override
  public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel
      (AsynchronousChannelGroup group) throws IOException {
    return null;
  }

  @Override
  public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException {
    return null;
  }
}
