package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.AbstractStatefulLink.EventBusConnectionEventEmitter;
import org.interledger.link.events.LinkConnectedEvent;
import org.interledger.link.events.LinkConnectionEventEmitter;
import org.interledger.link.events.LinkConnectionEventListener;
import org.interledger.link.events.LinkDisconnectedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Unit tests for {@link AbstractStatefulLink}.
 */
public class AbstractStatefulLinkTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");
  private static final LinkId LINK_ID = LinkId.of("foo");
  private static final LinkSettings LINK_SETTINGS = LinkSettings.builder()
      .linkType(LoopbackLink.LINK_TYPE)
      .build();

  @Mock
  private LinkConnectionEventEmitter linkConnectionEventEmitterMock;

  private AbstractStatefulLink<?> link;

  private AtomicBoolean doConnectCalled;
  private AtomicBoolean doDisconnectCalled;

  private AtomicBoolean onConnectCalled;
  private AtomicBoolean onDisconnectCalled;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.doConnectCalled = new AtomicBoolean();
    this.doDisconnectCalled = new AtomicBoolean();
    this.onConnectCalled = new AtomicBoolean();
    this.onDisconnectCalled = new AtomicBoolean();

    link = new TestAbstractStatefulLink(
        () -> OPERATOR_ADDRESS,
        LINK_SETTINGS,
        linkConnectionEventEmitterMock
    );
    link.setLinkId(LINK_ID);
  }

  @Test(expected = IllegalStateException.class)
  public void getLinkIdWhenNull() {
    link = new TestAbstractStatefulLink(
        () -> OPERATOR_ADDRESS,
        LINK_SETTINGS,
        linkConnectionEventEmitterMock
    );

    try {
      link.getLinkId();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("The LinkId must be set before using a Link");
      throw e;
    }
  }

  @Test
  public void getLinkId() {
    assertThat(link.getLinkId()).isEqualTo(LINK_ID);
  }

  @Test(expected = IllegalStateException.class)
  public void setLinkIdWhenAlreadySet() {
    try {
      link.setLinkId(LinkId.of("bar"));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).isEqualTo("LinkId may only be set once");
      throw e;
    }
  }

  @Test
  public void getOperatorAddressSupplier() {
    assertThat(link.getOperatorAddressSupplier().get()).isEqualTo(OPERATOR_ADDRESS);
  }

  @Test
  public void getLinkSettings() {
    assertThat(link.getLinkSettings()).isEqualTo(LINK_SETTINGS);
  }

  @Test
  public void connect() {
    assertThat(doConnectCalled.get()).isEqualTo(false);
    assertThat(link.isConnected()).isEqualTo(false);
    link.connect();
    assertThat(doConnectCalled.get()).isEqualTo(true);
    assertThat(link.isConnected()).isEqualTo(true);
    verify(linkConnectionEventEmitterMock).emitEvent(Mockito.<LinkConnectedEvent>any());
    verifyNoMoreInteractions(linkConnectionEventEmitterMock);
  }

  @Test
  public void connectAndDisconnect() {
    assertThat(doConnectCalled.get()).isEqualTo(false);
    assertThat(doDisconnectCalled.get()).isEqualTo(false);
    assertThat(link.isConnected()).isEqualTo(false);

    link.connect();

    assertThat(doConnectCalled.get()).isEqualTo(true);
    assertThat(doDisconnectCalled.get()).isEqualTo(false);
    assertThat(link.isConnected()).isEqualTo(true);

    link.disconnect();

    assertThat(doConnectCalled.get()).isEqualTo(true);
    assertThat(doDisconnectCalled.get()).isEqualTo(true);
    assertThat(link.isConnected()).isEqualTo(false);
  }

  @Test
  public void close() {
    assertThat(doConnectCalled.get()).isEqualTo(false);
    assertThat(link.isConnected()).isEqualTo(false);
    link.connect();
    assertThat(doConnectCalled.get()).isEqualTo(true);
    assertThat(link.isConnected()).isEqualTo(true);

    link.close();
    assertThat(doConnectCalled.get()).isEqualTo(true);
    assertThat(doDisconnectCalled.get()).isEqualTo(true);
    assertThat(link.isConnected()).isEqualTo(false);
  }

  @Test
  public void disconnect() {
    assertThat(doDisconnectCalled.get()).isEqualTo(false);
    assertThat(link.isConnected()).isEqualTo(false);
    link.connect();
    assertThat(doConnectCalled.get()).isEqualTo(true);
    assertThat(link.isConnected()).isEqualTo(true);
  }

  @Test
  public void registerAndUnregisterLinkHandler() {
    assertThat(link.getLinkHandler().isPresent()).isEqualTo(false);

    LinkHandler linkHandlerMock = mock(LinkHandler.class);
    link.registerLinkHandler(linkHandlerMock);
    assertThat(link.getLinkHandler().get()).isEqualTo(linkHandlerMock);

    link.unregisterLinkHandler();
    assertThat(link.getLinkHandler().isPresent()).isEqualTo(false);
  }

  @Test(expected = IllegalStateException.class)
  public void safeGetLinkHandlerWithoutRegistering() {
    try {
      link.safeGetLinkHandler();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("You MUST register a LinkHandler before using this link!");
      throw e;
    }
  }

  @Test
  public void safeGetLinkHandler() {
    assertThat(link.getLinkHandler().isPresent()).isEqualTo(false);
    LinkHandler linkHandlerMock = mock(LinkHandler.class);
    link.registerLinkHandler(linkHandlerMock);
    assertThat(link.safeGetLinkHandler()).isEqualTo(linkHandlerMock);
  }

  @Test
  public void addLinkEventListener() {
    final LinkConnectionEventListener linkConnectionEventListenerMock = new LinkConnectionEventListener() {

      @Override
      @Subscribe
      public void onConnect(LinkConnectedEvent event) {
        onConnectCalled.set(true);
      }

      @Override
      @Subscribe
      public void onDisconnect(LinkDisconnectedEvent event) {
        onDisconnectCalled.set(true);
      }
    };

    final EventBus eventBus = new EventBus();

    this.link = new TestAbstractStatefulLink(
        () -> OPERATOR_ADDRESS,
        LINK_SETTINGS,
        new EventBusConnectionEventEmitter(eventBus)
    );

    link.setLinkId(LINK_ID);
    link.addLinkEventListener(linkConnectionEventListenerMock);

    assertThat(onConnectCalled.get()).isEqualTo(false);
    assertThat(onDisconnectCalled.get()).isEqualTo(false);

    link.connect();
    assertThat(onConnectCalled.get()).isEqualTo(true);
    assertThat(onDisconnectCalled.get()).isEqualTo(false);

    link.sendPacket(mock(InterledgerPreparePacket.class));
    assertThat(onConnectCalled.get()).isEqualTo(true);
    assertThat(onDisconnectCalled.get()).isEqualTo(false);

    link.disconnect();
    assertThat(onConnectCalled.get()).isEqualTo(true);
    assertThat(onDisconnectCalled.get()).isEqualTo(true);

    // Try sending when disconnected.
    link.sendPacket(mock(InterledgerPreparePacket.class));
    assertThat(onConnectCalled.get()).isEqualTo(true);
    assertThat(onDisconnectCalled.get()).isEqualTo(true);
  }

  @Test
  public void sendWhenDisconnected() {
    LinkConnectionEventListener linkConnectionEventListenerMock = mock(LinkConnectionEventListener.class);
    link.addLinkEventListener(linkConnectionEventListenerMock);

    // Try sending when disconnected.
    link.sendPacket(mock(InterledgerPreparePacket.class));
    assertThat(onConnectCalled.get()).isEqualTo(false);
    assertThat(onDisconnectCalled.get()).isEqualTo(false);
  }

  @Test
  public void onConnectionError() {
    final LinkConnectionEventListener linkConnectionEventListener = new LinkConnectionEventListener() {
      @Override
      public void onConnect(LinkConnectedEvent event) {
      }

      @Override
      public void onDisconnect(LinkDisconnectedEvent event) {
      }
    };

    final EventBus eventBus = new EventBus();

    link = new AbstractStatefulLink(
        () -> OPERATOR_ADDRESS,
        LINK_SETTINGS,
        new EventBusConnectionEventEmitter(eventBus)
    ) {
      @Override
      public CompletableFuture<Void> doConnect() {
        doConnectCalled.set(true);
        return CompletableFuture.supplyAsync(() -> {
          throw new RuntimeException("connect error");
        });
      }

      @Override
      public CompletableFuture<Void> doDisconnect() {
        doDisconnectCalled.set(true);
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public InterledgerResponsePacket sendPacket(InterledgerPreparePacket preparePacket) {
        return mock(InterledgerResponsePacket.class);
      }
    };

    link.addLinkEventListener(linkConnectionEventListener);

    assertThat(onConnectCalled.get()).isEqualTo(false);
    assertThat(onDisconnectCalled.get()).isEqualTo(false);

    link.setLinkId(LINK_ID);

    try {
      link.connect().join();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("java.lang.RuntimeException: connect error");
    }

    assertThat(onConnectCalled.get()).isEqualTo(false);
    assertThat(onDisconnectCalled.get()).isEqualTo(false);
  }

  @Test
  public void removeLinkEventListener() {
    LinkConnectionEventListener linkConnectionEventListenerMock = mock(LinkConnectionEventListener.class);
    link.addLinkEventListener(linkConnectionEventListenerMock);
    link.removeLinkEventListener(linkConnectionEventListenerMock);

    link.connect();
    link.disconnect();
    verifyNoMoreInteractions(linkConnectionEventListenerMock);
  }

  /**
   * Helper class for testing {@link AbstractStatefulLink}.
   */
  private class TestAbstractStatefulLink extends AbstractStatefulLink {

    private TestAbstractStatefulLink(
        Supplier<InterledgerAddress> operatorAddressSupplier,
        LinkSettings linkSettings,
        LinkConnectionEventEmitter linkConnectionEventEmitter
    ) {
      super(operatorAddressSupplier, linkSettings, linkConnectionEventEmitter);
    }

    @Override
    public CompletableFuture<Void> doConnect() {
      doConnectCalled.set(true);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> doDisconnect() {
      doDisconnectCalled.set(true);
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public InterledgerResponsePacket sendPacket(InterledgerPreparePacket preparePacket) {
      return InterledgerRejectPacket.builder()
          .triggeredBy(OPERATOR_ADDRESS)
          .code(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE)
          .message("the message")
          .build();
    }
  }

}
