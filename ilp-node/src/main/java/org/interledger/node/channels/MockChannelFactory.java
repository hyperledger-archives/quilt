package org.interledger.node.channels;

import org.interledger.node.channels.spi.ChannelFactory;

public class MockChannelFactory implements ChannelFactory<MockChannel> {

  @Override
  public Channel newChannel(ChannelConfig config) {
    return new MockChannel((MockChannel.MockChannelConfig) config);
  }

  @Override
  public Class<MockChannel> getChannelType() {
    return MockChannel.class;
  }
}
