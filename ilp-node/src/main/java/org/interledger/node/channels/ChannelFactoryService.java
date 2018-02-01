package org.interledger.node.channels;

import static java.lang.String.format;

import org.interledger.core.InterledgerRuntimeException;
import org.interledger.node.Account;
import org.interledger.node.channels.spi.ChannelFactory;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class ChannelFactoryService {

  private static ChannelFactoryService service;
  private ServiceLoader<ChannelFactory> loader;

  private ChannelFactoryService() {
    loader = ServiceLoader.load(ChannelFactory.class);
  }

  public static synchronized ChannelFactoryService getInstance() {
    if (service == null) {
      service = new ChannelFactoryService();
    }
    return service;
  }

  public Channel getChannel(Class<Channel> type, ChannelConfig config) {

    try {
      Iterator<ChannelFactory> factories = loader.iterator();
      while (factories.hasNext()) {
        ChannelFactory factory = factories.next();
        if(factory.getChannelType() == type) {
          return factory.newChannel(config);
        }
      }
      throw new InterledgerRuntimeException(
          format("Unable to load channel: %s. No ChannelFactory found.", type.getCanonicalName()));
    } catch (ServiceConfigurationError serviceError) {
      throw new InterledgerRuntimeException(
          format("Error loading channel: %s", type.getCanonicalName()), serviceError);
    }
  }
}