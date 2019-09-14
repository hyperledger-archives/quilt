package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.link.exceptions.LinkException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for {@link LinkFactoryProvider}.
 */
public class LinkFactoryProviderTest {

  public static final LinkType TEST_LINK_TYPE = LinkType.of("foo");

  @Mock
  private LinkFactory linkFactoryMock;

  private LinkFactoryProvider linkFactoryProvider;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.linkFactoryProvider = new LinkFactoryProvider();
  }

  @Test
  public void registerAndGetLinkFactory() {
    linkFactoryProvider.registerLinkFactory(TEST_LINK_TYPE, linkFactoryMock);
    assertThat(linkFactoryProvider.getLinkFactory(TEST_LINK_TYPE)).isEqualTo(linkFactoryMock);
  }

  @Test(expected = LinkException.class)
  public void getUnregisteredLinkFactory() {
    try {
      linkFactoryProvider.getLinkFactory(TEST_LINK_TYPE);
    } catch (LinkException e) {
      assertThat(e.getMessage()).isEqualTo("No registered LinkFactory linkType=LinkType(FOO)");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void registerLinkFactoryWithNullLinkType() {
    try {
      linkFactoryProvider.registerLinkFactory(null, linkFactoryMock);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("linkType must not be null");
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void registerLinkFactoryWithNullLinkFactory() {
    try {
      linkFactoryProvider.registerLinkFactory(TEST_LINK_TYPE, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("linkFactory must not be null");
      throw e;
    }
  }
}
