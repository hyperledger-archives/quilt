package org.interledger.stream.pay.exceptions;

import org.interledger.stream.pay.model.SendState;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link StreamPayerException}.
 */
public class StreamPayerExceptionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void constructWithNonErrorReady() {
    expectedException.expect(IllegalArgumentException.class);
    new StreamPayerException(SendState.Ready);
  }

  @Test
  public void constructWithNonErrorWait() {
    expectedException.expect(IllegalArgumentException.class);
    new StreamPayerException(SendState.Wait);
  }

  @Test
  public void constructWithNonErrorEnd() {
    expectedException.expect(IllegalArgumentException.class);
    new StreamPayerException(SendState.End);
  }
}