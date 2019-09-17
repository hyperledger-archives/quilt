package org.interledger.stream.frames;

/*-
 * ========================LICENSE_START=================================
 * Interledger Core
 * %%
 * Copyright (C) 2017 - 2018 Hyperledger and its contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import com.google.common.primitives.UnsignedLong;
import org.interledger.core.Immutable;

/**
 * <p>The amount of money that should go to each stream is calculated by dividing the number of shares for the given
 * stream by the total number of shares in all of the StreamMoney frames in the packet.</p>
 *
 * <p>For example, if an ILP Prepare packet has an amount of 100 and three StreamMoney frames with 5, 15, and 30 shares
 * for streams 2, 4, and 6, respectively, that would indicate that stream 2 should get 10 units, stream 4 gets 30 units,
 * and stream 6 gets 60 units.</p>
 *
 * <p>If the Prepare amount is not divisible by the total number of shares, implementations SHOULD round the stream
 * amounts down. The remainder SHOULD be allocated to the lowest-numbered open stream that has not reached its maximum
 * receive amount.</p>
 */
@Immutable
public interface StreamMoneyFrame extends StreamFrame {

  /**
   * Get the default builder.
   *
   * @return a {@link StreamMoneyFrameBuilder} instance.
   */
  static StreamMoneyFrameBuilder builder() {
    return new StreamMoneyFrameBuilder();
  }

  @Override
  default StreamFrameType streamFrameType() {
    return StreamFrameType.StreamMoney;
  }

  /**
   * Identifier of the stream this frame refers to.
   *
   * @return A {@link UnsignedLong} with the stream id.
   */
  UnsignedLong streamId();

  /**
   * <p>Proportion of the ILP Prepare amount destined for the stream specified.</p>
   *
   * <p>The amount of money that should go to each stream is calculated by dividing the number of shares for the given
   * stream by the total number of shares in all of the StreamMoney frames in the packet.</p>
   *
   * <p>For example, if an ILP Prepare packet has an amount of 100 and three StreamMoney frames with 5, 15, and 30
   * shares for streams 2, 4, and 6, respectively, that would indicate that stream 2 should get 10 units, stream 4 gets
   * 30 units, and stream 6 gets 60 units.</p>
   *
   * <p>If the Prepare amount is not divisible by the total number of shares, implementations SHOULD round the stream
   * amounts down. The remainder SHOULD be allocated to the lowest-numbered open stream that has not reached its maximum
   * receive amount.</p>
   *
   * @return An {@link UnsignedLong} with the number of shares.
   */
  UnsignedLong shares();

}
