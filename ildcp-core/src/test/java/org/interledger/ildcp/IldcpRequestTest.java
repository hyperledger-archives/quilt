package org.interledger.ildcp;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

import java.time.Instant;

/**
 * Unit tests for {@link IldcpRequest}.
 */
public class IldcpRequestTest {

  @Test
  public void testBuilder() {
    final Instant now = Instant.now();
    final IldcpRequest actual = IldcpRequest.builder().expiresAt(now).build();
    assertThat(actual.getExpiresAt(), is(now));
  }

  @Test
  public void testEmptyBuilder() {
    final Instant now = Instant.now();
    final IldcpRequest actual = IldcpRequest.builder().build();
    assertThat(actual.getExpiresAt().isAfter(now), is(true));
  }

  @Test
  public void testEqualsHashcode() {
    final Instant now = Instant.parse("2019-12-25T01:02:03.996Z");
    final Instant then = Instant.parse("2020-12-25T01:02:03.996Z");

    final IldcpRequest first = IldcpRequest.builder().expiresAt(now).build();
    final IldcpRequest second = IldcpRequest.builder().from(first).build();
    final IldcpRequest third = IldcpRequest.builder().expiresAt(then).build();

    assertThat(first.equals(second), is(true));
    assertThat(second.equals(first), is(true));
    assertThat(first.equals(third), is(false));
    assertThat(second.equals(third), is(false));

    assertThat(first.hashCode(), is(second.hashCode()));
    assertThat(second.hashCode(), is(first.hashCode()));
    assertThat(second.hashCode(), is(not(third.hashCode())));
    assertThat(second.hashCode(), is(not(third.hashCode())));
  }

  @Test
  public void testToString() {
    final Instant now = Instant.parse("2019-12-25T01:02:03.996Z");
    final IldcpRequest first = IldcpRequest.builder().expiresAt(now).build();

    assertThat(first.toString(), is("IldcpRequest{expiresAt=2019-12-25T01:02:03.996Z}"));
  }
}