package org.interledger.stream.pay;

import org.interledger.stream.pay.exceptions.StreamPayerException;
import org.interledger.stream.pay.model.SendState;

import org.hamcrest.CustomTypeSafeMatcher;

/**
 * Matchers for inspecting {@link SendState} values in a {@link StreamPayerException}.
 */
public class SendStateMatcher {

  /**
   * A {@link CustomTypeSafeMatcher} for ensuring that an instance of {@link StreamPayerException} has the indicated
   * {@link SendState}.
   *
   * @param sendState The {@link SendState} to require.
   *
   * @return An instance of {@link CustomTypeSafeMatcher}.
   */
  public static CustomTypeSafeMatcher<StreamPayerException> hasSendState(final SendState sendState) {
    return new CustomTypeSafeMatcher<StreamPayerException>("Has SendState") {
      @Override
      protected boolean matchesSafely(final StreamPayerException item) {
        return item.getSendState().equals(sendState);
      }
    };
  }

}
