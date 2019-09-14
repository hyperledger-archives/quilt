package org.interledger.link;

import static org.assertj.core.api.Assertions.assertThat;

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

    assertThat(linkSettings.getLinkType()).isEqualTo(LinkType.of("foo"));
    assertThat(linkSettings.getCustomSettings().size()).isEqualTo(1);
    assertThat(linkSettings.getCustomSettings().get("foo")).isEqualTo("bar");
  }
}
