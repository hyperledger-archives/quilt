package org.interledger.stream.receiver;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;

/**
 * An extension of {@link AbstractThrowableProblem} that is emitted when an SPSP request is made for an account/address
 * that does not exist.
 */
public class InvalidReceiverProblem extends AbstractThrowableProblem {

  // TODO: Move to ILP-link-core?
  @Deprecated
  public static final String TYPE_PREFIX = "https://errors.interledger.org";

  public InvalidReceiverProblem() {
    super(
        URI.create(TYPE_PREFIX + "/invalid-receiver-id"),
        "Invalid receiver ID",
        Status.NOT_FOUND
    );
  }

}
