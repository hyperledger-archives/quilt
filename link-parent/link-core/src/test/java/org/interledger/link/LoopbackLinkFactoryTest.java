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
 * Unit tests for {@link LoopbackLinkFactory}.
 */
public class LoopbackLinkFactoryTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");
  private final LinkId linkId = LinkId.of("foo");

  @Mock
  private LinkSettings linkSettingsMock;

  @Mock
  private PacketRejector packetRejectorMock;


  private LoopbackLinkFactory loopbackLinkFactory;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.loopbackLinkFactory = new LoopbackLinkFactory(packetRejectorMock);
  }

  @Test(expected = NullPointerException.class)
  public void constructWithNulPacketRejector() {
    try {
      new LoopbackLinkFactory(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("packetRejector must not be null");
      throw e;
    }
  }

  @Test
  public void supports() {
    assertThat(loopbackLinkFactory.supports(LoopbackLink.LINK_TYPE)).isEqualTo(true);
    assertThat(loopbackLinkFactory.supports(LinkType.of("foo"))).isEqualTo(false);
  }

  @Test(expected = NullPointerException.class)
  public void constructLinkWithNullOperator() {
    try {
      loopbackLinkFactory.constructLink(null, linkSettingsMock);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("operatorAddressSupplier must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void constructLinkWithNullLinkSettings() {
    try {
      loopbackLinkFactory.constructLink(() -> OPERATOR_ADDRESS, null);
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
      loopbackLinkFactory.constructLink(() -> OPERATOR_ADDRESS, linkSettings);
      fail();
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("LinkType not supported by this factory. linkType=LinkType(FOO)");
      throw e;
    }
  }

  @Test
  public void constructLink() {
    LinkSettings linkSettings = LinkSettings.builder()
        .linkType(LoopbackLink.LINK_TYPE)
        .build();
    Link<?> link = loopbackLinkFactory.constructLink(() -> OPERATOR_ADDRESS, linkSettings);
    link.setLinkId(linkId);
    assertThat(link.getLinkId()).isEqualTo(linkId);
  }
}
