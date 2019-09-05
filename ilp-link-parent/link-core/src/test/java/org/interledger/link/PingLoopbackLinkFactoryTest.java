package org.interledger.link;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.events.LinkEventEmitter;
import org.interledger.link.exceptions.LinkException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

/**
 * Unit tests for {@link PingLoopbackLinkFactory}.
 */
public class PingLoopbackLinkFactoryTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");

  @Mock
  private LinkSettings linkSettingsMock;

  @Mock
  private LinkEventEmitter linkEventEmitterMock;

  private PingLoopbackLinkFactory pingLoopbackLinkFactory;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.pingLoopbackLinkFactory = new PingLoopbackLinkFactory(linkEventEmitterMock);
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNullLinkEventEmitter() {
    try {
      new PingLoopbackLinkFactory(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("linkEventEmitter must not be null"));
      throw e;
    }
  }

  @Test
  public void supports() {
    assertThat(pingLoopbackLinkFactory.supports(PingLoopbackLink.LINK_TYPE), is(true));
    assertThat(pingLoopbackLinkFactory.supports(LinkType.of("foo")), is(false));
  }

  @Test(expected = NullPointerException.class)
  public void constructLinkWithNullOperator() {
    try {
      pingLoopbackLinkFactory.constructLink(null, linkSettingsMock);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("operatorAddressSupplier must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructLinkWithNullLinkSettings() {
    try {
      pingLoopbackLinkFactory.constructLink(() -> Optional.of(OPERATOR_ADDRESS), null);
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
      pingLoopbackLinkFactory.constructLink(() -> Optional.of(OPERATOR_ADDRESS), linkSettings);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("LinkType not supported by this factory. linkType=LinkType(FOO)"));
      throw e;
    }
  }

  @Test
  public void constructLink() {
    LinkSettings linkSettings = LinkSettings.builder()
        .linkType(PingLoopbackLink.LINK_TYPE)
        .build();
    Link<?> link = pingLoopbackLinkFactory.constructLink(() -> Optional.of(OPERATOR_ADDRESS), linkSettings);
    assertThat(link.getLinkId(), is(nullValue()));
  }
}
