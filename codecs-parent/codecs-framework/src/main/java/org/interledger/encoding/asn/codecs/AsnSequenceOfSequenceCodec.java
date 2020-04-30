package org.interledger.encoding.asn.codecs;

/*-
 * ========================LICENSE_START=================================
 * Interledger Codec Framework
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A CoDec for ASN.1 OER SEQUENCE-OF-SEQUENCE types (i.e., the mechanism used by ASN.1 OER to encode an array of
 * objects).
 *
 * @param <L> The type of {@link List} this codec stores its sub-codecs in.
 * @param <T> The type of object in the {@link List} of codecs this SEQUENCE-OF codec uses.
 *
 * @see "https://www.oss.com/asn1/resources/books-whitepapers-pubs/Overview_of_OER.pdf"
 */
public class AsnSequenceOfSequenceCodec<L extends List<T>, T> extends AsnObjectCodecBase<L> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AsnSequenceOfSequenceCodec.class);

  private static final int CODECS_ARRAY_INITIAL_CAPACITY = 5;

  private final Supplier<L> listConstructor;
  private final Supplier<AsnSequenceCodec<T>> subCodecSupplier;

  private volatile ArrayList<AsnSequenceCodec<T>> codecs;

  /**
   * Required-args constructor. Allows a caller to specify a {@link Supplier} of a {@link List} of objects to encode or
   * decode, using potentially discrete codecs in an array such that for each index of the list, the identically indexed
   * codec is used.
   *
   * @param listConstructor  A {@link Supplier} of type {@link L}, which is a Codec, that can be used to construct a new
   *                         {@link List}.
   * @param subCodecSupplier A {@link Supplier} of type {@link AsnSequenceCodec} of type {@link T}.
   */
  public AsnSequenceOfSequenceCodec(
      final Supplier<L> listConstructor, final Supplier<AsnSequenceCodec<T>> subCodecSupplier
  ) {
    this.listConstructor = Objects.requireNonNull(listConstructor);
    this.subCodecSupplier = Objects.requireNonNull(subCodecSupplier);

    // A SEQUENCE-OF-SEQUENCE must always have the same type of SEQUENCE in its collection, so the codec used for each
    // SEQUENCE in an Array of SEQUENCES must always be the same type. That said, due to the unfortunate design of this
    // implementation, there must be a unique instance of a sub-codec for each item in the listSupplier. Despite this
    // limitation, we can construct a reasonably small array here in order to covers our happy-path (e.g., a typical
    // use-case for this functionality is the encoding/decoding of STREAM frames, and there generally aren't more than
    // a few frames in any given STREAM packet). Therefore, this implementation starts with a small array, but allows
    // the array to grow dynamically as needed.
    // This design preferable to simply constructing an array with a length equal to the length supplied in the ASN.1
    // OER packet, because this could lead to a potential DOS attack if an attacker were to maliciously specify an
    // artificially long length value.
    // For example, a malicious packet creator could specify a length value of Integer.MAX_VALUE, which a naive decoder
    // would use to initialize an array, thus accidentially allocating 2GB of JVM heap. This likely would result in an
    // OutOfMemoryError depending on the default heap size of the JVM. Instead, the chosen design which dynamically
    // allocates the array based upon actual byte counts avoids this potential attack vector.
    this.codecs = new ArrayList<>(CODECS_ARRAY_INITIAL_CAPACITY);
  }

  /**
   * <p>Return the number of sub-codecs currently allocated in this Codec.</p>
   *
   * <p>While this size-value can theoretically change at runtime, by the time a serializer is calling `write` on this
   * codec, no more sub-codecs will be added to this instance, which means we can adjust the contents of {@link #codecs}
   * in a just-in-time fashion in response to {@link #getCodecAt(int)}. This design, while imperfect, is a legacy of the
   * original design of our Codec system, which puts state into each sub-codec. This will be addressed and  improved
   * once the Codec system becomes less stateful.</p>
   *
   * @return An integer representing the number of sub-codecs in this instance.
   *
   * @see "https://github.com/hyperledger/quilt/issues/164"
   */
  public int size() {
    return this.codecs.size();
  }

  /**
   * <p>Accessor for the codec at index {@code index}.</p>
   *
   * <p>This implementation uses a just-in-time construction mechanism for each sub-codec as an optimization, for two
   * reasons. First, each sub-codec must have its own unique state, so we can't just use a single instance of the
   * sub-codec for all indices. Second, the way the Sub-codecs work is the sub-codec is not actually used until it is
   * accessed via this method. Thus, constructing each instance via the supplier is acceptable.</p>
   *
   * @param index The index of the codec to retrieve. Index corresponds to the index of the item in the SEQUENCE-OF that
   *              is about to be encoded/decoded.
   *
   * @return An {@link AsnSequenceCodec} for encoding/decoding a particular item (encoded in ASN.1 OER) in a SEQUENCE-OF
   *     collection.
   */
  public AsnSequenceCodec<T> getCodecAt(final int index) {
    Objects.requireNonNull(codecs);
    final AsnSequenceCodec<T> subCodec;
    if (codecs.size() == 0 || codecs.size() <= index) {
      subCodec = subCodecSupplier.get();
      codecs.add(index, subCodec);
    } else {
      subCodec = codecs.get(index);
    }
    return subCodec;
  }

  /**
   * Decode and return the value read into the codec during serialization.
   *
   * @return the decoded object
   */
  @Override
  public L decode() {
    Objects.requireNonNull(codecs);
    L list = listConstructor.get();
    for (AsnSequenceCodec<T> codec : codecs) {
      list.add(codec.decode());
    }
    return list;
  }

  /**
   * Encode the provided value into the codec to be written during serialization.
   *
   * @param values the value to encode
   */
  @Override
  public void encode(final L values) {
    Objects.requireNonNull(values);
    this.codecs = new ArrayList<>(CODECS_ARRAY_INITIAL_CAPACITY);
    for (T value : values) {
      AsnSequenceCodec<T> codec = subCodecSupplier.get();
      codec.encode(value);
      this.codecs.add(codec);
    }
  }

}
