package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.interledger.core.InterledgerAddress;
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
  private final LinkId LINK_ID = LinkId.of("foo");

  @Mock
  private LinkSettings linkSettingsMock;

  private PingLoopbackLinkFactory pingLoopbackLinkFactory;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.pingLoopbackLinkFactory = new PingLoopbackLinkFactory();
  }

  @Test
  public void supports() {
    assertThat(pingLoopbackLinkFactory.supports(PingLoopbackLink.LINK_TYPE)).isEqualTo(true);
    assertThat(pingLoopbackLinkFactory.supports(LinkType.of("foo"))).isEqualTo(false);
  }

  @Test(expected = NullPointerException.class)
  public void constructLinkWithNullOperator() {
    try {
      pingLoopbackLinkFactory.constructLink(null, linkSettingsMock);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("operatorAddressSupplier must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructLinkWithNullLinkSettings() {
    try {
      pingLoopbackLinkFactory.constructLink(() -> OPERATOR_ADDRESS, null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("linkSettings must not be null");
      throw e;
    }
  }

  @Test(expected = LinkException.class)
  public void constructLinkWithUnsupportedLinkType() {
    try {
      LinkSettings linkSettings = LinkSettings.builder()
          .linkType(LinkType.of("foo"))
          .build();
      pingLoopbackLinkFactory.constructLink(() -> OPERATOR_ADDRESS, linkSettings);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("LinkType not supported by this factory. linkType=LinkType(FOO)");
      throw e;
    }
  }

  @Test
  public void constructLink() {
    LinkSettings linkSettings = LinkSettings.builder()
        .linkType(PingLoopbackLink.LINK_TYPE)
        .build();
    Link<?> link = pingLoopbackLinkFactory.constructLink(() -> OPERATOR_ADDRESS, linkSettings);
    link.setLinkId(LINK_ID);
    assertThat(link.getLinkId()).isEqualTo(LINK_ID);
  }
}
