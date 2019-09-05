package org.interledger.link;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Unit tests for {@link LinkSettings}.
 */
public class LinkSettingsTest {

  @Test
  public void builder() {
    final LinkSettings linkSettings = LinkSettings.builder()
        .linkType(LinkType.of("foo"))
        .putCustomSettings("foo", "bar")
        .build();

    assertThat(linkSettings.getLinkType(), is(LinkType.of("foo")));
    assertThat(linkSettings.getCustomSettings().size(), is(1));
    assertThat(linkSettings.getCustomSettings().get("foo"), is("bar"));
  }
}
