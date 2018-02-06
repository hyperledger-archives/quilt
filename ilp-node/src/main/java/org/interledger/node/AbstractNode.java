package org.interledger.node;

import org.interledger.core.InterledgerRuntimeException;
import org.interledger.node.channels.Channel;
import org.interledger.node.events.ChannelErrorEvent;
import org.interledger.node.exceptions.InterledgerNodeConfigurationException;
import org.interledger.node.services.ildcp.IldcpService;
import org.interledger.node.services.InterledgerPacketDispatcherService;
import org.interledger.node.services.InterledgerPaymentProtocolService;
import org.interledger.node.services.LoggingService;
import org.interledger.node.services.routing.SimplePaymentRouter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class AbstractNode {

  private final ThreadPoolExecutor pool;
  private final LoggingService logger;
  private final NodeConfiguration config;
  private final AccountManager accounts;

  private final InterledgerPacketDispatcherService dispatcher;
  private final IldcpService ildcpService;
  private final InterledgerPaymentProtocolService paymentProtocolService;

  protected AbstractNode(
      ThreadPoolExecutor pool, LoggingService logger, NodeConfiguration config,
      AccountManager accounts,
      InterledgerPacketDispatcherService dispatcher, IldcpService ildcpService, InterledgerPaymentProtocolService paymentProtocolService) {
    this.pool = pool;
    this.logger = logger;
    this.config = config;
    this.accounts = accounts;

    this.ildcpService = ildcpService;
    this.paymentProtocolService = paymentProtocolService;
    this.dispatcher = dispatcher;
  }

  void start() {

    //If we have a parent load that channel first
    if (accounts.getParent().isPresent()) {

      final Account parent = accounts.getParent().get();
      openChannel(parent);

      // If we have no configured ILP address, try to get one via ILDCP
      if (!config.getAddress().isPresent()) {
        try {
          ildcpService.requestConfigurationFromParent(parent).get();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        } catch (ExecutionException e) {
          throw new InterledgerRuntimeException(e);
        }
      }

    } else {
      if (!config.getAddress().isPresent()) {
        throw new InterledgerNodeConfigurationException(
            "No ILP address provided for the node "
                + "and no parent account configured to get address.");
      }
    }

    for (Account account : accounts) {
      if (account.getRelationship() != AccountRelationship.PARENT) {
        openChannel(account);
      }
    }

  }

  private void openChannel(Account account) {

    account.getChannel()
        .setOnOpened((e) -> {
          logger.logEvent(account, e);
        })
        .setOnClosed((e) -> {
          logger.logEvent(account, e);
        })
        .setOnError((e) -> {
          logger.logEvent(account, e);
          handleChannelError(account, e.getError());
        })
        .setOnIncomingTransfer((e) -> {
          logger.logEvent(account, e);
          paymentProtocolService.handleTransfer(account, e.getTransferAmount());
        })
        .setIncomingRequestHandler((request) -> {
          logger.logPacketReceived(account, request);
          return dispatcher.handlePacket(account, request);
        })
        .open();

  }

  /**
   * Spawn a thread to try and close and re-open the channel
   *
   * @param account the account that is the origin of the error
   * @param e       the error emitted by the channel
   */
  private void handleChannelError(final Account account, final Exception e) {

    //TODO Track restarts so we don't do this forever

    //TODO Understand the error and respond accordingly

    pool.execute(new Runnable() {

      final Channel channel = account.getChannel();

      @Override
      public void run() {

        //TODO Review this code for safety
        for (int i = 0; i < 5 && channel.isOpen(); i++) {
          try {
            channel.close();
            Thread.sleep(1000);
          } catch (Exception e) {
            logger.logEvent(account, ChannelErrorEvent.builder().error(e).build());
          }
        }

        channel.open();

      }
    });

  }

}
