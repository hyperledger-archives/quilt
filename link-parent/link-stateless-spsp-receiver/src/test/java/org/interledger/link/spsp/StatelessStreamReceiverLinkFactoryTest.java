package org.interledger.link.spsp;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.PacketRejector;
import org.interledger.link.exceptions.LinkException;
import org.interledger.stream.receiver.StatelessStreamReceiver;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link StatelessSpspReceiverLinkFactory}.
 */
public class StatelessStreamReceiverLinkFactoryTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.operator");
  private final LinkId linkId = LinkId.of("foo");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private LinkSettings linkSettingsMock;

  @Mock
  private PacketRejector packetRejectorMock;

  @Mock
  private StatelessStreamReceiver statelessStreamReceiverMock;

  private StatelessSpspReceiverLinkFactory statelessSpspReceiverLinkFactory;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.statelessSpspReceiverLinkFactory = new StatelessSpspReceiverLinkFactory(
        packetRejectorMock, statelessStreamReceiverMock
    );
  }

  @Test
  public void constructWithNulPacketRejector() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("packetRejector must not be null");

    new StatelessSpspReceiverLinkFactory(null, statelessStreamReceiverMock);
  }

  @Test
  public void constructWithNulStatelessStreamReceiver() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("statelessStreamReceiver must not be null");

    new StatelessSpspReceiverLinkFactory(packetRejectorMock, null);
  }

  @Test
  public void supports() {
    assertThat(statelessSpspReceiverLinkFactory.supports(StatelessStreamReceiverLink.LINK_TYPE)).isEqualTo(true);
    assertThat(statelessSpspReceiverLinkFactory.supports(LinkType.of("foo"))).isEqualTo(false);
  }

  @Test
  public void constructLinkWithNullOperator() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("operatorAddressSupplier must not be null");

    statelessSpspReceiverLinkFactory.constructLink(null, linkSettingsMock);
  }

  @Test
  public void constructLinkWithNullLinkSettings() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("linkSettings must not be null");

    statelessSpspReceiverLinkFactory.constructLink(() -> OPERATOR_ADDRESS, null);
  }

  @Test
  public void constructLinkWithUnsupportedLinkType() {
    expectedException.expect(LinkException.class);
    expectedException.expectMessage("LinkType not supported by this factory. linkType=LinkType(FOO)");

    LinkSettings linkSettings = LinkSettings.builder()
        .linkType(LinkType.of("foo"))
        .build();
    statelessSpspReceiverLinkFactory.constructLink(() -> OPERATOR_ADDRESS, linkSettings);
  }

  @Test
  public void constructLink() {
    LinkSettings linkSettings = StatelessSpspReceiverLinkSettings.builder()
        .assetCode("USD")
        .assetScale((short) 9)
        .build();
    StatelessStreamReceiverLink link = (StatelessStreamReceiverLink) statelessSpspReceiverLinkFactory
        .constructLink(() -> OPERATOR_ADDRESS, linkSettings);

    assertThat(link.getLinkSettings().assetCode()).isEqualTo("USD");
    assertThat(link.getLinkSettings().assetScale()).isEqualTo(9);
    assertThat(link.getOperatorAddressSupplier().get()).isEqualTo(OPERATOR_ADDRESS);
  }
}
