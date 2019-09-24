package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Unit tests for {@link AbstractLink}.
 */
public class AbstractLinkTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");
  private static final LinkId LINK_ID = LinkId.of("foo");
  private static final LinkSettings LINK_SETTINGS = LinkSettings.builder()
      .linkType(LoopbackLink.LINK_TYPE)
      .build();

  private AbstractLink<?> link;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.link = new TestAbstractLink(
        () -> OPERATOR_ADDRESS,
        LINK_SETTINGS
    );
    this.link.setLinkId(LINK_ID);
  }

  @Test(expected = IllegalStateException.class)
  public void getLinkIdWhenNull() {
    this.link = new TestAbstractLink(
        () -> OPERATOR_ADDRESS,
        LinkSettings.builder().linkType(LinkType.of("foo")).build()
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

  /**
   * Helper class for testing {@link AbstractLink}.
   */
  private class TestAbstractLink extends AbstractLink {

    private TestAbstractLink(Supplier<InterledgerAddress> operatorAddressSupplier, LinkSettings linkSettings) {
      super(operatorAddressSupplier, linkSettings);
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
