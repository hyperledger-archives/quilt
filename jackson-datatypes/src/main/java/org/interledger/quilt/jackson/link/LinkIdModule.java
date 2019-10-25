package org.interledger.quilt.jackson.link;

import org.interledger.link.LinkId;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * A Jackson {@link SimpleModule} for serializing and deserializing instances of {@link LinkId}.
 */
public class LinkIdModule extends SimpleModule {

  private static final String NAME = "LinkIdModule";

  /**
   * No-args Constructor.
   */
  public LinkIdModule() {
    super(NAME, new Version(1, 0, 0, null, "org.interledger", "link-id"));

    addSerializer(LinkId.class, new LinkIdSerializer());
    addDeserializer(LinkId.class, new LinkIdDeserializer());
  }
}
