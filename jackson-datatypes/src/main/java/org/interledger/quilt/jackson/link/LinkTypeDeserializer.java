package org.interledger.quilt.jackson.link;

import org.interledger.link.LinkType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link LinkType}.
 */
public class LinkTypeDeserializer extends StdDeserializer<LinkType> {

  protected LinkTypeDeserializer() {
    super(LinkType.class);
  }

  @Override
  public LinkType deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
    return LinkType.of(jsonParser.getText());
  }
}
