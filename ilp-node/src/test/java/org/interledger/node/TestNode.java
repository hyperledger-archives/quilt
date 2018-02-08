package org.interledger.node;

import static java.lang.String.format;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.cryptoconditions.PreimageSha256Fulfillment;
import org.interledger.node.channels.Channel;
import org.interledger.node.channels.MockChannel;
import org.interledger.node.events.Event;
import org.interledger.node.exceptions.RequestRejectedException;
import org.interledger.node.services.InterledgerPacketDispatcherService;
import org.interledger.node.services.InterledgerPaymentProtocolService;
import org.interledger.node.services.LoggingService;
import org.interledger.node.services.fx.OneToOneExchangeRate;
import org.interledger.node.services.ildcp.IldcpService;
import org.interledger.node.services.routing.InMemoryRoutingTable;
import org.interledger.node.services.routing.Route;
import org.interledger.node.services.routing.RouteId;
import org.interledger.node.services.routing.RoutingTable;
import org.interledger.node.services.routing.SimplePaymentRouter;

import org.javamoney.moneta.CurrencyUnitBuilder;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class TestNode extends AbstractNode {

  static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  static final NodeConfiguration config = new Config();
  static final AccountManager accounts = defaultAccounts();
  static final IldcpService ildpService
      = new DefaultInterledgerPeerProtocolService(executor, config);
  static final InterledgerPaymentProtocolService ilpService
      = new DefaultInterledgerPaymentProtocolService(executor, config, new SimplePaymentRouter(defaultRoutes()));
  static final InterledgerPacketDispatcherService dispatcher
      = new InterledgerPacketDispatcherService(ildpService, ilpService);

  public TestNode() {
    super(
        executor,
        new Logger(),
        config,
        accounts,
        dispatcher,
        ildpService,
        ilpService);
  }

  public static void main(String[] args) {
    TestNode node = new TestNode();

    node.start();

    accounts.forEach((account) -> {
      MockChannel channel = (MockChannel) account.getChannel();

      try {
        InterledgerFulfillPacket response = channel.mockIncomingRequest(
            InterledgerPreparePacket.builder()
              .amount(BigInteger.TEN)
              .destination(InterledgerAddress.of("test1.chloe"))
              .executionCondition(PreimageSha256Fulfillment.from(new byte[32]).getCondition())
              .expiresAt(Instant.now().plusSeconds(30))
              .data(new byte[]{})
              .build()
        );

        System.out.println("Got response: " + response.toString());

      } catch (RequestRejectedException e) {
        e.printStackTrace();
      }

      channel.mockError(new RuntimeException("Test Exception"));


    });


  }

  static RoutingTable<Route> defaultRoutes() {

    InMemoryRoutingTable table = new InMemoryRoutingTable();

    for (Account account : accounts) {
      table.addRoute(Route.builder()
          .routeId(RouteId.of(UUID.randomUUID()))
          .sourceAccount(account)
          .destinationAccount(account)
          .exchangeRate(new OneToOneExchangeRate(
              account.getCurrencyUnit(),
              account.getCurrencyUnit()))
          .expiryMargin(Duration.ofSeconds(2))
          .targetPrefix(InterledgerAddress.of("test1."))
          .build());
    }

    return table;
  }

  static AccountManager defaultAccounts() {
    AccountManager accounts = new AccountManager();

    AccountId id = AccountId.of(UUID.randomUUID());

    accounts.add(Account.builder()
        .accountId(id)
        .relationship(AccountRelationship.PEER)
        .counterparty(Counterparty.builder()
          .name("Chloe")
          .build())
        .currencyScale(2)
        .currencyUnit(CurrencyUnitBuilder.of("XRP", "undefined").build())
        .channel(createMockChannel(id, executor))
        .build()
    );


    return accounts;
  }

  static Channel createMockChannel(AccountId id, ExecutorService executor) {

    return new MockChannel(MockChannel.MockChannelConfig.builder()
        .requestHandler((request) -> {

          System.out.println(format("Request into mock channel for account %s : %S",
              id, request.toString()));

          return executor.submit(() -> {
            return InterledgerFulfillPacket.builder()
                .fulfillment(PreimageSha256Fulfillment.from(new byte[32]))
                .data(new byte[] {})
                .build();
          });

        }).build());
  }

  static class Config implements NodeConfiguration {
    @Override
    public Optional<InterledgerAddress> getAddress() {
      return Optional.of(InterledgerAddress.of("test1.alice"));
    }
  }

  static class Logger implements LoggingService {

    @Override
    public void logEvent(Account source, Event event) {
      System.out.println(format("%s %s %s %s",
          event.getEventId(),
          event.getTimestamp(),
          source.getAccountId(),
          event.toString()));
    }

    @Override
    public void logPacketReceived(Account source, InterledgerPacket packet) {
      System.out.println(format("Received from %s %s",
          source.getAccountId(),
          packet.toString()));
    }

    @Override
    public void logPacketSent(Account source, InterledgerPacket packet) {
      System.out.println(format("Sent to %s %s",
          source.getAccountId(),
          packet.toString()));
    }
  }

}
