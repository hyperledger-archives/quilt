package org.interledger.link;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.events.LinkConnectionEventEmitter;
import org.interledger.link.exceptions.LinkException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

/**
 * Unit tests for {@link LoopbackLinkFactory}.
 */
public class LoopbackLinkFactoryTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");
  private final LinkId LINK_ID = LinkId.of("foo");

  @Mock
  private LinkSettings linkSettingsMock;

  @Mock
  private PacketRejector packetRejectorMock;

  @Mock
  private LinkConnectionEventEmitter linkConnectionEventEmitterMock;

  private LoopbackLinkFactory loopbackLinkFactory;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.loopbackLinkFactory = new LoopbackLinkFactory(linkConnectionEventEmitterMock, packetRejectorMock);
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullLinkEventEmitter() {
    try {
      new LoopbackLinkFactory(null, packetRejectorMock);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("linkEventEmitter must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNulPacketRejector() {
    try {
      new LoopbackLinkFactory(linkConnectionEventEmitterMock, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("packetRejector must not be null"));
      throw e;
    }
  }

  @Test
  public void supports() {
    assertThat(loopbackLinkFactory.supports(LoopbackStatefulLink.LINK_TYPE), is(true));
    assertThat(loopbackLinkFactory.supports(LinkType.of("foo")), is(false));
  }

  @Test(expected = NullPointerException.class)
  public void constructLinkWithNullOperator() {
    try {
      loopbackLinkFactory.constructLink(null, linkSettingsMock);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("operatorAddressSupplier must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructLinkWithNullLinkSettings() {
    try {
      loopbackLinkFactory.constructLink(() -> Optional.of(OPERATOR_ADDRESS), null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("linkSettings must not be null"));
      throw e;
    }
  }

  @Test(expected = LinkException.class)
  public void constructLinkWithUnsupportedLinkType() {
    try {
      LinkSettings linkSettings = LinkSettings.builder()
          .linkType(LinkType.of("foo"))
          .build();
      loopbackLinkFactory.constructLink(() -> Optional.of(OPERATOR_ADDRESS), linkSettings);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("LinkType not supported by this factory. linkType=LinkType(FOO)"));
      throw e;
    }
  }

  @Test
  public void constructLink() {
    LinkSettings linkSettings = LinkSettings.builder()
        .linkType(LoopbackStatefulLink.LINK_TYPE)
        .build();
    Link<?> link = loopbackLinkFactory.constructLink(() -> Optional.of(OPERATOR_ADDRESS), linkSettings);
    link.setLinkId(LINK_ID);
    assertThat(link.getLinkId(), is(LINK_ID));
  }
}
