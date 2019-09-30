package org.interledger.link.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.link.LinkId;

import org.junit.Test;

/**
 * Unit tests for {@link LinkException}.
 */
public class LinkExceptionTest {

  @Test
  public void getLinkId() {
    assertThat(new LinkException(LinkId.of("foo")).getLinkId().value()).isEqualTo("foo");
  }

  @Test
  public void testToStringWithNullMessage() {
    assertThat(new LinkException(LinkId.of("foo")).toString())
        .isEqualTo("org.interledger.link.exceptions.LinkException: LinkId=LinkId(foo)");
  }

  @Test
  public void testToString() {
    assertThat(new LinkException("hello", LinkId.of("foo")).toString())
        .isEqualTo("org.interledger.link.exceptions.LinkException: hello LinkId=LinkId(foo)");
  }
}
