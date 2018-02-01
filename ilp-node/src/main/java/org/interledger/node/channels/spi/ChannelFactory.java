package org.interledger.node.channels.spi;

import org.interledger.node.channels.Channel;
import org.interledger.node.channels.ChannelConfig;

public interface ChannelFactory<T extends Channel> {

  Channel newChannel(ChannelConfig config);

  Class<T> getChannelType();
}
