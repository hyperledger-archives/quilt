package org.interledger.link;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
      .linkType(LoopbackStatefulLink.LINK_TYPE)
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
        () -> Optional.of(OPERATOR_ADDRESS),
        LINK_SETTINGS,
        linkConnectionEventEmitterMock
    );
    link.setLinkId(LINK_ID);
  }

  @Test(expected = IllegalStateException.class)
  public void getLinkIdWhenNull() {
    link = new TestAbstractStatefulLink(
        () -> Optional.of(OPERATOR_ADDRESS),
        LINK_SETTINGS,
        linkConnectionEventEmitterMock
    );

    try {
      link.getLinkId();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("The LinkId must be set before using a Link"));
      throw e;
    }
  }

  @Test
  public void getLinkId() {
    assertThat(link.getLinkId(), is(LINK_ID));
  }

  @Test(expected = IllegalStateException.class)
  public void setLinkIdWhenAlreadySet() {
    try {
      link.setLinkId(LinkId.of("bar"));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage(), is("LinkId may only be set once"));
      throw e;
    }
  }

  @Test
  public void getOperatorAddressSupplier() {
    assertThat(link.getOperatorAddressSupplier().get().get(), is(OPERATOR_ADDRESS));
  }

  @Test
  public void getLinkSettings() {
    assertThat(link.getLinkSettings(), is(LINK_SETTINGS));
  }

  @Test
  public void connect() {
    assertThat(doConnectCalled.get(), is(false));
    assertThat(link.isConnected(), is(false));
    link.connect();
    assertThat(doConnectCalled.get(), is(true));
    assertThat(link.isConnected(), is(true));
    verify(linkConnectionEventEmitterMock).emitEvent(Mockito.<LinkConnectedEvent>any());
    verifyNoMoreInteractions(linkConnectionEventEmitterMock);
  }

  @Test
  public void connectAndDisconnect() {
    assertThat(doConnectCalled.get(), is(false));
    assertThat(doDisconnectCalled.get(), is(false));
    assertThat(link.isConnected(), is(false));

    link.connect();

    assertThat(doConnectCalled.get(), is(true));
    assertThat(doDisconnectCalled.get(), is(false));
    assertThat(link.isConnected(), is(true));

    link.disconnect();

    assertThat(doConnectCalled.get(), is(true));
    assertThat(doDisconnectCalled.get(), is(true));
    assertThat(link.isConnected(), is(false));
  }

  @Test
  public void close() {
    assertThat(doConnectCalled.get(), is(false));
    assertThat(link.isConnected(), is(false));
    link.connect();
    assertThat(doConnectCalled.get(), is(true));
    assertThat(link.isConnected(), is(true));

    link.close();
    assertThat(doConnectCalled.get(), is(true));
    assertThat(doDisconnectCalled.get(), is(true));
    assertThat(link.isConnected(), is(false));
  }

  @Test
  public void disconnect() {
    assertThat(doDisconnectCalled.get(), is(false));
    assertThat(link.isConnected(), is(false));
    link.connect();
    assertThat(doConnectCalled.get(), is(true));
    assertThat(link.isConnected(), is(true));
  }

  @Test
  public void registerAndUnregisterLinkHandler() {
    assertThat(link.getLinkHandler().isPresent(), is(false));

    LinkHandler linkHandlerMock = mock(LinkHandler.class);
    link.registerLinkHandler(linkHandlerMock);
    assertThat(link.getLinkHandler().get(), is(linkHandlerMock));

    link.unregisterLinkHandler();
    assertThat(link.getLinkHandler().isPresent(), is(false));
  }

  @Test(expected = IllegalStateException.class)
  public void safeGetLinkHandlerWithoutRegistering() {
    try {
      link.safeGetLinkHandler();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage(), is("You MUST register a LinkHandler before using this link!"));
      throw e;
    }
  }

  @Test
  public void safeGetLinkHandler() {
    assertThat(link.getLinkHandler().isPresent(), is(false));
    LinkHandler linkHandlerMock = mock(LinkHandler.class);
    link.registerLinkHandler(linkHandlerMock);
    assertThat(link.safeGetLinkHandler(), is(linkHandlerMock));
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
        () -> Optional.of(OPERATOR_ADDRESS),
        LINK_SETTINGS,
        new EventBusConnectionEventEmitter(eventBus)
    );

    link.setLinkId(LINK_ID);
    link.addLinkEventListener(linkConnectionEventListenerMock);

    assertThat(onConnectCalled.get(), is(false));
    assertThat(onDisconnectCalled.get(), is(false));

    link.connect();
    assertThat(onConnectCalled.get(), is(true));
    assertThat(onDisconnectCalled.get(), is(false));

    link.sendPacket(mock(InterledgerPreparePacket.class));
    assertThat(onConnectCalled.get(), is(true));
    assertThat(onDisconnectCalled.get(), is(false));

    link.disconnect();
    assertThat(onConnectCalled.get(), is(true));
    assertThat(onDisconnectCalled.get(), is(true));

    // Try sending when disconnected.
    link.sendPacket(mock(InterledgerPreparePacket.class));
    assertThat(onConnectCalled.get(), is(true));
    assertThat(onDisconnectCalled.get(), is(true));
  }

  @Test
  public void sendWhenDisconnected() {
    LinkConnectionEventListener linkConnectionEventListenerMock = mock(LinkConnectionEventListener.class);
    link.addLinkEventListener(linkConnectionEventListenerMock);

    // Try sending when disconnected.
    link.sendPacket(mock(InterledgerPreparePacket.class));
    assertThat(onConnectCalled.get(), is(false));
    assertThat(onDisconnectCalled.get(), is(false));
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
        () -> Optional.of(OPERATOR_ADDRESS),
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

    assertThat(onConnectCalled.get(), is(false));
    assertThat(onDisconnectCalled.get(), is(false));

    link.setLinkId(LINK_ID);

    try {
      link.connect().join();
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage(), is("java.lang.RuntimeException: connect error"));
    }

    assertThat(onConnectCalled.get(), is(false));
    assertThat(onDisconnectCalled.get(), is(false));
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
        Supplier operatorAddressSupplier, LinkSettings linkSettings,
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
